package com.wearx.music.ui.components

import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import coil3.compose.AsyncImage

/**
 * Album cover with a graceful placeholder and a short fade-in once the bitmap is decoded.
 *
 * The artwork comes from `MediaStore`'s album-art provider as a `content://` Uri, which Coil
 * resolves and caches without any extra network dependency.
 */
@Composable
fun AlbumArtwork(
    artworkUri: Uri?,
    contentDescription: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
) {
    // Keyed on the Uri so switching tracks replays the fade instead of showing the old cover.
    var imageReady by remember(artworkUri) { mutableStateOf(false) }
    val imageAlpha by animateFloatAsState(
        targetValue = if (imageReady) 1f else 0f,
        animationSpec = tween(durationMillis = 320),
        label = "artworkAlpha",
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        if (artworkUri != null) {
            AsyncImage(
                model = artworkUri,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                onSuccess = { imageReady = true },
                onError = { imageReady = true },
                modifier = Modifier
                    .size(size)
                    .graphicsLayer { alpha = imageAlpha },
            )
        } else {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = contentDescription,
                modifier = Modifier.size(size / 2),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
