package com.wearx.music.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import kotlin.math.roundToInt

/**
 * A finger-draggable continuous slider.
 *
 * Wear M3's own `Slider` is a segmented control driven by the rotary and by its +/− icons — it
 * has no drag gesture at all, which is why "调不动" on a slider meant to be dragged. This is the
 * drag: horizontal finger movement (and a tap to seek) over a simple track-and-thumb, sized to
 * swallow horizontal movement so a vertical list does not steal the gesture.
 *
 * `steps > 0` snaps to that many detents between the ends; `steps = 0` is genuinely stepless.
 * [onValueChangeFinished] fires once per gesture so callers can commit without writing settings
 * (and rebuilding the whole type scale) on every frame of the drag.
 */
@Composable
fun SteplessSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val currentValue by rememberUpdatedState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnValueChangeFinished by rememberUpdatedState(onValueChangeFinished)

    val span = (valueRange.endInclusive - valueRange.start).takeIf { it != 0f } ?: 1f

    fun quantized(candidate: Float): Float {
        val clamped = candidate.coerceIn(valueRange)
        if (steps <= 0) return clamped
        val stepSize = span / steps
        return valueRange.start + ((clamped - valueRange.start) / stepSize).roundToInt() * stepSize
    }

    val thumbRadius = ThumbDiameter / 2
    val trackHeight = TrackHeight

    // Captured outside the draw block: MaterialTheme is a composable read, and the dynamic
    // scheme may change under us without the slider needing to re-measure.
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val fillColor = MaterialTheme.colorScheme.primary
    val thumbRingColor = MaterialTheme.colorScheme.background

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(TouchHeight)
            .pointerInput(valueRange, steps) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        currentOnValueChange(quantized(valueRange.start + (offset.x / size.width) * span))
                    },
                    onDragEnd = { currentOnValueChangeFinished?.invoke() },
                    onDragCancel = { currentOnValueChangeFinished?.invoke() },
                    onHorizontalDrag = { _, dragAmount ->
                        val delta = dragAmount / size.width * span
                        currentOnValueChange(quantized(currentValue + delta))
                    },
                )
            }
            .pointerInput(valueRange, steps) {
                detectTapGestures { offset ->
                    currentOnValueChange(quantized(valueRange.start + (offset.x / size.width) * span))
                    currentOnValueChangeFinished?.invoke()
                }
            },
    ) {
        val fraction = ((currentValue - valueRange.start) / span).coerceIn(0f, 1f)
        val cy = size.height / 2f
        val trackY = cy - trackHeight.toPx() / 2f
        val thumbX = thumbRadius.toPx() + fraction * (size.width - thumbRadius.toPx() * 2)

        drawRoundRect(
            color = trackColor,
            topLeft = Offset(0f, trackY),
            size = Size(size.width, trackHeight.toPx()),
            cornerRadius = CornerRadius(trackHeight.toPx() / 2f),
        )
        drawRoundRect(
            color = fillColor,
            topLeft = Offset(0f, trackY),
            size = Size(thumbX, trackHeight.toPx()),
            cornerRadius = CornerRadius(trackHeight.toPx() / 2f),
        )
        drawCircle(color = fillColor, radius = thumbRadius.toPx(), center = Offset(thumbX, cy))
        drawCircle(
            color = thumbRingColor,
            radius = thumbRadius.toPx(),
            center = Offset(thumbX, cy),
            style = Stroke(width = ThumbRing.toPx()),
        )
    }
}

private val ThumbDiameter = 18.dp
private val ThumbRing = 2.dp
private val TrackHeight = 6.dp
private val TouchHeight = 44.dp
