package com.wearx.music.ui.components

import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.ScalingParams

/**
 * `ScalingLazyColumn`'s default `edgeAlpha` is 0.5 — items near the edges are faded halfway out,
 * which on a dark theme reads as though the card itself changed colour depending on where it sits.
 * Only the size falloff is kept, so a container looks the same wherever it is in the list.
 */
val NoEdgeFadeScaling: ScalingParams = ScalingLazyColumnDefaults.scalingParams(edgeAlpha = 1f)
