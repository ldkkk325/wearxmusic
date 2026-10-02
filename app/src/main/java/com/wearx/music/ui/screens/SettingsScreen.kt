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
import androidx.wear.compose.material3.Stepper
import androidx.wear.compose.material3.Text
import com.wearx.music.R
import com.wearx.music.data.AppSettings
import com.wearx.music.ui.components.MorphGroupIconButton
import com.wearx.music.ui.components.NoEdgeFadeScaling
import kotlin.math.roundToInt

/** Labels for [AppSettings.UI_SCALE_CHOICES], index-aligned. */
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
                    // Wear M3's own Stepper, which is three parts in one (decrease, value, increase)
                    // rather than the two-button group this used to be. On a 227 dp dial the saving
                    // matters, and it is the component the platform's own settings use.
                    Stepper(
                        value = scaleIndex.toFloat(),
                        onValueChange = { index ->
                            onUiScaleChange(scales[index.toInt().coerceIn(scales.indices)])
                        },
                        steps = scales.size - 1,
                        valueRange = 0f..(scales.size - 1).toFloat(),
                        decreaseIcon = {
                            Icon(
                                imageVector = Icons.Filled.Remove,
                                contentDescription = stringResource(R.string.settings_scale_down),
                            )
                        },
                        increaseIcon = {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = stringResource(R.string.settings_scale_up),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(UI_SCALE_LABELS[scaleIndex]),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
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
