package com.wearx.music.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * A wave-shaped circular progress ring.
 *
 * The ring's radius ripples along its circumference, and the ripple travels around the circle while
 * [waving] is true — so the ring undulates during playback and settles into a plain, still ring
 * when playback is paused (the amplitude is animated, so it flattens smoothly rather than snapping).
 * The arc up to [progress] is drawn in [indicatorColor] over a [trackColor] ring.
 *
 * Both the phase and the amplitude are read inside the draw lambda, so the animation invalidates
 * only the drawing — it never recomposes the screen.
 */
@Composable
fun WaveProgressRing(
    progress: Float,
    waving: Boolean,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 5.dp,
    waveAmplitude: Dp = 2.5.dp,
    waveCount: Int = 9,
) {
    val wave = rememberInfiniteTransition(label = "waveRing")
    val phase by wave.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "waveRingPhase",
    )
    val waveFactor by animateFloatAsState(
        targetValue = if (waving) 1f else 0f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "waveRingFactor",
    )

    Canvas(modifier) {
        val strokePx = strokeWidth.toPx()
        val amplitude = waveAmplitude.toPx() * waveFactor
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val baseRadius = (minOf(size.width, size.height) - strokePx) / 2f - amplitude
        if (baseRadius <= 0f) return@Canvas

        // Start at the top (-90°) and walk clockwise; the radius ripples with a travelling sine.
        //
        // `phase` is read here, and a snapshot read inside a draw block is exactly what makes
        // Compose invalidate this draw — so reading it unconditionally costs a redraw every frame
        // forever, even while paused with the amplitude at zero and the ring a perfectly still
        // circle. On a watch that is a permanent drain on a screen that is usually just sitting
        // there, so the read is gated on there actually being a wave to move. While the wave fades
        // out the amplitude is still non-zero and the flattening is animated; once it reaches zero
        // the ring goes quiet until playback resumes.
        val ripple = if (amplitude > 0f) phase else 0f

        val ring = Path()
        val steps = 240
        for (i in 0..steps) {
            val angle = (i / steps.toFloat()) * 2f * PI.toFloat()
            val radius = baseRadius + amplitude * sin(waveCount * angle + ripple)
            val x = centerX + radius * sin(angle)
            val y = centerY - radius * cos(angle)
            if (i == 0) ring.moveTo(x, y) else ring.lineTo(x, y)
        }
        ring.close()

        drawPath(ring, color = trackColor, style = Stroke(width = strokePx, cap = StrokeCap.Round))

        // The played portion is its own path running from the top to `progress`, sampled with the
        // same formula as the track so it lies exactly on top of it. It used to be the closed ring
        // clipped to a sector, which cut the stroke mid-width at both ends — `StrokeCap.Round`
        // cannot show through a clip, so the head of the arc was a flat radial line, most obvious
        // in the first seconds of playback when the arc is a short sliver with two flat sides.
        // Drawing a partial path instead lets the round cap do its job.
        val played = progress.coerceIn(0f, 1f)
        if (played > 0f) {
            val arcSteps = max(2, (steps * played).roundToInt())
            val playedRing = Path()
            for (i in 0..arcSteps) {
                val angle = (i / arcSteps.toFloat()) * 2f * PI.toFloat() * played
                val radius = baseRadius + amplitude * sin(waveCount * angle + ripple)
                val x = centerX + radius * sin(angle)
                val y = centerY - radius * cos(angle)
                if (i == 0) playedRing.moveTo(x, y) else playedRing.lineTo(x, y)
            }
            drawPath(
                playedRing,
                color = indicatorColor,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
        }
    }
}
