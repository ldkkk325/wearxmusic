package com.wearx.music.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.Icon
import com.wearx.music.R

/**
 * The back affordance for a page one level below another, built on Wear Material 3's [EdgeButton].
 *
 * Wear OS has no back BUTTON — leaving a screen is the system edge swipe that `AppScaffold` wires
 * up — so what this is for is discoverability: a gesture-only way out of a deep screen is invisible
 * until you know it exists, and the settings pages are where that costs the most.
 *
 * Pass this into `ScreenScaffold`'s **`edgeButton` slot**. That is the supported placement: the
 * scaffold pins the button at the bottom of the screen and derives the content's bottom spacing
 * from `edgeButtonSpacing`, so it is an element of the page rather than a floating overlay. Do not
 * put it in the list and do not pin it with `align` — both of those look detached on a round dial.
 */
@Composable
fun ScreenBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EdgeButton(
        onClick = onClick,
        buttonSize = EdgeButtonSize.Small,
        modifier = modifier,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.action_back),
        )
    }
}
