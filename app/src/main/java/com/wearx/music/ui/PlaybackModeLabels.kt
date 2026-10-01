package com.wearx.music.ui

import com.wearx.music.R
import com.wearx.music.data.PlaybackMode

/**
 * The label resource for a playback mode, so the current value can be shown as a line of text
 * instead of trying to fit it inside a button.
 */
fun PlaybackMode.labelRes(): Int = when (this) {
    PlaybackMode.SHUFFLE -> R.string.mode_shuffle
    PlaybackMode.REPEAT_ALL -> R.string.mode_repeat_all
    PlaybackMode.REPEAT_ONE -> R.string.mode_repeat_one
}
