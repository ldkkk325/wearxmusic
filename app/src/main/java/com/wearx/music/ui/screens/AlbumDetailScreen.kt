package com.wearx.music.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.wearx.music.R
import com.wearx.music.data.model.Album
import com.wearx.music.data.model.Track
import com.wearx.music.ui.components.AlbumArtwork
import com.wearx.music.ui.components.MarqueeText
import com.wearx.music.ui.components.MorphButton
import com.wearx.music.ui.components.NoEdgeFadeScaling
import com.wearx.music.util.formatDuration

/** Album detail: hero cover, play-all action, and the track list. */
@Composable
fun AlbumDetailScreen(
    album: Album,
    nowPlayingMediaId: String?,
    onPlayAll: () -> Unit,
    onTrackClick: (Track) -> Unit,
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
                AlbumArtwork(
                    artworkUri = album.artUri,
                    contentDescription = album.title,
                    size = 88.dp,
                    cornerRadius = 20.dp,
                )
            }
            item {
                Text(
                    text = album.title.ifBlank { stringResource(R.string.label_unknown_album) },
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            item {
                Text(
                    text = album.artist.ifBlank { stringResource(R.string.label_unknown_artist) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            item {
                MorphButton(
                    onClick = onPlayAll,
                    modifier = Modifier.fillMaxWidth(),
                    icon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                    label = { Text(stringResource(R.string.action_play)) },
                )
            }
            itemsIndexed(album.tracks, key = { _, track -> track.id }) { index, track ->
                Card(
                    onClick = { onTrackClick(track) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.size(34.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (track.id.toString() == nowPlayingMediaId) {
                                Icon(
                                    imageVector = Icons.Filled.GraphicEq,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            } else {
                                Text(
                                    text = track.trackNumber.takeIf { it > 0 }?.toString() ?: "♪",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        MarqueeText(
                            text = track.title.ifBlank { stringResource(R.string.label_unknown_album) },
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = formatDuration(track.durationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
