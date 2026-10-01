package com.wearx.music.playback

import androidx.media3.common.Player
import com.wearx.music.data.PlaybackMode

/**
 * The mode a player is currently in, derived from its shuffle + repeat state.
 *
 * Shuffle is folded into the same axis as the repeat modes rather than being a separate switch:
 * on a watch "how should this play?" is a single question, and one control with three states fits
 * where shuffle-checkbox plus repeat-cycle does not.
 */
fun Player.playbackMode(): PlaybackMode = when {
    shuffleModeEnabled -> PlaybackMode.SHUFFLE
    repeatMode == Player.REPEAT_MODE_ONE -> PlaybackMode.REPEAT_ONE
    else -> PlaybackMode.REPEAT_ALL
}

/** Applies [mode] as the pair of player flags it stands for. */
fun Player.applyPlaybackMode(mode: PlaybackMode) {
    shuffleModeEnabled = mode == PlaybackMode.SHUFFLE
    repeatMode = when (mode) {
        // Shuffled playback still loops the queue, otherwise it would stop after one pass.
        PlaybackMode.SHUFFLE, PlaybackMode.REPEAT_ALL -> Player.REPEAT_MODE_ALL
        PlaybackMode.REPEAT_ONE -> Player.REPEAT_MODE_ONE
    }
}
