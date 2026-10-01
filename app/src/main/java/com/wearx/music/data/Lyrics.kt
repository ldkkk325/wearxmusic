package com.wearx.music.data

/** One timestamped lyric line (LRC style). */
data class LyricLine(val timeMs: Long, val text: String)

/** Result of a lyrics lookup for a track. */
sealed interface Lyrics {
    /** Nothing found — the UI shows a hint instead. */
    data object None : Lyrics

    /** Timestamped lines that can follow playback. */
    data class Synced(val lines: List<LyricLine>) : Lyrics

    /** Untimed lyrics (or lyrics whose timestamps we could not parse). */
    data class Plain(val text: String) : Lyrics
}
