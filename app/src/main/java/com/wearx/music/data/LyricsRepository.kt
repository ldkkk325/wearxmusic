package com.wearx.music.data

import android.content.Context
import com.wearx.music.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

/**
 * Finds lyrics for a track, preferring what is embedded in the audio file and falling back to a
 * `.lrc` file sitting next to it.
 *
 * Embedded lookup works with just `READ_MEDIA_AUDIO` because it reads through the track's
 * `content://` Uri. The `.lrc` fallback needs real filesystem access, so it only succeeds when the
 * app has been granted `MANAGE_EXTERNAL_STORAGE`; when it cannot read the file the lookup simply
 * reports [Lyrics.None].
 */
class LyricsRepository(private val context: Context) {

    suspend fun load(track: Track?): Lyrics = withContext(Dispatchers.IO) {
        if (track == null) return@withContext Lyrics.None
        val raw = readEmbedded(track) ?: readSidecar(track)
        raw?.let(::toLyrics) ?: Lyrics.None
    }

    // ---------------------------------------------------------------- embedded

    private fun readEmbedded(track: Track): String? = runCatching {
        context.contentResolver.openInputStream(track.uri)?.use { input ->
            val head = input.readAtMost(10)
            val isId3 = head.size >= 10 &&
                head[0] == ID3 && head[1] == 'D'.code.toByte() && head[2] == '3'.code.toByte()
            if (isId3) {
                val majorVersion = head[3].toInt()
                val tagSize = synchsafe(head, 6).coerceAtMost(MAX_TAG_BYTES)
                parseId3Frames(majorVersion, input.readAtMost(tagSize))
            } else {
                // Vorbis comments live near the start of FLAC/OGG files.
                parseVorbisLyrics(head + input.readAtMost(VORBIS_SCAN_BYTES))
            }
        }
    }.getOrNull()

    private fun parseId3Frames(majorVersion: Int, body: ByteArray): String? {
        val idLength = if (majorVersion == 2) 3 else 4
        val headerLength = if (majorVersion == 2) 6 else 10
        var offset = 0

        while (offset + headerLength <= body.size) {
            val id = String(body, offset, idLength, Charsets.ISO_8859_1)
            if (id[0] == '\u0000' || id.isBlank()) break

            val size = when {
                majorVersion == 2 ->
                    ((body[offset + 3].toInt() and 0xFF) shl 16) or
                        ((body[offset + 4].toInt() and 0xFF) shl 8) or
                        (body[offset + 5].toInt() and 0xFF)

                majorVersion >= 4 -> synchsafe(body, offset + 4)
                else -> beInt(body, offset + 4)
            }
            if (size <= 0 || offset + headerLength + size > body.size) break

            if (id == "USLT") {
                val frame = body.copyOfRange(offset + headerLength, offset + headerLength + size)
                return decodeUslt(frame)
            }
            offset += headerLength + size
        }
        return null
    }

    /** USLT layout: encoding, 3 language bytes, a terminated descriptor, then the lyrics text. */
    private fun decodeUslt(frame: ByteArray): String? {
        if (frame.size < 5) return null
        val encoding = frame[0].toInt()
        var index = 4

        if (encoding == 0 || encoding == 3) {
            while (index < frame.size && frame[index] != 0.toByte()) index++
            index++
        } else {
            while (index + 1 < frame.size &&
                !(frame[index] == 0.toByte() && frame[index + 1] == 0.toByte())
            ) {
                index += 2
            }
            index += 2
        }
        if (index >= frame.size) return null

        val payload = frame.copyOfRange(index, frame.size)
        val text = when (encoding) {
            0 -> String(payload, Charsets.ISO_8859_1)
            1 -> String(payload, Charsets.UTF_16)
            2 -> String(payload, Charsets.UTF_16BE)
            else -> String(payload, Charsets.UTF_8)
        }
        return text.trim('\u0000', '\uFEFF', '\r', '\n', ' ').takeIf { it.isNotBlank() }
    }

    private fun parseVorbisLyrics(bytes: ByteArray): String? {
        val text = String(bytes, Charsets.ISO_8859_1)
        val match = VORBIS_LYRICS.find(text) ?: return null
        val value = match.groupValues[1]
            .filter { it == '\n' || it == '\r' || it == '\t' || it.code >= 0x20 }
            .trim()
        return value.takeIf { it.isNotBlank() }
    }

    // ------------------------------------------------------------------ sidecar

    private fun readSidecar(track: Track): String? {
        val path = track.path ?: return null
        val dot = path.lastIndexOf('.')
        val base = if (dot > 0) path.substring(0, dot) else path
        return SIDECAR_EXTENSIONS.firstNotNullOfOrNull { extension ->
            runCatching { File(base + extension).takeIf { it.isFile }?.readText() }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
        }
    }

    // ------------------------------------------------------------------- parse

    /** Parses LRC timestamps when present; otherwise treats the whole text as untimed lyrics. */
    private fun toLyrics(raw: String): Lyrics {
        val lines = mutableListOf<LyricLine>()
        for (line in raw.lines()) {
            val stamps = LRC_TIME.findAll(line).toList()
            if (stamps.isEmpty()) continue
            val text = line.substring(stamps.last().range.last + 1).trim()
            for (stamp in stamps) {
                val minutes = stamp.groupValues[1].toLongOrNull() ?: continue
                val seconds = stamp.groupValues[2].toLongOrNull() ?: continue
                val fraction = stamp.groupValues[3]
                val millis = when (fraction.length) {
                    0 -> 0L
                    1 -> fraction.toLong() * 100
                    2 -> fraction.toLong() * 10
                    else -> fraction.take(3).toLong()
                }
                lines += LyricLine(minutes * 60_000L + seconds * 1000L + millis, text)
            }
        }
        if (lines.isEmpty()) {
            val plain = raw.trim()
            return if (plain.isEmpty()) Lyrics.None else Lyrics.Plain(plain)
        }
        return Lyrics.Synced(lines.sortedBy { it.timeMs })
    }

    // ------------------------------------------------------------------ helpers

    private fun InputStream.readAtMost(max: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val chunk = ByteArray(16 * 1024)
        var total = 0
        while (total < max) {
            val read = read(chunk, 0, minOf(chunk.size, max - total))
            if (read <= 0) break
            out.write(chunk, 0, read)
            total += read
        }
        return out.toByteArray()
    }

    private fun synchsafe(bytes: ByteArray, offset: Int): Int {
        if (offset + 3 >= bytes.size) return 0
        return ((bytes[offset].toInt() and 0x7F) shl 21) or
            ((bytes[offset + 1].toInt() and 0x7F) shl 14) or
            ((bytes[offset + 2].toInt() and 0x7F) shl 7) or
            (bytes[offset + 3].toInt() and 0x7F)
    }

    private fun beInt(bytes: ByteArray, offset: Int): Int {
        if (offset + 3 >= bytes.size) return 0
        return ((bytes[offset].toInt() and 0xFF) shl 24) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
            (bytes[offset + 3].toInt() and 0xFF)
    }

    private companion object {
        val ID3 = 'I'.code.toByte()
        const val MAX_TAG_BYTES = 4 * 1024 * 1024
        const val VORBIS_SCAN_BYTES = 256 * 1024
        val SIDECAR_EXTENSIONS = listOf(".lrc", ".LRC")
        val LRC_TIME = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")
        val VORBIS_LYRICS = Regex("""(?i)(?:UNSYNCED)?LYRICS=([^\u0000]{0,20000})""")
    }
}
