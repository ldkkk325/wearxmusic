package com.wearx.music.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.material3.ButtonGroup
import androidx.wear.compose.material3.ButtonGroupDefaults
import androidx.wear.compose.material3.Icon
import com.wearx.music.R

/**
 * Volume and settings shortcuts, plus an optional "more" entry — icon-only so nothing can be
 * clipped on a small screen.
 *
 * Lyrics are reached by swiping left on the now-playing screen, so they cost no slot here.
 */
@Composable
fun NavigationRow(
    onOpenVolume: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenMore: (() -> Unit)? = null,
) {
    ButtonGroup(
        modifier = modifier.fillMaxWidth(),
        contentPadding = ButtonGroupDefaults.fullWidthPaddings(),
    ) {
        if (onOpenMore != null) {
            MorphGroupIconButton(
                onClick = onOpenMore,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.MoreHoriz,
                        contentDescription = stringResource(R.string.nav_more),
                    )
                },
            )
        }
        MorphGroupIconButton(
            onClick = onOpenVolume,
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = stringResource(R.string.nav_volume),
                )
            },
        )
        MorphGroupIconButton(
            onClick = onOpenSettings,
            icon = {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.nav_settings),
                )
            },
        )
    }
}
