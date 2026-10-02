package com.wearx.music.data

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.provider.MediaStore
import com.wearx.music.data.model.Album
import com.wearx.music.data.model.Track
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/**
 * Read-only view over the device's local audio.
 *
 * MediaStore alone is not enough on real devices, so the library is the **union of two sources**:
 *
 *  1. `MediaStore`, queried across all external volumes and *without* an `IS_MUSIC` filter.
 *  2. A direct filesystem walk of the usual music folders.
 *
 * Both are needed. Files copied onto a watch over MTP or with a file manager routinely end up with
 * `IS_MUSIC`/`ALBUM`/`ALBUM_ID` all NULL — and a NULL makes `IS_MUSIC != 0` evaluate to NULL, so a
 * SQL filter silently drops exactly the tracks the user just added. Worse, MediaProvider hides such
 * rows from an app holding only `READ_MEDIA_AUDIO` even when a plain `content query` shows them. The
 * filesystem walk is what actually guarantees the songs show up.
 */
class MusicLibraryRepository(private val context: Context) {

    /**
     * Per-file results that survive a reload.
     *
     * `MediaMetadataRetriever.setDataSource` is not cheap, and [loadLibrary] runs again on every
     * permission change and every rescan, so re-opening every file each time is pure waste on a
     * watch. Both caches are keyed by something that changes when the underlying file does, so an
     * edited or replaced track is still re-read.
     */
    private val parsedFiles = mutableMapOf<String, ParsedTrack>()

    /** `File.canonicalPath` costs a syscall per call; the paths do not change between reloads. */
    private val canonicalPaths = mutableMapOf<String, String>()

    /** A parsed file plus the file state it was parsed from, so staleness is detectable. */
    private class ParsedTrack(val stamp: String, val track: Track)

    suspend fun loadLibrary(): List<Album> = withContext(Dispatchers.IO) {
        val fromStore = queryMediaStore()
        val knownPaths = fromStore.mapNotNullTo(mutableSetOf()) { it.path?.let(::identity) }
        val extras = collectAudioFiles()
            .filterNot { identity(it.absolutePath) in knownPaths }
            .mapNotNull(::readTrack)
        groupIntoAlbums(fromStore + extras)
    }

    /**
     * Asks the platform scanner to index every audio file we can see under the music folders, so
     * the next [loadLibrary] finds them in MediaStore (with album ids and artwork). Returns how
     * many files were handed to the scanner.
     */
    suspend fun requestRescan(): Int = withContext(Dispatchers.IO) {
        val paths = collectAudioFiles().map { it.absolutePath }
        if (paths.isEmpty()) return@withContext 0

        val scanned = CompletableDeferred<Unit>()
        var remaining = paths.size
        MediaScannerConnection.scanFile(context, paths.toTypedArray(), null) { _, _ ->
            // Only files the scanner actually indexed report back, so a file that is already in
            // MediaStore may never call in — which is why this waits on a timeout rather than on a
            // counter reaching zero.
            if (--remaining == 0) scanned.complete(Unit)
        }

        // Suspending instead of `CountDownLatch.await`. The old version parked the calling IO
        // THREAD for the whole timeout, and since the timer only expires when the scanner goes
        // quiet, that was up to twelve seconds in which every other IO caller — library loads,
        // Coil decoding artwork — queued behind it.
        withTimeoutOrNull(SCAN_TIMEOUT_MILLIS) { scanned.await() }
        paths.size
    }

    // ------------------------------------------------------------- MediaStore

    private fun queryMediaStore(): List<Track> {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            // `MediaColumns.DATA` is deprecated but is still the only way to reach a sibling
            // `.lrc` file and to de-duplicate against the filesystem walk; the literal avoids a
            // deprecation warning.
            "_data",
        )
        // VOLUME_EXTERNAL rather than EXTERNAL_CONTENT_URI so a second volume is not missed.
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val order = "${MediaStore.Audio.Media.ALBUM} ASC, ${MediaStore.Audio.Media.TRACK} ASC"

        val tracks = mutableListOf<Track>()
        context.contentResolver
            .query(collection, projection, null, null, order)
            ?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val dataColumn = cursor.getColumnIndex("_data")

                while (cursor.moveToNext()) {
                    val path = if (dataColumn >= 0) cursor.getString(dataColumn) else null
                    if (isSystemSoundPath(path)) continue

                    val id = cursor.getLong(idColumn)
                    val albumId = cursor.getLong(albumIdColumn)
                    // Untagged files leave ALBUM NULL; falling back to the containing folder keeps
                    // them grouped sensibly (usually "Music") instead of under an empty name.
                    val albumName = cursor.getString(albumColumn)?.takeIf { it.isNotBlank() }
                        ?: path?.substringBeforeLast('/')?.substringAfterLast('/').orEmpty()

                    tracks += Track(
                        id = id,
                        title = cursor.getString(titleColumn).orEmpty(),
                        artist = cursor.getString(artistColumn).orEmpty(),
                        album = albumName,
                        albumId = albumId,
                        durationMs = cursor.getLong(durationColumn),
                        uri = ContentUris.withAppendedId(collection, id),
                        trackNumber = cursor.getInt(trackColumn),
                        path = path,
                        artUri = albumArtUri(albumId),
                    )
                }
            }
        return tracks
    }

    // ------------------------------------------------------------ filesystem

    private fun collectAudioFiles(): List<File> {
        val seen = mutableSetOf<String>()
        return MUSIC_ROOTS
            .map(::File)
            .filter { it.isDirectory }
            .flatMap { root -> root.walkTopDown().filter { it.isFile }.toList() }
            .filter { it.extension.lowercase() in AUDIO_EXTENSIONS }
            .filterNot { isSystemSoundPath(it.absolutePath) }
            .filter { seen.add(identity(it.absolutePath)) }
    }

    private fun readTrack(file: File): Track? {
        // Cheap staleness check: if the file has not moved, the parse from last time still holds.
        val stamp = "${file.lastModified()}:${file.length()}"
        parsedFiles[file.absolutePath]?.takeIf { it.stamp == stamp }?.let { return it.track }

        val retriever = MediaMetadataRetriever()
        return runCatching {
            retriever.setDataSource(file.absolutePath)
            val title = retriever.string(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: file.nameWithoutExtension
            val artist = retriever.string(MediaMetadataRetriever.METADATA_KEY_ARTIST).orEmpty()
            val album = retriever.string(MediaMetadataRetriever.METADATA_KEY_ALBUM).orEmpty()
            val duration = retriever.string(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            val trackNumber = retriever.string(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                ?.substringBefore('/')
                ?.trim()
                ?.toIntOrNull() ?: 0

            val folder = file.parentFile?.name.orEmpty()
            val albumName = album.ifBlank { folder }
            val artwork = retriever.embeddedPicture?.let { cacheArtwork(albumName, it) }

            Track(
                id = identity(file.absolutePath).hashCode().toLong(),
                title = title,
                artist = artist,
                album = albumName,
                albumId = albumName.hashCode().toLong(),
                durationMs = duration,
                uri = Uri.fromFile(file),
                trackNumber = trackNumber,
                path = file.absolutePath,
                artUri = artwork,
            )
        }.getOrNull().also {
            runCatching { retriever.release() }
            if (it != null) {
                if (parsedFiles.size >= PARSED_CACHE_SIZE) parsedFiles.clear()
                parsedFiles[file.absolutePath] = ParsedTrack(stamp, it)
            }
        }
    }

    private fun MediaMetadataRetriever.string(key: Int): String? =
        extractMetadata(key)?.takeIf { it.isNotBlank() }

    /** Caches an embedded cover in the app cache dir so Coil can load it from a real Uri. */
    private fun cacheArtwork(albumName: String, bytes: ByteArray): Uri? = runCatching {
        val dir = File(context.cacheDir, "artwork").apply { mkdirs() }
        val file = File(dir, "${albumName.hashCode()}.img")
        if (!file.isFile || file.length() != bytes.size.toLong()) file.writeBytes(bytes)
        Uri.fromFile(file)
    }.getOrNull()

    // ---------------------------------------------------------------- shared

    /**
     * Groups by album *name* + artist rather than by `MediaStore`'s `album_id`: the id is NULL for
     * untagged files (and `getLong` silently turns that into 0), which would scatter one folder of
     * freshly copied songs into a bogus album per file.
     */
    private fun groupIntoAlbums(tracks: List<Track>): List<Album> = tracks
        .groupBy { "${it.album.lowercase()}\u0000${it.artist.lowercase()}" }
        .map { (key, albumTracks) ->
            val ordered = albumTracks.sortedWith(
                compareBy({ it.trackNumber.takeIf { n -> n > 0 } ?: Int.MAX_VALUE }, { it.title }),
            )
            val first = ordered.first()
            Album(
                id = key.hashCode().toLong(),
                title = first.album,
                artist = first.artist,
                artUri = ordered.firstNotNullOfOrNull { it.artUri },
                tracks = ordered,
            )
        }
        .sortedWith(compareBy({ it.title.lowercase() }, { it.artist.lowercase() }))

    /** Resolves `/sdcard/...` and `/storage/emulated/0/...` to one identity so nothing is listed twice. */
    private fun identity(path: String): String {
        canonicalPaths[path]?.let { return it }
        val resolved = runCatching { File(path).canonicalPath }.getOrDefault(path)
        if (canonicalPaths.size >= CANONICAL_CACHE_SIZE) canonicalPaths.clear()
        canonicalPaths[path] = resolved
        return resolved
    }

    /** Ringtones / notifications / alarms / Android internals are not part of the music library. */
    private fun isSystemSoundPath(path: String?): Boolean {
        if (path == null) return false
        return SYSTEM_SOUND_MARKERS.any { path.contains(it, ignoreCase = true) }
    }

    private companion object {
        /**
         * How long to wait for the scanner before reloading anyway. The scan itself continues in the
         * background either way, so this only decides when the UI stops saying "rescanning"; the
         * old twelve seconds was long enough to look like a freeze.
         */
        const val SCAN_TIMEOUT_MILLIS = 3_000L

        const val PARSED_CACHE_SIZE = 512
        const val CANONICAL_CACHE_SIZE = 2_048

        val MUSIC_ROOTS = listOf(
            "/storage/emulated/0/Music",
            "/sdcard/Music",
            "/storage/self/primary/Music",
        )
        val AUDIO_EXTENSIONS = setOf(
            "mp3", "m4a", "aac", "flac", "ogg", "opus", "wav", "wma", "mp4", "3gp", "amr", "mid",
        )
        val SYSTEM_SOUND_MARKERS = listOf(
            "/Ringtones/", "/Notifications/", "/Alarms/", "/Android/data/", "/Android/media/",
        )
    }
}
