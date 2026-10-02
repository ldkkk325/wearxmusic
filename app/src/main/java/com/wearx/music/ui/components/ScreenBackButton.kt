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
 * **It is a fixed region at the bottom of the page, and the content is given room above it.** Not a
 * list item, which would scroll away and only appear at the very end of the list, and not an
 * overlay, which would sit on top of the content for the whole time — the button is always there
 * and the content stops short of it.
 *
 * That is the arrangement `ScreenScaffold` uses: the button claims the space below the list and the
 * list is given a gap above it. The public `ScreenScaffold` does not expose its `edgeButton` slot
 * (checked against the compiled signature of compose-material3 1.7.0, the newest release — it
 * exists only on the internal overload), so both halves are done here: this is pinned to the bottom,
 * and each page pads its content by [BackButtonReservedHeight].
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
 * The height the page's content must leave at the bottom so it never runs under [ScreenBackButton].
 *
 * Wear's own numbers: `EdgeButtonSize.Small` is 56 dp and `ScreenScaffoldDefaults.EdgeButtonSpacing`
 * is 16 dp (both read from the library source). Their sum is added to the screen's normal bottom
 * padding rather than replacing it.
 */
val BackButtonReservedHeight = 56.dp + 16.dp
