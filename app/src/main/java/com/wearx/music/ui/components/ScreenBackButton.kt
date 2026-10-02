package com.wearx.music.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.Icon
import com.wearx.music.R

/**
 * A back affordance for a page one level below another, built on Wear Material 3's [EdgeButton].
 *
 * Wear OS has no back BUTTON — leaving a screen is the system edge swipe that `AppScaffold` wires
 * up — so what this is for is discoverability: a gesture-only way out of a deep screen is invisible
 * until you know it exists, and the settings pages are where that costs the most.
 *
 * The component is native; only the placement is ours. `ScreenScaffold` HAS an `edgeButton` slot, but
 * it is on the internal overload — the public one (checked against the compiled signature of
 * compose-material3 1.7.0, the newest release) does not expose it — so the button is placed here
 * instead, at the bottom of the screen where [EdgeButton] is designed to live and where a thumb
 * rests.
 *
 * **The list must leave room for it.** In the scaffold's own layout the button claims the space below
 * the list and the list is given `edgeButtonSpacing` above it, with the list's bottom padding
 * dropped. Here the caller gets the same contract: give the list a bottom spacer of about
 * [BackButtonReservedHeight] so the last row is never trapped underneath the button.
 */
@Composable
fun BoxScope.ScreenBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EdgeButton(
        onClick = onClick,
        buttonSize = EdgeButtonSize.Small,
        modifier = modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth(),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.action_back),
        )
    }
}

/**
 * The space a list should leave below its last item so [ScreenBackButton] never covers it.
 *
 * `EdgeButtonSize.Small` is 56 dp tall; the gap between the two is what makes it look deliberate
 * rather than flush.
 */
val BackButtonReservedHeight = 72.dp
