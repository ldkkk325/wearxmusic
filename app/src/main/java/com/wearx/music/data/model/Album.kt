package com.wearx.music.data.model

import android.net.Uri

/** An album, i.e. the set of [Track]s sharing one `MediaStore` album id. */
data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val artUri: Uri?,
    val tracks: List<Track>,
) {
    val trackCount: Int get() = tracks.size

    val durationMs: Long get() = tracks.fold(0L) { acc, track -> acc + track.durationMs }
}
