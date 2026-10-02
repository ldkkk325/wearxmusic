package com.wearx.music.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Shuffle
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
import androidx.wear.compose.material3.Text
import com.wearx.music.R
import com.wearx.music.data.PlaybackMode
import com.wearx.music.playback.PlayerUiState
import com.wearx.music.ui.components.MorphGroupIconButton
import com.wearx.music.ui.components.BackButtonSpacing
import com.wearx.music.ui.components.NoEdgeFadeScaling
import com.wearx.music.ui.components.ScreenBackButton
import com.wearx.music.ui.labelRes

private const val SEEK_STEP_MS = 10_000L

/**
 * Secondary playback controls — the seek steps and the repeat mode.
 *
 * They used to sit on the now-playing screen, which made that screen's control row four items
 * wide; moving them here leaves room for two comfortable buttons instead.
 */
@Composable
fun MoreScreen(
    state: PlayerUiState,
    onSeekBy: (Long) -> Unit,
    onCyclePlaybackMode: () -> Unit,
    onBack: () -> Unit,
) {
    val scrollState = rememberScalingLazyListState()

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
                        text = stringResource(R.string.more_title),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.more_seek),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.height(6.dp))
                    ButtonGroup(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        MorphGroupIconButton(
                            onClick = { onSeekBy(-SEEK_STEP_MS) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.Replay10,
                                    contentDescription = stringResource(R.string.more_seek_back),
                                )
                            },
                        )
                        MorphGroupIconButton(
                            onClick = { onSeekBy(SEEK_STEP_MS) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.Forward10,
                                    contentDescription = stringResource(R.string.more_seek_forward),
                                )
                            },
                        )
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.more_mode),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(
                            R.string.more_mode_value,
                            stringResource(state.playbackMode.labelRes()),
                        ),
                        style = MaterialTheme.typography.bodyExtraSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(6.dp))
                    ButtonGroup(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        MorphGroupIconButton(
                            onClick = onCyclePlaybackMode,
                            active = true,
                            icon = {
                                AnimatedContent(
                                    targetState = state.playbackMode,
                                    transitionSpec = {
                                        (scaleIn(initialScale = 0.5f) + fadeIn()) togetherWith
                                            (scaleOut(targetScale = 0.5f) + fadeOut())
                                    },
                                    label = "playbackModeIcon",
                                ) { mode ->
                                    Icon(
                                        imageVector = when (mode) {
                                            PlaybackMode.SHUFFLE -> Icons.Filled.Shuffle
                                            PlaybackMode.REPEAT_ONE -> Icons.Filled.RepeatOne
                                            PlaybackMode.REPEAT_ALL -> Icons.Filled.Repeat
                                        },
                                        contentDescription = stringResource(R.string.more_mode),
                                    )
                                }
                            },
                        )
                    }
                }
            }

            // In the content flow at the end, not floating over it — see ScreenBackButton.
            item {
                Spacer(Modifier.height(BackButtonSpacing))
                ScreenBackButton(onClick = onBack)
            }
        }
    }
}
