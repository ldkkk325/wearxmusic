package com.wearx.music.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import kotlin.math.PI
import kotlin.math.sin

/**
 * A wave-shaped seek bar: drag anywhere along it to scrub the play head.
 *
 * The wave is the player's own WaveProgressRing language made horizontal — a travelling ripple
 * while playing, flattening when paused. Dragging uses `draggable`, which is the gesture that
 * cooperates with a vertical list instead of stealing its touches (the tap-based seek did not).
 * [onSeek] fires continuously during the drag; [onSeekFinished] once, on release.
 */
@Composable
fun WaveSeekSlider(
    positionMs: Long,
    durationMs: Long,
    waving: Boolean,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onSeekFinished: (() -> Unit)? = null,
) {
    val currentPosition by rememberUpdatedState(positionMs)
    val currentOnSeek by rememberUpdatedState(onSeek)
    val currentOnSeekFinished by rememberUpdatedState(onSeekFinished)

    var widthPx by remember { mutableFloatStateOf(1f) }
    val dragState = rememberDraggableState { delta ->
        if (durationMs > 0L) {
            val deltaMs = delta / widthPx * durationMs
            val target = (currentPosition + deltaMs.toLong()).coerceIn(0L, durationMs)
            currentOnSeek(target)
        }
    }

    val wave = rememberInfiniteTransition(label = "seekWave")
    val phase by wave.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2_600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "seekWavePhase",
    )

    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val fillColor = MaterialTheme.colorScheme.primary
    val thumbRingColor = MaterialTheme.colorScheme.background

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(SeekTouchHeight)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .draggable(
                orientation = Orientation.Horizontal,
                state = dragState,
                onDragStopped = { currentOnSeekFinished?.invoke() },
            ),
    ) {
        val cy = size.height / 2f
        val amplitude = SeekWaveAmplitude.toPx()
        val strokeWidth = SeekStrokeWidth.toPx()
        // Gated read: once the wave flattens, the phase stops invalidating the draw.
        val ripple = if (waving) phase else 0f
        val period = SeekWaveLength.toPx()

        fun wavePath(fromX: Float, toX: Float): Path {
            val path = Path()
            if (toX <= fromX) return path
            var x = fromX
            path.moveTo(x, cy + amplitude * sin((x / period) * 2f * PI.toFloat() + ripple))
            while (x < toX) {
                x += SeekWaveStep.toPx()
                val px = minOf(x, toX)
                path.lineTo(px, cy + amplitude * sin((px / period) * 2f * PI.toFloat() + ripple))
            }
            return path
        }

        val fraction = if (durationMs > 0L) {
            (currentPosition.toFloat() / durationMs).coerceIn(0f, 1f)
        } else {
            0f
        }
        val headX = size.width * fraction

        drawPath(
            path = wavePath(0f, size.width),
            color = trackColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        if (headX > 0f) {
            drawPath(
                path = wavePath(0f, headX),
                color = fillColor,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
        // The play head: a thumb sitting on the wave, ringed in the page background so it reads
        // as above the track rather than merged into it.
        drawCircle(
            color = thumbRingColor,
            radius = SeekThumbDiameter.toPx() / 2f + SeekThumbRing.toPx(),
            center = Offset(headX, cy),
        )
        drawCircle(
            color = fillColor,
            radius = SeekThumbDiameter.toPx() / 2f,
            center = Offset(headX, cy),
        )
    }
}

private val SeekThumbDiameter = 16.dp
private val SeekThumbRing = 2.dp
private val SeekWaveAmplitude = 3.dp
private val SeekWaveLength = 36.dp
private val SeekStrokeWidth = 6.dp
private val SeekWaveStep = 2.dp
private val SeekTouchHeight = 44.dp
