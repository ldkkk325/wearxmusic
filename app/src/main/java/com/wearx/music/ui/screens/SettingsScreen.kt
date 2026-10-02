package com.wearx.music.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.wearx.music.R
import com.wearx.music.ui.SettingsSection
import com.wearx.music.ui.components.NoEdgeFadeScaling
import com.wearx.music.ui.components.rememberTapFeedback

/**
 * The settings index: one row per category, each opening its own page.
 *
 * The categories are the structure the settings are built on, and more are coming, so they get
 * destinations rather than being sections of one long scroll — [SettingsSectionPage] is where each
 * one lives, and a new setting means putting it in a section rather than adding a card here.
 */
@Composable
fun SettingsScreen(onOpenSection: (SettingsSection) -> Unit) {
    val scrollState = rememberScalingLazyListState()

    ScreenScaffold(scrollState = scrollState) { padding ->
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = scrollState,
            scalingParams = NoEdgeFadeScaling,
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            item {
                ListHeader {
                    Text(
                        text = stringResource(R.string.settings_title),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
            items(SETTINGS_SECTIONS) { entry ->
                SectionRow(entry = entry, onClick = { onOpenSection(entry.section) })
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun SectionRow(entry: SettingsSectionEntry, onClick: () -> Unit) {
    val tapFeedback = rememberTapFeedback()
    Card(
        onClick = {
            tapFeedback()
            onClick()
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = entry.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(entry.titleRes),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(entry.summaryRes),
                    style = MaterialTheme.typography.bodyExtraSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** One entry of the settings index: where it goes, and what it says on the way in. */
private class SettingsSectionEntry(
    val section: SettingsSection,
    val titleRes: Int,
    val summaryRes: Int,
    val icon: ImageVector,
)

private val SETTINGS_SECTIONS = listOf(
    SettingsSectionEntry(
        section = SettingsSection.APPEARANCE,
        titleRes = R.string.settings_group_appearance,
        summaryRes = R.string.settings_group_appearance_summary,
        icon = Icons.Filled.Palette,
    ),
    SettingsSectionEntry(
        section = SettingsSection.MOTION,
        titleRes = R.string.settings_group_motion,
        summaryRes = R.string.settings_group_motion_summary,
        icon = Icons.Filled.Animation,
    ),
    SettingsSectionEntry(
        section = SettingsSection.PLAYBACK,
        titleRes = R.string.settings_group_playback,
        summaryRes = R.string.settings_group_playback_summary,
        icon = Icons.Filled.PlayCircle,
    ),
    SettingsSectionEntry(
        section = SettingsSection.ABOUT,
        titleRes = R.string.settings_group_about,
        summaryRes = R.string.settings_group_about_summary,
        icon = Icons.Filled.Info,
    ),
)
