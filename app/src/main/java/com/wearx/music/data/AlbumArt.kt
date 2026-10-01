package com.wearx.music.data

import android.content.ContentUris
import android.net.Uri

/**
 * `MediaStore` exposes album artwork through a dedicated pseudo-provider. The returned Uri can be
 * handed straight to an image loader (Coil) and to `MediaMetadata.artworkUri` so the system media
 * notification shows the same cover.
 */
private val ALBUM_ART_BASE: Uri = Uri.parse("content://media/external/audio/albumart")

fun albumArtUri(albumId: Long): Uri? =
    if (albumId > 0L) ContentUris.withAppendedId(ALBUM_ART_BASE, albumId) else null
