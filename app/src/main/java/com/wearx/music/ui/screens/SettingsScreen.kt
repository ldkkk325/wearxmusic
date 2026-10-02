package com.wearx.music.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.ButtonGroup
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Slider
import androidx.wear.compose.material3.Text
import com.wearx.music.R
import com.wearx.music.data.AppSettings
import com.wearx.music.data.ThemeMode
import com.wearx.music.ui.components.MorphGroupIconButton
import com.wearx.music.ui.components.NoEdgeFadeScaling
import kotlin.math.roundToInt

/** Labels for [AppSettings.UI_SCALE_CHOICES], index-aligned. */
/** Turns the stored blur index into the wording shown under the slider. */
@Composable
private fun blurLabel(index: Int): String {
    val dp = AppSettings.MORPH_BLUR_CHOICES[index]
    return if (dp <= 0f) {
        stringResource(R.string.settings_morph_blur_off)
    } else {
        "${dp.toInt()} dp"
    }
}

private val UI_SCALE_LABELS = listOf(
    R.string.ui_scale_small,
    R.string.ui_scale_normal,
    R.string.ui_scale_large,
    R.string.ui_scale_xlarge,
)

/**
 * Settings: motion preference, UI scale, the repeat mode a new queue starts in, and an about block.
 *
 * Every choice is an **icon-only** button with the current value spelled out as a single line of
 * text above it. Wear buttons are small enough that a multi-character label gets clipped — icons
 * plus one status line never truncate and read better on a round screen.
 */
@Composable
fun SettingsScreen(
    settings: AppSettings,
    versionName: String,
    onReduceMotionChange: (Boolean) -> Unit,
    onUiScaleChange: (Float) -> Unit,
    onMorphBlurChange: (Int) -> Unit,
    onTransitionDurationChange: (Int) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
) {
    val scrollState = rememberScalingLazyListState()

    val scales = AppSettings.UI_SCALE_CHOICES
    val scaleIndex = scales.indexOfFirst { it == settings.uiScale }.takeIf { it >= 0 } ?: 1

    ScreenScaffold(scrollState = scrollState) { padding ->
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = scrollState,
            scalingParams = NoEdgeFadeScaling,
            contentPadding = padding,
        ) {
            item {
                ListHeader {
                    Text(
                        text = stringResource(R.string.settings_title),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.settings_reduce_motion),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(
                            if (settings.reduceMotion) {
                                R.string.settings_state_on
                            } else {
                                R.string.settings_state_off
                            },
                        ),
                        style = MaterialTheme.typography.bodyExtraSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.settings_reduce_motion_hint),
                        style = MaterialTheme.typography.bodyExtraSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    ButtonGroup(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        MorphGroupIconButton(
                            onClick = { onReduceMotionChange(true) },
                            active = settings.reduceMotion,
                            icon = { Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.settings_switch_on)) },
                        )
                        MorphGroupIconButton(
                            onClick = { onReduceMotionChange(false) },
                            active = !settings.reduceMotion,
                            icon = { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.settings_switch_off)) },
                        )
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.settings_dynamic_color),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(
                            if (settings.dynamicColor) {
                                R.string.settings_state_on
                            } else {
                                R.string.settings_state_off
                            },
                        ),
                        style = MaterialTheme.typography.bodyExtraSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.settings_dynamic_color_hint),
                        style = MaterialTheme.typography.bodyExtraSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    ButtonGroup(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        MorphGroupIconButton(
                            onClick = { onDynamicColorChange(true) },
                            active = settings.dynamicColor,
                            icon = { Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.settings_switch_on)) },
                        )
                        MorphGroupIconButton(
                            onClick = { onDynamicColorChange(false) },
                            active = !settings.dynamicColor,
                            icon = { Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.settings_switch_off)) },
                        )
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.settings_theme_mode),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(
                            R.string.settings_theme_mode_value,
                            stringResource(
                                when (settings.themeMode) {
                                    ThemeMode.SYSTEM -> R.string.theme_system
                                    ThemeMode.DARK -> R.string.theme_dark
                                    ThemeMode.LIGHT -> R.string.theme_light
                                },
                            ),
                        ),
                        style = MaterialTheme.typography.bodyExtraSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.settings_theme_mode_hint),
                        style = MaterialTheme.typography.bodyExtraSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Slider(
                        value = settings.themeMode.ordinal,
                        onValueChange = { index ->
                            onThemeModeChange(
                                ThemeMode.entries[index.coerceIn(ThemeMode.entries.indices)],
                            )
                        },
                        valueProgression = 0..ThemeMode.entries.lastIndex,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.settings_transition_duration),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(
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
                        style = MaterialTheme.typography.bodyExtraSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.settings_transition_duration_hint),
                        style = MaterialTheme.typography.bodyExtraSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Slider(
                        value = settings.transitionDurationIndex,
                        onValueChange = onTransitionDurationChange,
                        valueProgression = 0..AppSettings.TRANSITION_DURATION_CHOICES.lastIndex,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.settings_morph_blur),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(
                            R.string.settings_morph_blur_value,
                            blurLabel(settings.morphBlurIndex),
                        ),
                        style = MaterialTheme.typography.bodyExtraSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.settings_morph_blur_hint),
                        style = MaterialTheme.typography.bodyExtraSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    // A Slider, not a Stepper: the Stepper forces its content to fill the available
                    // space and vanishes when it cannot, whereas a Slider lays out inline and is
                    // already used on the volume page. The Int overload, because the Float one
                    // derives its step from `steps` and will not call back for a value that sits
                    // between two stops.
                    Slider(
                        value = settings.morphBlurIndex,
                        onValueChange = onMorphBlurChange,
                        valueProgression = 0..AppSettings.MORPH_BLUR_CHOICES.lastIndex,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.settings_ui_scale),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(
                            R.string.settings_ui_scale_value,
                            stringResource(UI_SCALE_LABELS[scaleIndex]),
                            (settings.uiScale * 100).roundToInt(),
                        ),
                        style = MaterialTheme.typography.bodyExtraSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.settings_ui_scale_hint),
                        style = MaterialTheme.typography.bodyExtraSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    // A plain two-button group, deliberately NOT Wear M3's Stepper. Stepper forces
                    // its content to fill the available space (`modifier.fillMaxSize()` on the
                    // Column it builds) because it is designed for a whole screen — drop it into a
                    // Card inside a list item and the available height resolves to nothing, so the
                    // three parts collapse and the control is simply not there. Tried and reverted.
                    ButtonGroup(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        MorphGroupIconButton(
                            onClick = {
                                onUiScaleChange(scales[(scaleIndex - 1).coerceAtLeast(0)])
                            },
                            enabled = scaleIndex > 0,
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.Remove,
                                    contentDescription = stringResource(R.string.settings_scale_down),
                                )
                            },
                        )
                        MorphGroupIconButton(
                            onClick = {
                                onUiScaleChange(
                                    scales[(scaleIndex + 1).coerceAtMost(scales.lastIndex)],
                                )
                            },
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

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(R.string.settings_about),
                            style = MaterialTheme.typography.titleSmall,
                        )
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
            }
        }
    }
}
