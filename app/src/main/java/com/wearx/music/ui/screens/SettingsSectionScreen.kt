package com.wearx.music.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.ButtonGroup
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Slider
import androidx.wear.compose.material3.SplitSwitchButton
import androidx.wear.compose.material3.Text
import com.wearx.music.R
import com.wearx.music.data.AppSettings
import com.wearx.music.data.ThemeMode
import com.wearx.music.ui.SettingsSection
import com.wearx.music.ui.components.MorphGroupIconButton
import com.wearx.music.ui.components.BackButtonSpacing
import com.wearx.music.ui.components.NoEdgeFadeScaling
import com.wearx.music.ui.components.ScreenBackButton
import kotlin.math.roundToInt

/**
 * One settings category, on its own page.
 *
 * Reached from [SettingsScreen]. Every section takes only the settings it uses, so adding a setting
 * means touching exactly one of the private composables below and nothing else — the route, the index
 * and the plumbing between them do not change.
 */
@Composable
fun SettingsSectionScreen(
    section: SettingsSection,
    settings: AppSettings,
    versionName: String,
    onReduceMotionChange: (Boolean) -> Unit,
    onUiScaleChange: (Float) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onMorphBlurChange: (Int) -> Unit,
    onTransitionDurationChange: (Int) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onBack: () -> Unit,
) {
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
                        text = stringResource(section.titleRes()),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            when (section) {
                SettingsSection.APPEARANCE -> appearanceSection(
                    settings = settings,
                    onUiScaleChange = onUiScaleChange,
                    onDynamicColorChange = onDynamicColorChange,
                    onThemeModeChange = onThemeModeChange,
                )

                SettingsSection.MOTION -> motionSection(
                    settings = settings,
                    onReduceMotionChange = onReduceMotionChange,
                    onTransitionDurationChange = onTransitionDurationChange,
                    onMorphBlurChange = onMorphBlurChange,
                )

                SettingsSection.PLAYBACK -> playbackSection()
                SettingsSection.ABOUT -> aboutSection(settings = settings, versionName = versionName)
            }

            item {
                // In the content flow at the end, not floating over it — see ScreenBackButton.
                Spacer(Modifier.height(BackButtonSpacing))
                ScreenBackButton(onClick = onBack)
            }
        }
    }
}

private fun SettingsSection.titleRes(): Int = when (this) {
    SettingsSection.APPEARANCE -> R.string.settings_group_appearance
    SettingsSection.MOTION -> R.string.settings_group_motion
    SettingsSection.PLAYBACK -> R.string.settings_group_playback
    SettingsSection.ABOUT -> R.string.settings_group_about
}

private fun ScalingLazyListScope.appearanceSection(
    settings: AppSettings,
    onUiScaleChange: (Float) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    val scales = AppSettings.UI_SCALE_CHOICES
    val scaleIndex = scales.indexOfFirst { it == settings.uiScale }.takeIf { it >= 0 } ?: 1
    group {
        sliderRow(
            titleRes = R.string.settings_theme_mode,
            value = stringResource(
                when (settings.themeMode) {
                    ThemeMode.SYSTEM -> R.string.theme_system
                    ThemeMode.DARK -> R.string.theme_dark
                    ThemeMode.LIGHT -> R.string.theme_light
                },
            ),
            valueIndex = settings.themeMode.ordinal,
            lastIndex = ThemeMode.entries.lastIndex,
            onValueChange = { onThemeModeChange(ThemeMode.entries[it]) },
        )
        switchRow(
            titleRes = R.string.settings_dynamic_color,
            checked = settings.dynamicColor,
            onCheckedChange = onDynamicColorChange,
        )
        scaleRow(
            scales = scales,
            scaleIndex = scaleIndex,
            onUiScaleChange = onUiScaleChange,
        )
    }
}

private fun ScalingLazyListScope.motionSection(
    settings: AppSettings,
    onReduceMotionChange: (Boolean) -> Unit,
    onTransitionDurationChange: (Int) -> Unit,
    onMorphBlurChange: (Int) -> Unit,
) {
    group {
        switchRow(
            titleRes = R.string.settings_reduce_motion,
            checked = settings.reduceMotion,
            onCheckedChange = onReduceMotionChange,
        )
        sliderRow(
            titleRes = R.string.settings_transition_duration,
            value = stringResource(
                R.string.settings_transition_duration_value,
                AppSettings.TRANSITION_DURATION_LABELS[
                    settings.transitionDurationIndex.coerceIn(
                        AppSettings.TRANSITION_DURATION_LABELS.indices,
                    )
                ],
                AppSettings.TRANSITION_DURATION_CHOICES[
                    settings.transitionDurationIndex.coerceIn(
                        AppSettings.TRANSITION_DURATION_CHOICES.indices,
                    )
                ],
            ),
            valueIndex = settings.transitionDurationIndex,
            lastIndex = AppSettings.TRANSITION_DURATION_CHOICES.lastIndex,
            onValueChange = onTransitionDurationChange,
        )
        sliderRow(
            titleRes = R.string.settings_morph_blur,
            value = if (settings.morphBlurIndex <= 0) {
                stringResource(R.string.settings_morph_blur_off)
            } else {
                "${AppSettings.MORPH_BLUR_CHOICES[settings.morphBlurIndex].toInt()} dp"
            },
            valueIndex = settings.morphBlurIndex,
            lastIndex = AppSettings.MORPH_BLUR_CHOICES.lastIndex,
            onValueChange = onMorphBlurChange,
        )
    }
}

/** Empty on purpose: nothing about playback is configurable yet, and this is where it will land. */
private fun ScalingLazyListScope.playbackSection() {
    group {
        Text(
            text = stringResource(R.string.settings_group_playback_summary),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun ScalingLazyListScope.aboutSection(settings: AppSettings, versionName: String) {
    group {
        Text(
            text = stringResource(R.string.settings_version, versionName),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.settings_about_hint),
            style = MaterialTheme.typography.bodyExtraSmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

/** One card holding the whole group; the section's own title is the list header above it. */
private fun ScalingLazyListScope.group(content: @Composable ColumnScope.() -> Unit) {
    item {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                content = content,
            )
        }
    }
}

/** A titled on/off row. The whole row toggles; the trailing switch is the affordance. */
@Composable
private fun ColumnScope.switchRow(
    titleRes: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val title = stringResource(titleRes)
    SplitSwitchButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        toggleContentDescription = title,
        onContainerClick = { onCheckedChange(!checked) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A title, its current value, and a slider over a fixed set of indices. */
@Composable
private fun ColumnScope.sliderRow(
    titleRes: Int,
    value: String,
    valueIndex: Int,
    lastIndex: Int,
    onValueChange: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(text = stringResource(titleRes), style = MaterialTheme.typography.titleSmall)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyExtraSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        // The Int overload with an explicit progression: the Float one computes its step as
        // `(max - min) / (steps + 1)` and will not call back for a value that falls between stops,
        // which for a 3- or 4-item setting is most of them.
        Slider(
            value = valueIndex.coerceIn(0, lastIndex),
            onValueChange = onValueChange,
            valueProgression = 0..lastIndex,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * UI scale keeps its pair of buttons rather than becoming a slider: the values are multipliers that
 * only make sense to somebody who already knows what 85 % looks like, and a slider that jumps
 * between 0.85 / 1 / 1.15 / 1.3 while the whole screen rescales under your finger is unpleasant to
 * aim at. Two targets are precise, and the disabled state shows which end you have reached.
 */
@Composable
private fun ColumnScope.scaleRow(
    scales: List<Float>,
    scaleIndex: Int,
    onUiScaleChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.settings_ui_scale),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${(scales[scaleIndex] * 100).roundToInt()}%",
                style = MaterialTheme.typography.bodyExtraSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(4.dp))
        ButtonGroup(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(0.dp),
        ) {
            MorphGroupIconButton(
                onClick = { onUiScaleChange(scales[(scaleIndex - 1).coerceAtLeast(0)]) },
                enabled = scaleIndex > 0,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Remove,
                        contentDescription = stringResource(R.string.settings_scale_down),
                    )
                },
            )
            MorphGroupIconButton(
                onClick = { onUiScaleChange(scales[(scaleIndex + 1).coerceAtMost(scales.lastIndex)]) },
                enabled = scaleIndex < scales.lastIndex,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.settings_scale_up),
                    )
                },
            )
        }
    }
}
