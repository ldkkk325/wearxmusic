package com.wearx.music.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonColors
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ButtonGroupScope
import androidx.wear.compose.material3.CompactButton
import kotlin.math.roundToInt

/**
 * The Material 3 Expressive "press morph": a button rests as a pill and snaps to a squircle while
 * held. Wear M3's `Button`/`CompactButton` accept only a single static `shape`, so the morph is
 * driven here from the button's own interaction source and handed back as the `shape`.
 */
@Composable
fun rememberPressShape(interactionSource: InteractionSource): Shape {
    val pressed by interactionSource.collectIsPressedAsState()
    val cornerPercent by animateFloatAsState(
        targetValue = if (pressed) PressedCornerPercent else RestingCornerPercent,
        animationSpec = tween(durationMillis = 150, easing = PressEasing),
        label = "pressCornerPercent",
    )
    return RoundedCornerShape(cornerPercent.roundToInt())
}

/** A labelled button that morphs its shape while pressed. */
@Composable
fun MorphButton(
    onClick: () -> Unit,
    label: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    icon: @Composable BoxScope.() -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val tapFeedback = rememberTapFeedback()
    Button(
        onClick = {
            tapFeedback()
            onClick()
        },
        modifier = modifier,
        enabled = enabled,
        shape = rememberPressShape(interactionSource),
        colors = colors,
        interactionSource = interactionSource,
        icon = icon,
        label = label,
    )
}

/**
 * A [CompactButton] for use inside a [ButtonGroup].
 *
 * `Modifier.animateWidth` (the group's expand-on-press) is opt-in in Wear M3 and must be handed the
 * *same* interaction source the button reports on, so the source is created here rather than by the
 * caller. Without this the button only scales; with it the neighbours give way as it grows.
 */
@Composable
fun ButtonGroupScope.MorphGroupIconButton(
    onClick: () -> Unit,
    icon: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    active: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val tapFeedback = rememberTapFeedback()
    CompactButton(
        onClick = {
            tapFeedback()
            onClick()
        },
        modifier = modifier.weight(1f).animateWidth(interactionSource),
        enabled = enabled,
        shape = rememberPressShape(interactionSource),
        colors = if (active) {
            ButtonDefaults.buttonColors()
        } else {
            ButtonDefaults.filledTonalButtonColors()
        },
        interactionSource = interactionSource,
        icon = icon,
    )
}

/** A labelled [Button] for use inside a [ButtonGroup], with the same press morph. */
@Composable
fun ButtonGroupScope.MorphGroupButton(
    onClick: () -> Unit,
    label: @Composable RowScope.() -> Unit,
    icon: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    active: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val tapFeedback = rememberTapFeedback()
    Button(
        onClick = {
            tapFeedback()
            onClick()
        },
        modifier = modifier.weight(1f).animateWidth(interactionSource),
        enabled = enabled,
        shape = rememberPressShape(interactionSource),
        colors = if (active) {
            ButtonDefaults.buttonColors()
        } else {
            ButtonDefaults.filledTonalButtonColors()
        },
        interactionSource = interactionSource,
        icon = icon,
        label = label,
    )
}

/**
 * Press feedback as a small scale, for buttons whose *outline* is already driven by something else
 * (the play button morphs into its burst while playing, so it cannot also morph its corners).
 */
@Composable
fun rememberPressScale(interactionSource: InteractionSource): Float {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = tween(durationMillis = 150, easing = PressEasing),
        label = "pressScale",
    )
    return scale
}

/**
 * Press feedback uses the same decelerating curve as the page transitions, on a short clock: a
 * press has to answer the finger at once, so a bouncy spring here only ever showed up as the shape
 * or the scale overshooting on release — which is the one thing a press should not do.
 */
private val PressEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

private const val RestingCornerPercent = 50f
private const val PressedCornerPercent = 26f
