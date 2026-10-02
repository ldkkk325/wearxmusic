package com.wearx.music.ui.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.MaterialTheme
import com.wearx.music.R

/**
 * A back chevron for a page that is one level below another, placed at the leading edge.
 *
 * Wear OS has no back BUTTON — leaving a screen is the system edge swipe, and `AppScaffold` wires it
 * up. What it has no equivalent for is *discoverability*: a gesture-only way out of a deep screen is
 * invisible until you know it exists, which matters most on the settings pages where a single wrong
 * tap can change something you did not mean to.
 *
 * So this is deliberately NOT presented as a native component, because it is not one. Wear M3 1.7.0 —
 * the newest release — has no back affordance in either `compose-material3` or the older
 * `androidx.wear.compose:material` package, and its public `ScreenScaffold` does not expose the
 * internal `edgeButton` slot. This is a plain Wear M3 [IconButton] with the standard chevron, sized to
 * a comfortable touch target and tinted quietly so it reads as navigation rather than as a primary
 * action.
 *
 * It is meant to be placed with [BoxScope.align] at [Alignment.TopStart] of a `ScreenScaffold`'s
 * content, where it stays put while the page scrolls.
 */
@Composable
fun BoxScope.ScreenBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .align(Alignment.TopStart)
            // Nudged in from the edge and down under the time text, which AppScaffold draws on top.
            .padding(start = 6.dp, top = 20.dp)
            .size(BackButtonSize),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.action_back),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(BackButtonIconSize),
        )
    }
}

/** Comfortably above the 48 dp minimum rather than exactly at it. */
private val BackButtonSize = 40.dp

private val BackButtonIconSize = 20.dp
