package com.wearx.music.util

import java.util.Locale

/** Formats a millisecond duration as `m:ss`, or `--:--` when the duration is unknown. */
fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "--:--"
    val totalSeconds = durationMs / 1000L
    return String.format(Locale.US, "%d:%02d", totalSeconds / 60L, totalSeconds % 60L)
}
