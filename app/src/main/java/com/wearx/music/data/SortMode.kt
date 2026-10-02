package com.wearx.music.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.ui.graphics.vector.ImageVector
import com.wearx.music.R

/**
 * How the track list is ordered.
 *
 * The library used to be alphabetical by title only, which is fine for a handful of songs and
 * useless for a few hundred. One cycling button covers the four orders that matter on a watch
 * without spending a whole row on a picker — and the icon on the button is always the order it
 * switches to, so it reads as "tap to sort by this".
 *
 * Albums keep their own name-then-artist ordering; this applies to the track list.
 */
enum class SortMode(val labelRes: Int, val icon: ImageVector) {
    TITLE(R.string.sort_title, Icons.Filled.SortByAlpha),
    ARTIST(R.string.sort_artist, Icons.Filled.Person),
    ALBUM(R.string.sort_album, Icons.Filled.Album),
    DURATION(R.string.sort_duration, Icons.Filled.Schedule),
    ;

    /** The order after this one, wrapping. */
    fun next(): SortMode = entries[(ordinal + 1) % entries.size]
}

/** Applies [mode] to [this] list, returning a new list. */
fun List<com.wearx.music.data.model.Track>.sortedBy(mode: SortMode): List<com.wearx.music.data.model.Track> =
    when (mode) {
        SortMode.TITLE -> sortedBy { it.title.lowercase() }
        SortMode.ARTIST -> sortedWith(compareBy({ it.artist.lowercase() }, { it.title.lowercase() }))
        SortMode.ALBUM -> sortedWith(compareBy({ it.album.lowercase() }, { it.title.lowercase() }))
        // Longest last: a long song is a deliberate choice, not something to be found first.
        SortMode.DURATION -> sortedWith(compareBy({ it.durationMs }, { it.title.lowercase() }))
    }
