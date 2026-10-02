package com.wearx.music.ui.screens

import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.ButtonGroup
import androidx.wear.compose.material3.ButtonGroupDefaults
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.RevealValue
import androidx.wear.compose.material3.SwipeToReveal
import androidx.wear.compose.material3.SwipeToRevealDefaults
import androidx.wear.compose.material3.rememberRevealState
import androidx.wear.compose.material3.Text
import com.wearx.music.R
import kotlinx.coroutines.launch
import com.wearx.music.data.SortMode
import com.wearx.music.data.model.Album
import com.wearx.music.data.model.Track
import com.wearx.music.playback.PlayerUiState
import com.wearx.music.ui.components.AlbumArtwork
import com.wearx.music.ui.components.MarqueeText
import com.wearx.music.ui.components.MorphButton
import com.wearx.music.ui.components.MorphGroupIconButton
import com.wearx.music.ui.components.NoEdgeFadeScaling
import com.wearx.music.ui.components.rememberTapFeedback
import com.wearx.music.util.formatDuration

/**
 * Home screen.
 *
 * Tracks are the primary list (and the default tab), each row carrying its own cover. The top bar
 * is deliberately terse: the screen is only ~227 dp tall, so there is no page title and every
 * control is icon-only. With playback running, a now-playing row sits above the tabs.
 */
@Composable
fun LibraryScreen(
    albums: List<Album>,
    tracks: List<Track>,
    playback: PlayerUiState,
    artByTrackId: Map<Long, Uri?>,
    isRescanning: Boolean,
    onAlbumClick: (Album) -> Unit,
    onTrackClick: (Track) -> Unit,
    onPlayNext: (Track) -> Unit,
    sortMode: SortMode,
    onCycleSort: () -> Unit,
    onOpenVolume: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    onTogglePlay: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onRescan: () -> Unit,
) {
    val scrollState = rememberScalingLazyListState()
    var showTracks by rememberSaveable { mutableStateOf(true) }

    ScreenScaffold(scrollState = scrollState) { padding ->
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = scrollState,
            verticalArrangement = Arrangement.spacedBy(3.dp),
            scalingParams = NoEdgeFadeScaling,
            contentPadding = padding,
        ) {
            if (playback.hasMedia) {
                item {
                    NowPlayingBar(
                        playback = playback,
                        onOpen = onOpenNowPlaying,
                        onTogglePlay = onTogglePlay,
                    )
                }
            }

            // Two rows, because these are two different kinds of control squeezed into one. Five
            // across a 227 dp dial is ~42 dp per button, under the 48 dp Wear asks for, and the row
            // mixed a view-mode toggle (one of which is ALWAYS active) with three plain navigation
            // buttons (none of which ever is). Splitting them gives every button ~72 dp or more, and
            // each row now says one thing.
            item {
                ButtonGroup(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = ButtonGroupDefaults.fullWidthPaddings(),
                ) {
                    MorphGroupIconButton(
                        onClick = { showTracks = true },
                        active = showTracks,
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.MusicNote,
                                contentDescription = stringResource(R.string.tab_tracks),
                            )
                        },
                    )
                    MorphGroupIconButton(
                        onClick = { showTracks = false },
                        active = !showTracks,
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Album,
                                contentDescription = stringResource(R.string.tab_albums),
                            )
                        },
                    )
                    // A cycling button rather than a picker: the label says which order tapping
                    // switches TO, so it never needs a dialog, and the row stays three buttons.
                    MorphGroupIconButton(
                        onClick = onCycleSort,
                        active = sortMode != SortMode.TITLE,
                        icon = {
                            Icon(
                                imageVector = sortMode.icon,
                                contentDescription = stringResource(sortMode.labelRes),
                            )
                        },
                    )
                }
            }

            item {
                ButtonGroup(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = ButtonGroupDefaults.fullWidthPaddings(),
                ) {
                    MorphGroupIconButton(
                        onClick = onOpenSearch,
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = stringResource(R.string.nav_search),
                            )
                        },
                    )
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

            if (showTracks) {
                itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                    TrackRow(
                        track = track,
                        artUri = track.artUri ?: artByTrackId[track.id],
                        isCurrent = track.id.toString() == playback.mediaId,
                        isPlaying = playback.isPlaying,
                        onClick = { onTrackClick(track) },
                        onPlayNext = { onPlayNext(track) },
                    )
                }
            } else {
                itemsIndexed(albums, key = { _, album -> album.id }) { index, album ->
                    AlbumRow(
                        album = album,
                        onClick = { onAlbumClick(album) },
                    )
                }
            }

            if (albums.isEmpty() && tracks.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.empty_library),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item {
                    Text(
                        text = stringResource(R.string.empty_library_hint),
                        style = MaterialTheme.typography.bodyExtraSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                item {
                    MorphButton(
                        onClick = onRescan,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isRescanning,
                        icon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                        label = {
                            Text(
                                stringResource(
                                    if (isRescanning) R.string.rescanning else R.string.action_rescan,
                                ),
                            )
                        },
                    )
                }
            }

            item { Spacer(Modifier.height(18.dp)) }
        }
    }
}

/** Compact now-playing row: tapping the row opens the player, the button toggles playback. */
@Composable
private fun NowPlayingBar(
    playback: PlayerUiState,
    onOpen: () -> Unit,
    onTogglePlay: () -> Unit,
) {
    val tapFeedback = rememberTapFeedback()
    Card(
        onClick = {
            tapFeedback()
            onOpen()
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumArtwork(
                artworkUri = playback.artworkUri,
                contentDescription = playback.title,
                size = 36.dp,
                cornerRadius = 10.dp,
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                MarqueeText(
                    text = playback.title ?: stringResource(R.string.now_playing_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = playback.artist ?: stringResource(R.string.label_unknown_artist),
                    style = MaterialTheme.typography.bodyExtraSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            CompactButton(
                onClick = {
                    tapFeedback()
                    onTogglePlay()
                },
                icon = {
                    Icon(
                        imageVector = if (playback.isPlaying) {
                            Icons.Filled.Pause
                        } else {
                            Icons.Filled.PlayArrow
                        },
                        contentDescription = stringResource(
                            if (playback.isPlaying) R.string.action_pause else R.string.action_play,
                        ),
                    )
                },
            )
        }
    }
}

@Composable
private fun AlbumRow(album: Album, onClick: () -> Unit) {
    val tapFeedback = rememberTapFeedback()
    Card(
        onClick = {
            tapFeedback()
            onClick()
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumArtwork(
                artworkUri = album.artUri,
                contentDescription = album.title,
                size = 42.dp,
            )
            Spacer(Modifier.width(10.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = album.title.ifBlank { stringResource(R.string.label_unknown_album) },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = album.artist.ifBlank { stringResource(R.string.label_unknown_artist) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.label_track_count, album.trackCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * A library row that reveals a secondary action when swiped right-to-left.
 *
 * `SwipeToReveal` is Wear Material 3's own component (new in 1.7) and it is not used anywhere else
 * in the app, so this is its first use. It is right-to-left on purpose: the library already sits
 * inside a right-to-left predictive-back transition and the platform reserves the left edge for
 * swipe-to-dismiss, so the only free direction is this one.
 */
@Composable
private fun TrackRow(
    track: Track,
    artUri: Uri?,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onPlayNext: () -> Unit,
) {
    val revealState = rememberRevealState()
    val revealScope = rememberCoroutineScope()
    // The component's own contract: with a partially-revealed state allowed, the APP has to put the
    // state back to Covered. Without this the row stays parked half-swiped with its content pushed
    // aside, which is what made a track look like it had vanished and left a hole in the list.
    // `animateTo` suspends, so the action's click has to launch it rather than call it directly.
    fun closeReveal() {
        revealScope.launch { revealState.animateTo(RevealValue.Covered) }
    }

    SwipeToReveal(
        primaryAction = {
            PrimaryActionButton(
                onClick = {
                    closeReveal()
                    onPlayNext()
                },
                icon = { Icon(Icons.AutoMirrored.Filled.PlaylistPlay, contentDescription = null) },
                text = { Text(stringResource(R.string.action_play_next), maxLines = 1) },
                modifier = Modifier.height(SwipeToRevealDefaults.LargeActionButtonHeight),
            )
        },
        // Both entry points close the row: a full swipe is a different code path inside the
        // component and leaves it revealed too, so resetting only the button would fix half of it.
        onSwipePrimaryAction = {
            closeReveal()
            onPlayNext()
        },
        revealState = revealState,
        // Full swipe fires the same action as tapping the revealed button, as the component expects.
        modifier = Modifier.fillMaxWidth(),
    ) {
        TrackRowContent(
            track = track,
            artUri = artUri,
            isCurrent = isCurrent,
            isPlaying = isPlaying,
            onClick = onClick,
        )
    }
}

@Composable
private fun TrackRowContent(
    track: Track,
    artUri: Uri?,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    val tapFeedback = rememberTapFeedback()
    Card(
        onClick = {
            tapFeedback()
            onClick()
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                AlbumArtwork(
                    artworkUri = artUri,
                    contentDescription = track.title,
                    size = 42.dp,
                    cornerRadius = 10.dp,
                )
                if (isCurrent) {
                    PlayingBars(isPlaying = isPlaying)
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                MarqueeText(
                    text = track.title.ifBlank { stringResource(R.string.label_unknown_album) },
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isCurrent) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = track.artist.ifBlank { stringResource(R.string.label_unknown_artist) },
                    style = MaterialTheme.typography.bodyExtraSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = formatDuration(track.durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

/**
 * The "this is the current track" marker: three bars that rise and fall while the track is playing
 * and sit still, short, while it is paused.
 *
 * The three bars run at different periods so the marker never looks metronomic, and their heights
 * are read inside the draw block only while the track is actually playing — the same rule the wave
 * progress ring follows, for the same reason: a snapshot read in a draw block is what invalidates
 * the draw, so reading an idle animation forever would redraw the list sixty times a second for a
 * picture that never changes.
 */
@Composable
private fun PlayingBars(isPlaying: Boolean) {
    val transition = rememberInfiniteTransition(label = "playingBars")
    val bars = BarPeriods.indices.map { i ->
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = BarPeriods[i],
                    easing = FastOutSlowInEasing,
                ),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "playingBar$i",
        )
    }
    // Ramps the bars between their still height and the animated one, so pausing settles rather
    // than snaps.
    val level by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "playingBarsLevel",
    )
    val color = MaterialTheme.colorScheme.primary

    Canvas(Modifier.size(14.dp)) {
        // Read only when there is motion to show.
        val lift = if (level > 0f) bars.map { it.value } else List(BarPeriods.size) { 0f }
        val slot = size.width / (BarPeriods.size * 2f - 1f)
        BarPeriods.indices.forEach { i ->
            val height = size.height * (BarMinHeight + (1f - BarMinHeight) * lift[i] * level)
            drawRoundRect(
                color = color,
                topLeft = Offset(x = i * slot * 2f, y = (size.height - height) / 2f),
                size = Size(slot, height),
                cornerRadius = CornerRadius(slot / 2f),
            )
        }
    }
}

/** Three co-prime-ish periods, so the bars cross at irregular intervals. */
private val BarPeriods = intArrayOf(760, 520, 940)

/** How short a bar is at rest, as a fraction of the marker's height. */
private const val BarMinHeight = 0.3f
