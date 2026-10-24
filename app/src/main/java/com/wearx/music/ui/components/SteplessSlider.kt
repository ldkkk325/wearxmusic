package com.wearx.music.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.height
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme

/**
 * A finger-draggable continuous slider.
 *
 * Wear M3's own `Slider` is a segmented control driven by the rotary and its +/− icons — it has
 * no drag at all — and a hand-rolled drag fought the vertical list for the gesture and animated
 * nothing. This is phone Material 3's `Slider` instead: proper touch slop so the list keeps
 * vertical scrolls, thumb/track animations, press feedback and accessibility, with the colours
 * swapped to the Wear scheme so it still belongs to this app.
 *
 * [steps] is the number of intervals between the ends (same as phone M3): 0 is stepless,
 * `steps = 3` over 0f..1f snaps to 0, 1/3, 2/3, 1. [onValueChangeFinished] fires once per
 * gesture, so callers can commit a setting without rebuilding the type scale on every frame.
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
    val interactionSource = remember { MutableInteractionSource() }
    val colors = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.primary,
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        disabledThumbColor = MaterialTheme.colorScheme.outline,
        disabledActiveTrackColor = MaterialTheme.colorScheme.outline,
        disabledInactiveTrackColor = MaterialTheme.colorScheme.surfaceContainer,
    )

    androidx.compose.material3.Slider(
        value = value,
        onValueChange = onValueChange,
        // Room for the thumb to grow on press without being clipped by the row padding.
        modifier = modifier.height(44.dp),
        enabled = true,
        valueRange = valueRange,
        steps = steps.coerceAtLeast(0),
        onValueChangeFinished = onValueChangeFinished,
        interactionSource = interactionSource,
        colors = colors,
    )
}
