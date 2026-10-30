package com.wearx.music.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.wearx.music.R
import com.wearx.music.data.Lyrics
import com.wearx.music.ui.components.NoEdgeFadeScaling
import kotlinx.coroutines.delay
import kotlin.math.abs

/** How many lines each side of the active one still get a blur ramp. */
private const val MaxBlurSteps = 3

/** How much the line being sung grows. */
private const val ActiveScale = 1.06f

/** How much neighbours shrink, per step away from the active line. */
private const val NeighbourScaleStep = 0.018f

/**
 * Material's emphasised easing, shared by every lyric transition so scale, alpha and colour move as
 * one gesture instead of three separate ones. No overshoot: the line glides to its new size rather
 * than springing past it.
 */
private val LyricEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private const val LyricTransitionMs = 440

/**
 * The blur settles a little ahead of the size and colour, so focus lands on the new line before the
 * old one has finished shrinking. Kept below [LyricTransitionMs] so letting go of the list brings
 * the blur back promptly rather than trailing the finger.
 */
private const val LyricBlurMs = 300

/** How often to re-check whether the user's own scroll has finished, while waiting to follow. */
private const val FollowRetryMs = 50L

/**
 * Lyrics page, hosted as the second page of the player's pager so swiping between the two is one
 * continuous gesture.
 *
 * The line being sung eases up in size and colour while its neighbours blur and dim by up to
 * [blurRadius] dp (0 turns the blur off). The blur is LATCHED off by a touch: the moment the
 * user grabs the list it disappears and stays gone — it does not come back on release — and
 * only the next line arriving restores it, so the lines the user went to read stay readable
 * until the song itself has moved on.
 */
@Composable
fun LyricsPage(
    lyrics: Lyrics,
    isLoading: Boolean,
    positionMs: Long,
    trackTitle: String?,
    blurRadius: Float,
) {
    val scrollState = rememberScalingLazyListState()

    val activeIndex = remember(lyrics, positionMs) {
        val synced = lyrics as? Lyrics.Synced ?: return@remember -1
        synced.lines.indexOfLast { it.timeMs <= positionMs }
    }
    // The track title is the only item above the lyrics.
    val headerItems = 1

    // True only while *this* screen is the one animating the list. The auto-follow below is a real
    // scroll too, so it flips `isScrollInProgress` exactly like a finger does; without this flag
    // every lyric advance would read as "the user is scrolling" and latch the blur off forever.
    var autoFollowing by remember { mutableStateOf(false) }

    LaunchedEffect(activeIndex, headerItems) {
        if (activeIndex < 0) return@LaunchedEffect
        // Wait out the user's own scrolling instead of dropping the follow outright. The old
        // early-return meant a line change that landed mid-drag was lost — not retried — so the
        // list stayed on the old line until the NEXT one after that came around.
        while (scrollState.isScrollInProgress) delay(FollowRetryMs)
        autoFollowing = true
        try {
            scrollState.animateScrollToItem(headerItems + activeIndex)
        } finally {
            // Also runs when a new active line cancels this follow mid-flight, so the flag can never
            // be left stuck on.
            autoFollowing = false
        }
    }

    // A touch latches the blur OFF and it does NOT come back on release...
    var blurSuppressed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        var wasGrabbed = false
        while (true) {
            val grabbed = scrollState.isScrollInProgress && !autoFollowing
            if (grabbed && !wasGrabbed) blurSuppressed = true
            wasGrabbed = grabbed
            delay(FollowRetryMs)
        }
    }
    // ...until the song reaches the next line, which restores it (and re-arms the latch for the
    // next time the user grabs the list).
    LaunchedEffect(activeIndex) {
        blurSuppressed = false
    }

    ScreenScaffold(scrollState = scrollState) { padding ->
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = scrollState,
            scalingParams = NoEdgeFadeScaling,
            contentPadding = padding,
        ) {
            item {
                Text(
                    text = trackTitle ?: stringResource(R.string.lyrics_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            when {
                isLoading -> item { CircularProgressIndicator() }

                lyrics is Lyrics.Synced -> itemsIndexed(lyrics.lines) { index, line ->
                    LyricLine(
                        text = line.text.ifBlank { "♪" },
                        distance = if (activeIndex < 0) 0 else abs(index - activeIndex),
                        isActive = index == activeIndex,
                        blurSuppressed = blurSuppressed,
                        blurRadius = blurRadius,
                    )
                }

                lyrics is Lyrics.Plain -> {
                    item {
                        Text(
                            text = stringResource(R.string.lyrics_untimed),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    items(lyrics.text.lines().filter { it.isNotBlank() }) { line ->
                        LyricLine(
                            text = line,
                            distance = 0,
                            isActive = false,
                            blurSuppressed = true,
                            blurRadius = blurRadius,
                        )
                    }
                }

                trackTitle == null -> item {
                    Text(
                        text = stringResource(R.string.lyrics_no_track),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                else -> {
                    item {
                        Text(
                            text = stringResource(R.string.lyrics_empty),
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item {
                        Text(
                            text = stringResource(R.string.lyrics_empty_hint),
                            style = MaterialTheme.typography.bodyExtraSmall,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricLine(
    text: String,
    distance: Int,
    isActive: Boolean,
    blurSuppressed: Boolean,
    blurRadius: Float,
) {
    // The ramp is a pure function of the distance to the singing line: the active line is sharp,
    // and blur climbs linearly to the full [blurRadius] by MaxBlurSteps away. Nothing here is
    // keyed off the scroll position, which is what kept the blur from sitting still.
    val rampedRadius = if (blurRadius <= 0f) {
        0.dp
    } else {
        blurRadius.dp * (distance.coerceAtMost(MaxBlurSteps) / MaxBlurSteps.toFloat())
    }
    val animatedBlur by animateDpAsState(
        targetValue = if (blurSuppressed) 0.dp else rampedRadius,
        animationSpec = tween(durationMillis = LyricBlurMs, easing = LyricEasing),
        label = "lyricBlur",
    )
    val lineAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else (0.6f - 0.1f * distance).coerceAtLeast(0.3f),
        animationSpec = tween(durationMillis = LyricTransitionMs, easing = LyricEasing),
        label = "lyricAlpha",
    )
    // A graphicsLayer scale, so the change never reflows the list mid-scroll.
    val lineScale by animateFloatAsState(
        targetValue = if (isActive) {
            ActiveScale
        } else {
            (1f - NeighbourScaleStep * distance.coerceAtMost(MaxBlurSteps)).coerceAtLeast(0.93f)
        },
        animationSpec = tween(durationMillis = LyricTransitionMs, easing = LyricEasing),
        label = "lyricScale",
    )
    val lineColor by animateColorAsState(
        targetValue = if (isActive) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = LyricTransitionMs, easing = LyricEasing),
        label = "lyricColor",
    )

    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = lineColor,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            // Room for the blur to bleed into, so a line that wraps onto a second row is blurred
            // exactly like a single-row line.
            .padding(vertical = 2.dp)
            // Unbounded, because the default (`BlurredEdgeTreatment.Rectangle`) clips the blur to
            // the text's own box: a wrapped line's box is taller, and its outer rows had their blur
            // cut off, which made the effect depend on whether the line happened to wrap.
            .blur(animatedBlur, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            .graphicsLayer {
                scaleX = lineScale
                scaleY = lineScale
                alpha = lineAlpha
            },
    )
}
