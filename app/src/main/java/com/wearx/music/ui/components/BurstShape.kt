package com.wearx.music.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.lerp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath

/**
 * Number of spikes on the playing face.
 *
 * Ten by request. A star alternates between two radii, so `numVerticesPerRadius` is the spike count
 * and the polygon has twice that many vertices.
 */
private const val SpikeCount = 10

/** How far the valleys are pulled in, relative to the tips, once fully playing. */
private const val BurstInnerRatio = 0.82f

/**
 * Valley radius at rest. Must stay strictly below the tip radius — `RoundedPolygon.star` throws
 * `IllegalArgumentException("innerRadius must be less than radius")` otherwise, which is exactly
 * what passing `innerRadius = radius` did. At 0.995 of the tip radius the polygon reads as a
 * circle, so the resting face still looks round.
 */
private const val RestInnerRatio = 0.995f

private const val RestRoundingRatio = 0.5f
private const val BurstRoundingRatio = 0.16f
private const val RestInnerRoundingRatio = 0.5f
private const val BurstInnerRoundingRatio = 0.10f

/**
 * A button outline that morphs from a circle into a spiked burst while [playing].
 *
 * Both faces are the *same* star, so the morph is purely an interpolation of `innerRadius` and the
 * two corner roundings — no `Morph` is needed and there is nothing that can fail to line up. The
 * polygon is rebuilt per outline request (it is only 20 vertices) rather than cached, because unlike
 * a `Morph`-based implementation its geometry depends on the animation progress.
 *
 * This uses the same primitive Wear M3 uses internally (`androidx.graphics.shapes`); its own helper
 * classes are `internal`, so the recipe is reproduced here. The progress is read through a lambda
 * inside `createOutline` so the layer subscribes to it and re-runs when it changes — the same shape
 * as Wear's `AnimatedMorphShape`.
 */
@Composable
fun rememberBurstShape(playing: Boolean): Shape {
    val progress by animateFloatAsState(
        targetValue = if (playing) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "burstProgress",
    )
    return remember { BurstShape { progress } }
}

private class BurstShape(private val progress: () -> Float) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        if (size.minDimension <= 0f) return Outline.Rectangle(Rect(Offset.Zero, size))

        // Reading the state here is what ties the layer to the animation.
        val t = progress().coerceIn(0f, 1f)
        val radius = size.minDimension / 2f

        // Built directly in the button's pixel space — the polygon takes an explicit centre — so
        // the resulting path needs no extra scaling and cannot end up off-centre.
        val polygon = RoundedPolygon.star(
            numVerticesPerRadius = SpikeCount,
            radius = radius,
            innerRadius = radius * lerp(RestInnerRatio, BurstInnerRatio, t),
            rounding = CornerRounding(radius * lerp(RestRoundingRatio, BurstRoundingRatio, t)),
            innerRounding = CornerRounding(
                radius * lerp(RestInnerRoundingRatio, BurstInnerRoundingRatio, t),
            ),
            centerX = size.width / 2f,
            centerY = size.height / 2f,
        )
        return Outline.Generic(polygon.toPath().asComposePath())
    }
}
