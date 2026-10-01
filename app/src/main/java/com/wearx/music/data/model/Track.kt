package com.wearx.music.data.model

import android.net.Uri

/** A single playable audio file discovered in [android.provider.MediaStore] or on disk. */
data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val uri: Uri,
    val trackNumber: Int,
    /** Filesystem path, used to look for a sibling `.lrc` lyrics file. May be null. */
    val path: String?,
    /** Cover for this track: the MediaStore album-art Uri, or a cached embedded picture. */
    val artUri: Uri?,
)
