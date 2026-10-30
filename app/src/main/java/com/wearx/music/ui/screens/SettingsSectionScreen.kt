package com.wearx.music.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
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
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.ButtonGroup
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SplitSwitchButton
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.wearx.music.R
import com.wearx.music.data.AppSettings
import com.wearx.music.data.ThemeMode
import com.wearx.music.ui.SettingsSection
import com.wearx.music.ui.components.MorphButton
import com.wearx.music.ui.components.MorphGroupIconButton
import com.wearx.music.ui.components.ScreenBackButton
import com.wearx.music.ui.components.SteplessSlider
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
    isRescanning: Boolean,
    onRescan: () -> Unit,
    onReduceMotionChange: (Boolean) -> Unit,
    onUiScaleChange: (Float) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onMorphBlurChange: (Int) -> Unit,
    onTransitionDurationChange: (Int) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onFontScaleChange: (Float) -> Unit,
    onFontWeightChange: (Float) -> Unit,
    onFadeEnabledChange: (Boolean) -> Unit,
    onFadeDurationChange: (Int) -> Unit,
    onLyricsBlurChange: (Float) -> Unit,
    onBack: () -> Unit,
) {
    val scrollState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(
        scrollState = scrollState,
        edgeButton = { ScreenBackButton(onClick = onBack) },
    ) { padding ->
        TransformingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = scrollState,
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
                    transformationSpec = transformationSpec,
                    settings = settings,
                    onUiScaleChange = onUiScaleChange,
                    onDynamicColorChange = onDynamicColorChange,
                    onThemeModeChange = onThemeModeChange,
                    onFontScaleChange = onFontScaleChange,
                    onFontWeightChange = onFontWeightChange,
                )

                SettingsSection.MOTION -> motionSection(
                    transformationSpec = transformationSpec,
                    settings = settings,
                    onReduceMotionChange = onReduceMotionChange,
                    onTransitionDurationChange = onTransitionDurationChange,
                    onMorphBlurChange = onMorphBlurChange,
                    onLyricsBlurChange = onLyricsBlurChange,
                )

                SettingsSection.PLAYBACK -> playbackSection(
                    transformationSpec = transformationSpec,
                    settings = settings,
                    isRescanning = isRescanning,
                    onRescan = onRescan,
                    onFadeEnabledChange = onFadeEnabledChange,
                    onFadeDurationChange = onFadeDurationChange,
                )
                SettingsSection.ABOUT -> aboutSection(transformationSpec, settings, versionName)
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

private fun TransformingLazyColumnScope.appearanceSection(
    transformationSpec: TransformationSpec,
    settings: AppSettings,
    onUiScaleChange: (Float) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onFontScaleChange: (Float) -> Unit,
    onFontWeightChange: (Float) -> Unit,
) {
    val scales = AppSettings.UI_SCALE_CHOICES
    val scaleIndex = scales.indexOfFirst { it == settings.uiScale }.takeIf { it >= 0 } ?: 1
    group(transformationSpec) {
        pickerRow(
            titleRes = R.string.settings_theme_mode,
            value = stringResource(
                when (settings.themeMode) {
                    ThemeMode.SYSTEM -> R.string.theme_system
                    ThemeMode.DARK -> R.string.theme_dark
                    ThemeMode.LIGHT -> R.string.theme_light
                },
            ),
            options = listOf(
                stringResource(R.string.theme_system),
                stringResource(R.string.theme_dark),
                stringResource(R.string.theme_light),
            ),
            selectedIndex = settings.themeMode.ordinal,
            onSelect = { onThemeModeChange(ThemeMode.entries[it]) },
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
        // MiSans is a variable font, so size and weight are real continuous values — not a
        // picker pretending to be one.
        continuousSliderRow(
            titleRes = R.string.settings_font_size,
            value = stringResource(
                R.string.settings_font_size_value,
                (settings.fontScale * 100).roundToInt(),
            ),
            currentValue = settings.fontScale,
            range = AppSettings.FONT_SCALE_RANGE,
            onValueChange = onFontScaleChange,
        )
        continuousSliderRow(
            titleRes = R.string.settings_font_weight,
            value = stringResource(
                R.string.settings_font_weight_value,
                settings.fontWeight.roundToInt(),
            ),
            currentValue = settings.fontWeight,
            range = AppSettings.FONT_WEIGHT_RANGE,
            onValueChange = onFontWeightChange,
        )
    }
}

private fun TransformingLazyColumnScope.motionSection(
    transformationSpec: TransformationSpec,
    settings: AppSettings,
    onReduceMotionChange: (Boolean) -> Unit,
    onTransitionDurationChange: (Int) -> Unit,
    onMorphBlurChange: (Int) -> Unit,
    onLyricsBlurChange: (Float) -> Unit,
) {
    group(transformationSpec) {
        switchRow(
            titleRes = R.string.settings_reduce_motion,
            checked = settings.reduceMotion,
            onCheckedChange = onReduceMotionChange,
        )
        pickerRow(
            titleRes = R.string.settings_transition_duration,
            value = AppSettings.TRANSITION_DURATION_LABELS[
                settings.transitionDurationIndex.coerceIn(AppSettings.TRANSITION_DURATION_LABELS.indices)
            ],
            options = AppSettings.TRANSITION_DURATION_LABELS,
            selectedIndex = settings.transitionDurationIndex
                .coerceIn(AppSettings.TRANSITION_DURATION_LABELS.indices),
            onSelect = onTransitionDurationChange,
        )
        pickerRow(
            titleRes = R.string.settings_morph_blur,
            value = if (settings.morphBlurIndex <= 0) {
                stringResource(R.string.settings_morph_blur_off)
            } else {
                "${AppSettings.MORPH_BLUR_CHOICES[settings.morphBlurIndex].toInt()} dp"
            },
            options = AppSettings.MORPH_BLUR_CHOICES.map { blur ->
                if (blur <= 0f) stringResource(R.string.settings_morph_blur_off) else "${blur.toInt()} dp"
            },
            selectedIndex = settings.morphBlurIndex.coerceIn(AppSettings.MORPH_BLUR_CHOICES.indices),
            onSelect = onMorphBlurChange,
        )
        continuousSliderRow(
            titleRes = R.string.settings_lyrics_blur,
            value = if (settings.lyricsBlurRadius <= 0f) {
                stringResource(R.string.settings_morph_blur_off)
            } else {
                "${"%.1f".format(settings.lyricsBlurRadius)} dp"
            },
            currentValue = settings.lyricsBlurRadius,
            range = AppSettings.LYRICS_BLUR_RADIUS_RANGE,
            onValueChange = onLyricsBlurChange,
        )
    }
}

/**
 * Library scanning lives here rather than only behind the empty state: once a library has any
 * song at all, the old empty-state button was the only rescan entry point — and new files could
 * never be picked up without wiping app data. The fade is the one playback behaviour that is
 * genuinely configurable, so it gets the section's other slots.
 */
private fun TransformingLazyColumnScope.playbackSection(
    transformationSpec: TransformationSpec,
    settings: AppSettings,
    isRescanning: Boolean,
    onRescan: () -> Unit,
    onFadeEnabledChange: (Boolean) -> Unit,
    onFadeDurationChange: (Int) -> Unit,
) {
    group(transformationSpec) {
        switchRow(
            titleRes = R.string.settings_fade,
            checked = settings.playbackFadeEnabled,
            onCheckedChange = onFadeEnabledChange,
        )
        if (settings.playbackFadeEnabled) {
            pickerRow(
                titleRes = R.string.settings_fade_duration,
                value = AppSettings.FADE_DURATION_LABELS[
                    settings.playbackFadeDurationIndex
                        .coerceIn(AppSettings.FADE_DURATION_LABELS.indices)
                ],
                options = AppSettings.FADE_DURATION_LABELS,
                selectedIndex = settings.playbackFadeDurationIndex
                    .coerceIn(AppSettings.FADE_DURATION_LABELS.indices),
                onSelect = onFadeDurationChange,
            )
        }
        MorphButton(
            onClick = onRescan,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isRescanning,
            icon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
            label = {
                Text(stringResource(if (isRescanning) R.string.rescanning else R.string.action_rescan))
            },
        )
        lrcPermissionRow()
    }
}

/**
 * The All Files Access grant that makes `.lrc` sidecar lyrics readable at all. The system screen
 * is the only place it can be given, so this row reports the current state and hands off to it.
 */
@Composable
private fun ColumnScope.lrcPermissionRow() {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(Environment.isExternalStorageManager()) }

    // The grant is made on another screen; re-reading on resume is what keeps the row honest
    // after the user comes back.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = Environment.isExternalStorageManager()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Spacer(Modifier.height(8.dp))
    Card(
        onClick = { context.openAllFilesAccessSettings() },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)) {
            Text(
                text = stringResource(R.string.settings_lrc_title),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(if (granted) R.string.settings_lrc_on else R.string.settings_lrc_off),
                style = MaterialTheme.typography.bodyExtraSmall,
                color = if (granted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Hands off to the system page that can grant (and revoke) All Files Access for this app. */
private fun Context.openAllFilesAccessSettings() {
    val specific = Intent(
        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
        Uri.parse("package:$packageName"),
    )
    // Some builds only offer the (much clumsier) list-every-app page; fall back rather than crash.
    runCatching { startActivity(specific.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .onFailure {
            runCatching {
                startActivity(
                    Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
}

private fun TransformingLazyColumnScope.aboutSection(transformationSpec: TransformationSpec, settings: AppSettings, versionName: String) {
    group(transformationSpec) {
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
private fun TransformingLazyColumnScope.group(
    transformationSpec: TransformationSpec,
    content: @Composable ColumnScope.() -> Unit,
) {
    item {
        Card(
            modifier = Modifier.transformedHeight(this, transformationSpec),
            transformation = SurfaceTransformation(transformationSpec),
        ) {
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

/** A titled row showing the current choice; tapping it opens an [AlertDialog] of options. */
@Composable
private fun ColumnScope.pickerRow(
    titleRes: Int,
    value: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    val title = stringResource(titleRes)

    Card(
        onClick = { showDialog = true },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyExtraSmall,
                    color = MaterialTheme.colorScheme.primary,
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

    if (showDialog) {
        AlertDialog(
            visible = true,
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = title,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            content = {
                itemsIndexed(options) { index, option ->
                    val selected = index == selectedIndex
                    Card(
                        onClick = {
                            showDialog = false
                            onSelect(index)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = option,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            },
        )
    }
}

/** A title, its current value, and a stepless slider over a continuous range. */
@Composable
private fun ColumnScope.continuousSliderRow(
    titleRes: Int,
    value: String,
    currentValue: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    var draft by remember(currentValue) { mutableStateOf(currentValue) }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(text = stringResource(titleRes), style = MaterialTheme.typography.titleSmall)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyExtraSmall,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        SteplessSlider(
            value = draft,
            valueRange = range,
            steps = 0,
            onValueChange = { draft = it },
            onValueChangeFinished = { onValueChange(draft.coerceIn(range)) },
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
