package com.wearx.music.ui.components

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import coil3.compose.AsyncImage

private val BlurRadius = 40.dp
private const val ScrimAlpha = 0.62f

/**
 * Full-screen blurred copy of the current cover, dimmed so text stays legible.
 *
 * Shared by the player and the lyrics page so swiping between them keeps one continuous backdrop;
 * cross-fading it on every track change would also make the two screens visibly different.
 *
 * `Modifier.blur` needs API 31+; below that it is a no-op and the scrim alone does the job, so the
 * screen still reads correctly on Wear OS 3.
 */
@Composable
fun ArtworkBackdrop(
    artworkUri: Uri?,
    modifier: Modifier = Modifier,
) {
    val background = MaterialTheme.colorScheme.background
    Box(modifier.fillMaxSize().background(background)) {
        if (artworkUri == null) return@Box

        AnimatedContent(
            targetState = artworkUri,
            transitionSpec = {
                fadeIn(animationSpec = tween(durationMillis = 420)) togetherWith
                    fadeOut(animationSpec = tween(durationMillis = 300))
            },
            label = "backdrop",
        ) { uri ->
            AsyncImage(
                model = uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(BlurRadius),
            )
        }
        Box(Modifier.fillMaxSize().background(background.copy(alpha = ScrimAlpha)))
    }
}
