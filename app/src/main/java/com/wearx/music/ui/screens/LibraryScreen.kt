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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.ButtonGroup
import androidx.wear.compose.material3.ButtonGroupDefaults
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.HorizontalPageIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.wearx.music.R
import com.wearx.music.data.SortMode
import com.wearx.music.data.model.Album
import com.wearx.music.data.model.Track
import com.wearx.music.playback.PlayerUiState
import com.wearx.music.ui.components.AlbumArtwork
import com.wearx.music.ui.components.MarqueeText
import com.wearx.music.ui.components.MorphButton
import com.wearx.music.ui.components.MorphGroupIconButton
import com.wearx.music.ui.components.rememberTapFeedback
import com.wearx.music.util.formatDuration

/**
 * Home screen, as a two-page pager: the library itself, and the tool page (search, volume,
 * settings) that used to sit in button rows above the list.
 *
 * The library is the page you land on; swiping right — the platform's "previous page"
 * direction, same gesture as going back — reveals the tools. Tracks are the primary list (and
 * the default tab), each row carrying its own cover. The top bar is deliberately terse: the
 * screen is only ~227 dp tall, so there is no page title and every control is icon-only. With
 * playback running, a now-playing row sits above the tabs.
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
    val pagerState = rememberPagerState(
        initialPage = LibraryPageIndex,
        pageCount = { PageCount },
    )

    Box(Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            // Keep the tools page composed so the first swipe is not a rebuild. No rotary page
            // turning on purpose: the crown belongs to whatever list is on screen — the library
            // here, the lyrics on the player page — not to flipping between these two pages.
            beyondViewportPageCount = 1,
        ) { page ->
            if (page == ToolsPageIndex) {
                ToolsPage(
                    onOpenSearch = onOpenSearch,
                    onOpenVolume = onOpenVolume,
                    onOpenSettings = onOpenSettings,
                )
            } else {
                LibraryPageContent(
                    albums = albums,
                    tracks = tracks,
                    playback = playback,
                    artByTrackId = artByTrackId,
                    isRescanning = isRescanning,
                    onAlbumClick = onAlbumClick,
                    onTrackClick = onTrackClick,
                    onPlayNext = onPlayNext,
                    sortMode = sortMode,
                    onCycleSort = onCycleSort,
                    onTogglePlay = onTogglePlay,
                    onOpenNowPlaying = onOpenNowPlaying,
                    onRescan = onRescan,
                )
            }
        }

        HorizontalPageIndicator(
            pagerState = pagerState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp),
        )
    }
}

private const val ToolsPageIndex = 0
private const val LibraryPageIndex = 1
private const val PageCount = 2

@Composable
private fun LibraryPageContent(
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
    onTogglePlay: () -> Unit,
    onOpenNowPlaying: () -> Unit,
    onRescan: () -> Unit,
) {
    val scrollState = rememberTransformingLazyColumnState()
    // The settings page's default spec, not a custom wider band: the library's edge items used
    // to start scaling and fading much further from the centre than every other list in the app.
    val transformationSpec = rememberTransformationSpec()
    var showTracks by rememberSaveable { mutableStateOf(true) }

    ScreenScaffold(scrollState = scrollState) { padding ->
        TransformingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = scrollState,
            verticalArrangement = Arrangement.spacedBy(3.dp),
            contentPadding = padding,
        ) {
            if (playback.hasMedia) {
                item {
                    NowPlayingBar(
                        playback = playback,
                        onOpen = onOpenNowPlaying,
                        onTogglePlay = onTogglePlay,
                        transformation = SurfaceTransformation(transformationSpec),
                        modifier = Modifier.transformedHeight(this, transformationSpec),
                    )
                }
            }

            // Two rows, because these are two different kinds of control squeezed into one. Five
            // across a 227 dp dial is ~42 dp per button, under the 48 dp Wear asks for, and the row
            // mixed a view-mode toggle (one of which is ALWAYS active) with three plain navigation
            // buttons (none of which ever is). The navigation trio now lives on the tools page
            // (swipe right); this row is the view controls alone.
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

            if (showTracks) {
                itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
                    TrackRow(
                        track = track,
                        artUri = track.artUri ?: artByTrackId[track.id],
                        isCurrent = track.id.toString() == playback.mediaId,
                        isPlaying = playback.isPlaying,
                        onClick = { onTrackClick(track) },
                        onPlayNext = { onPlayNext(track) },
                        transformation = SurfaceTransformation(transformationSpec),
                        modifier = Modifier.transformedHeight(this, transformationSpec),
                    )
                }
            } else {
                itemsIndexed(albums, key = { _, album -> album.id }) { index, album ->
                    AlbumRow(
                        album = album,
                        onClick = { onAlbumClick(album) },
                        transformation = SurfaceTransformation(transformationSpec),
                        modifier = Modifier.transformedHeight(this, transformationSpec),
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
                        modifier = Modifier.transformedHeight(this, transformationSpec).fillMaxWidth(),
                        enabled = !isRescanning,
                        transformation = SurfaceTransformation(transformationSpec),
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

/**
 * The tools page: search, volume and settings, as the same action rows the settings index uses.
 * Home stays a pure library; this is the page the right-swipe reveals.
 */
@Composable
private fun ToolsPage(
    onOpenSearch: () -> Unit,
    onOpenVolume: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val scrollState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(scrollState = scrollState) { padding ->
        TransformingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = scrollState,
            verticalArrangement = Arrangement.spacedBy(3.dp),
            contentPadding = padding,
        ) {
            item {
                ListHeader {
                    Text(
                        text = stringResource(R.string.tools_title),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            item {
                ToolRow(
                    icon = Icons.Filled.Search,
                    label = stringResource(R.string.nav_search),
                    onClick = onOpenSearch,
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier.transformedHeight(this, transformationSpec),
                )
            }
            item {
                ToolRow(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    label = stringResource(R.string.nav_volume),
                    onClick = onOpenVolume,
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier.transformedHeight(this, transformationSpec),
                )
            }
            item {
                ToolRow(
                    icon = Icons.Filled.Settings,
                    label = stringResource(R.string.nav_settings),
                    onClick = onOpenSettings,
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier.transformedHeight(this, transformationSpec),
                )
            }
            item { Spacer(Modifier.height(18.dp)) }
        }
    }
}

/** One action row: icon, label, chevron — the settings index's row shape. */
@Composable
private fun ToolRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    transformation: SurfaceTransformation,
    modifier: Modifier = Modifier,
) {
    val tapFeedback = rememberTapFeedback()
    Card(
        onClick = {
            tapFeedback()
            onClick()
        },
        modifier = modifier.fillMaxWidth(),
        transformation = transformation,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Compact now-playing row: tapping the row opens the player, the button toggles playback. */
@Composable
private fun NowPlayingBar(
    playback: PlayerUiState,
    onOpen: () -> Unit,
    onTogglePlay: () -> Unit,
    transformation: SurfaceTransformation,
    modifier: Modifier = Modifier,
) {
    val tapFeedback = rememberTapFeedback()
    Card(
        onClick = {
            tapFeedback()
            onOpen()
        },
        modifier = modifier.fillMaxWidth(),
        transformation = transformation,
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
private fun AlbumRow(
    album: Album,
    onClick: () -> Unit,
    transformation: SurfaceTransformation,
    modifier: Modifier = Modifier,
) {
    val tapFeedback = rememberTapFeedback()
    Card(
        onClick = {
            tapFeedback()
            onClick()
        },
        modifier = modifier.fillMaxWidth(),
        transformation = transformation,
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
 * A library row: tap plays, long-press queues it as the next track.
 *
 * There is no SwipeToReveal here any more: its horizontal drag consumed every swipe that
 * started on a row, so the home pager could not turn pages from the list at all. Long-press
 * needs no horizontal gesture, which is exactly what frees the swipe for the pager.
 */
@Composable
private fun TrackRow(
    track: Track,
    artUri: Uri?,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onPlayNext: () -> Unit,
    transformation: SurfaceTransformation,
    modifier: Modifier = Modifier,
) {
    TrackRowContent(
        track = track,
        artUri = artUri,
        isCurrent = isCurrent,
        isPlaying = isPlaying,
        onClick = onClick,
        onPlayNext = onPlayNext,
        transformation = transformation,
        modifier = modifier,
    )
}

@Composable
private fun TrackRowContent(
    track: Track,
    artUri: Uri?,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onPlayNext: () -> Unit,
    transformation: SurfaceTransformation,
    modifier: Modifier = Modifier,
) {
    val tapFeedback = rememberTapFeedback()
    Card(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(track.id) {
                detectTapGestures(
                    onTap = {
                        tapFeedback()
                        onClick()
                    },
                    onLongPress = {
                        tapFeedback()
                        onPlayNext()
                    },
                )
            },
        transformation = transformation,
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
