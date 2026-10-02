package com.wearx.music.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
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
 * **It belongs in the scrolling content, not floating over it.** In the scaffold's own layout the
 * button claims the space *below* the list and the list is given a gap above it — it is laid out
 * after the content, not on top of it, so it only comes into view when you have scrolled to the end.
 * An overlay pinned to the bottom of the screen sits over the content the whole time, which is a
 * different thing and reads as one.
 *
 * So this is a plain composable to be added as the LAST item of the list, and `ScreenScaffold` is
 * used WITHOUT an `edgeButton` slot — the public one does not have one anyway (checked against the
 * compiled signature of compose-material3 1.7.0, the newest release; it exists only on the internal
 * overload).
 *
 * For the pages whose body is not a list, put it after the last child of the column instead. Either
 * way it ends up in the flow, at the bottom, exactly where the empty space was.
 */
@Composable
fun ScreenBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EdgeButton(
        onClick = onClick,
        buttonSize = EdgeButtonSize.Small,
        modifier = modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.action_back),
        )
    }
}

/**
 * The gap to leave above the button, matching the scaffold's own `edgeButtonSpacing` idea: the button
 * hugs the end of the content and the content stops short of it, so the two read as neighbours
 * rather than as a button pasted onto a list.
 */
val BackButtonSpacing = 12.dp
