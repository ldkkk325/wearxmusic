package com.wearx.music.playback

import android.net.Uri
import androidx.media3.common.Player
import com.wearx.music.data.PlaybackMode

/**
 * Why playback last stopped on its own, when it did.
 *
 * The service already handles audio focus and "becoming noisy" correctly, but the UI had no way to
 * say so: the controls simply stopped moving. Naming the reason is the difference between a watch
 * that looks broken and one that says why.
 */
enum class PlaybackInterruption { AUDIO_BECOMING_NOISY, AUDIO_FOCUS_LOST, ERROR }

/**
 * Immutable snapshot of the player's LOW-frequency state: everything that changes on a user
 * action or a track transition, nothing that moves with the play head.
 *
 * The play head lives in [PlayerPosition] on its own stream. Folding it in here made this flow
 * emit on every position tick — and since the whole app collects it, four polls a second turned
 * into four full recompositions of every screen per second on a watch.
 */
data class PlayerUiState(
    val isConnected: Boolean = false,
    val isPlaying: Boolean = false,
    val mediaId: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val artworkUri: Uri? = null,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val queueSize: Int = 0,
    /** Set only while playback is stopped for a reason the user did not ask for. */
    val interruption: PlaybackInterruption? = null,
) {
    val hasMedia: Boolean get() = mediaId != null

    /** The single "how is this playing?" value the UI cycles through. */
    val playbackMode: PlaybackMode
        get() = when {
            shuffleEnabled -> PlaybackMode.SHUFFLE
            repeatMode == Player.REPEAT_MODE_ONE -> PlaybackMode.REPEAT_ONE
            else -> PlaybackMode.REPEAT_ALL
        }
}

/** Where the play head is. Changes up to four times a second while playing. */
data class PlayerPosition(
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedMs: Long = 0L,
) {
    val progressFraction: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}
