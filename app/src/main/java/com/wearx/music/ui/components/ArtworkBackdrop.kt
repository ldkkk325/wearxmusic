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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import coil3.compose.AsyncImage

private val BlurRadius = 40.dp
private const val ScrimAlphaDark = 0.62f

/**
 * Heavier in light mode. The scrim's job is to hold `onSurface` text legible over a blurred cover,
 * and in light mode that text is dark — so the scrim has to push the cover further towards the
 * light background than the dark-mode value does. The same 0.62 leaves a dark cover showing through
 * and dark text on it is unreadable.
 */
private const val ScrimAlphaLight = 0.82f

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
    // Worked out here rather than inline: `background` is both this value and the name of the
    // modifier it is passed to, and reading it inside that call is needlessly hard to follow.
    val scrimAlpha = if (background.luminance() > 0.5f) ScrimAlphaLight else ScrimAlphaDark
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
        Box(Modifier.fillMaxSize().background(background.copy(alpha = scrimAlpha)))
    }
}
