package com.wearx.music.playback

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import org.json.JSONObject

/** The queue as it was when the process last went away: items, cursor, and the position within it. */
internal data class SavedQueue(
    val items: List<MediaItem>,
    val index: Int,
    val positionMs: Long,
)

/**
 * The one shape a queue item takes everywhere in the app — what [PlayerConnection] builds from a
 * [com.wearx.music.data.model.Track], and what [PlaybackQueueStore] restores after a restart — so
 * the two can never drift apart on metadata fields.
 */
internal fun buildMediaItem(
    mediaId: String,
    uri: Uri,
    title: String?,
    artist: String?,
    album: String?,
    artworkUri: Uri?,
): MediaItem = MediaItem.Builder()
    .setMediaId(mediaId)
    .setUri(uri)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(artworkUri)
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .build(),
    )
    .build()

/**
 * Remembers the play queue across process death.
 *
 * The player lives in [PlaybackService], which the system is free to kill on a watch; without a
 * persisted queue a restart comes back empty and the user has to find their song again. Two keys
 * on purpose: the item list changes only when the playlist does, while the cursor (index +
 * position) moves constantly — rewriting a thousand-item JSON blob every five seconds just to
 * advance a position would be pure disk churn.
 *
 * Restored paused, never auto-playing: coming back from the dead and making noise is not what the
 * user asked for. The media notification and the play button both light up with the old track
 * ready to go.
 */
internal class PlaybackQueueStore(context: Context) {

    private val prefs = context.getSharedPreferences("wearx_playback_queue", Context.MODE_PRIVATE)

    /** Writes the full item list. Call only when the playlist itself changed. */
    fun saveItems(player: Player) {
        val count = player.mediaItemCount
        if (count == 0) {
            prefs.edit().remove(KEY_ITEMS).apply()
            return
        }
        val array = org.json.JSONArray()
        for (i in 0 until count) {
            val json = player.getMediaItemAt(i).toQueueJson() ?: continue
            array.put(json)
        }
        prefs.edit().putString(KEY_ITEMS, array.toString()).apply()
    }

    /** Writes just the cursor. Cheap; safe to call at any frequency. */
    fun saveCursor(player: Player) {
        val count = player.mediaItemCount
        if (count == 0) return
        prefs.edit()
            .putInt(KEY_INDEX, player.currentMediaItemIndex.coerceIn(0, count - 1))
            .putLong(KEY_POSITION, player.currentPosition.coerceAtLeast(0L))
            .apply()
    }

    /** The last queue saved, or null when there is nothing (or anything unparseable) to restore. */
    fun load(): SavedQueue? {
        val raw = prefs.getString(KEY_ITEMS, null) ?: return null
        return runCatching {
            val array = org.json.JSONArray(raw)
            val items = buildList {
                for (i in 0 until array.length()) {
                    array.optJSONObject(i)?.toMediaItem()?.let(::add)
                }
            }
            if (items.isEmpty()) return null
            SavedQueue(
                items = items,
                // A stored index can outlive a shortened list (a rescanned library, a deleted
                // file); clamping beats crashing on setMediaItems.
                index = prefs.getInt(KEY_INDEX, 0).coerceIn(0, items.lastIndex),
                positionMs = prefs.getLong(KEY_POSITION, 0L).coerceAtLeast(0L),
            )
        }.getOrNull()
    }

    private fun MediaItem.toQueueJson(): JSONObject? {
        // The Uri is the only thing that cannot be rebuilt from metadata, and it lives on the
        // local configuration — present on both the service's own items and the controller's.
        val uri = localConfiguration?.uri ?: return null
        val metadata = mediaMetadata
        return JSONObject().apply {
            put(KEY_ID, mediaId)
            put(KEY_URI, uri.toString())
            metadata.title?.let { put(KEY_TITLE, it.toString()) }
            metadata.artist?.let { put(KEY_ARTIST, it.toString()) }
            metadata.albumTitle?.let { put(KEY_ALBUM, it.toString()) }
            metadata.artworkUri?.let { put(KEY_ART, it.toString()) }
        }
    }

    private fun JSONObject.toMediaItem(): MediaItem? = runCatching {
        buildMediaItem(
            mediaId = getString(KEY_ID),
            uri = Uri.parse(getString(KEY_URI)),
            title = optStringOrNull(KEY_TITLE),
            artist = optStringOrNull(KEY_ARTIST),
            album = optStringOrNull(KEY_ALBUM),
            artworkUri = optStringOrNull(KEY_ART)?.let(Uri::parse),
        )
    }.getOrNull()

    /** org.json's `optString` returns `""` for a missing key; a blank title should stay null. */
    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key)) optString(key).takeIf { it.isNotEmpty() } else null

    private companion object {
        const val KEY_ITEMS = "items"
        const val KEY_INDEX = "index"
        const val KEY_POSITION = "position"
        const val KEY_ID = "id"
        const val KEY_URI = "uri"
        const val KEY_TITLE = "title"
        const val KEY_ARTIST = "artist"
        const val KEY_ALBUM = "album"
        const val KEY_ART = "art"
    }
}
