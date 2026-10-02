package com.wearx.music.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.HorizontalPageIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.wearx.music.R
import com.wearx.music.data.Lyrics
import com.wearx.music.playback.PlaybackInterruption
import com.wearx.music.playback.PlayerUiState
import com.wearx.music.ui.components.ArtworkBackdrop
import com.wearx.music.ui.components.MarqueeText
import com.wearx.music.ui.components.NavigationRow
import com.wearx.music.ui.components.WaveProgressRing
import com.wearx.music.ui.components.rememberBurstShape
import com.wearx.music.ui.components.rememberPressScale
import com.wearx.music.ui.components.rememberTapFeedback
import com.wearx.music.util.formatDuration
import kotlinx.coroutines.launch

private const val FlipHalfTurn = 90f
private val PlayRingSize = 72.dp
private val PlayButtonSize = 52.dp

private const val PlayerPageIndex = 0
private const val PageCount = 2

/**
 * The player, as a two-page horizontal pager: the controls, then the lyrics.
 *
 * Putting the lyrics on the next page (rather than on their own destination) is what makes the
 * swipe continuous — the pager tracks the finger and settles, instead of a swipe being detected and
 * then triggering a discrete screen change. Both pages share one backdrop, so the image never
 * visibly swaps mid-gesture.
 */
@Composable
fun NowPlayingScreen(
    state: PlayerUiState,
    lyrics: Lyrics,
    lyricsLoading: Boolean,
    reduceMotion: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onOpenMore: () -> Unit,
    onOpenVolume: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { PageCount })
    val scope = rememberCoroutineScope()

    // Back from the lyrics returns to the player before leaving the screen.
    BackHandler(enabled = pagerState.currentPage != PlayerPageIndex) {
        scope.launch { pagerState.animateScrollToPage(PlayerPageIndex) }
    }

    Box(Modifier.fillMaxSize()) {
        ArtworkBackdrop(artworkUri = state.artworkUri)

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            // Keep the neighbouring page composed instead of disposing it as soon as it leaves the
            // viewport. At the default of 0, swiping to the lyrics tears the player page down and
            // swiping back rebuilds it from scratch — and after that round trip, scrolling the
            // player page leaves every control on it unresponsive. Rebuilding the page is the only
            // structural thing that distinguishes it from every other screen (the volume page has the
            // same scaffold-and-scrollable layout and does not misbehave), so keeping the page alive
            // removes the round trip entirely. It also means the lyrics are already composed and
            // measured when you swipe to them.
            beyondViewportPageCount = 1,
            // Wear's pager wires up the focus request itself once given a behavior, and the
            // pager-aware overload supplies the per-page snap provider — so the bezel turns pages
            // one at a time, with the detent haptics.
            rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(pagerState),
        ) { page ->
            if (page == PlayerPageIndex) {
                PlayerPage(
                    state = state,
                    reduceMotion = reduceMotion,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onOpenMore = onOpenMore,
                    onOpenVolume = onOpenVolume,
                    onOpenSettings = onOpenSettings,
                )
            } else {
                LyricsPage(
                    lyrics = lyrics,
                    isLoading = lyricsLoading,
                    positionMs = state.positionMs,
                    trackTitle = state.title,
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

@Composable
private fun PlayerPage(
    state: PlayerUiState,
    reduceMotion: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onOpenMore: () -> Unit,
    onOpenVolume: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val scaffoldState = rememberScalingLazyListState()
    val scrollState = rememberScrollState()

    // Flip flourish on track change. Reading `flip.value` inside graphicsLayer keeps the animation
    // on the draw phase instead of recomposing every frame.
    val flip = remember { Animatable(0f) }
    LaunchedEffect(state.mediaId, reduceMotion) {
        if (reduceMotion || state.mediaId == null) {
            flip.snapTo(0f)
            return@LaunchedEffect
        }
        flip.snapTo(FlipHalfTurn)
        flip.animateTo(0f, tween(durationMillis = 320, easing = LinearOutSlowInEasing))
    }

    ScreenScaffold(scrollState = scaffoldState) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        rotationY = if (reduceMotion) 0f else flip.value
                        cameraDistance = 16f * density
                    },
            ) {
                MarqueeText(
                    text = state.title ?: stringResource(R.string.now_playing_title),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = state.artist ?: stringResource(R.string.label_unknown_artist),
                    style = MaterialTheme.typography.bodyExtraSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        R.string.now_playing_progress,
                        formatDuration(state.positionMs),
                        formatDuration(state.durationMs),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )

                // Only while something actually stopped playback, and never while the music is
                // running — otherwise this would sit under every track claiming a problem.
                state.interruption?.takeIf { !state.isPlaying }?.let { reason ->
                    Text(
                        text = stringResource(
                            when (reason) {
                                PlaybackInterruption.AUDIO_BECOMING_NOISY ->
                                    R.string.playback_interrupted_noisy

                                PlaybackInterruption.AUDIO_FOCUS_LOST ->
                                    R.string.playback_interrupted_focus

                                PlaybackInterruption.ERROR -> R.string.playback_error
                            },
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }

            TransportRow(state, reduceMotion, onPrevious, onPlayPause, onNext)

            NavigationRow(
                onOpenVolume = onOpenVolume,
                onOpenSettings = onOpenSettings,
                onOpenMore = onOpenMore,
            )

            // Room for the page indicator that floats at the bottom of the pager.
            Spacer(Modifier.height(14.dp))
        }
    }
}

/**
 * Transport row: a plain [Row] rather than a [ButtonGroup], because the middle slot has to host the
 * wave progress ring *around* the play button and a ButtonGroup child cannot be oversized.
 */
@Composable
private fun TransportRow(
    state: PlayerUiState,
    reduceMotion: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val previousSource = remember { MutableInteractionSource() }
        CompactButton(
            onClick = onPrevious,
            enabled = state.hasPrevious,
            colors = ButtonDefaults.filledTonalButtonColors(),
            interactionSource = previousSource,
            icon = {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = stringResource(R.string.action_previous),
                )
            },
        )

        val playSource = remember { MutableInteractionSource() }
        val tapFeedback = rememberTapFeedback()
        // The play button's outline is owned by the burst morph, so its press feedback is a scale
        // rather than the corner morph the other buttons use.
        val burstShape = rememberBurstShape(state.isPlaying)
        val pressScale = rememberPressScale(playSource)

        Box(
            modifier = Modifier.size(PlayRingSize),
            contentAlignment = Alignment.Center,
        ) {
            WaveProgressRing(
                progress = state.progressFraction,
                waving = state.isPlaying && !reduceMotion,
                modifier = Modifier.size(PlayRingSize),
                strokeWidth = 4.dp,
                waveAmplitude = 2.dp,
                waveCount = 7,
            )
            Button(
                onClick = {
                    tapFeedback()
                    onPlayPause()
                },
                modifier = Modifier
                    .size(PlayButtonSize)
                    .graphicsLayer {
                        scaleX = pressScale
                        scaleY = pressScale
                    },
                shape = burstShape,
                colors = ButtonDefaults.buttonColors(),
                contentPadding = PaddingValues(0.dp),
                interactionSource = playSource,
                content = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AnimatedContent(
                            targetState = state.isPlaying,
                            transitionSpec = {
                                (scaleIn(initialScale = 0.5f) + fadeIn()) togetherWith
                                    (scaleOut(targetScale = 0.5f) + fadeOut())
                            },
                            label = "playPauseIcon",
                        ) { playing ->
                            Icon(
                                imageVector = if (playing) {
                                    Icons.Filled.Pause
                                } else {
                                    Icons.Filled.PlayArrow
                                },
                                contentDescription = stringResource(
                                    if (playing) R.string.action_pause else R.string.action_play,
                                ),
                            )
                        }
                    }
                },
            )
        }

        val nextSource = remember { MutableInteractionSource() }
        CompactButton(
            onClick = onNext,
            enabled = state.hasNext,
            colors = ButtonDefaults.filledTonalButtonColors(),
            interactionSource = nextSource,
            icon = {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = stringResource(R.string.action_next),
                )
            },
        )
    }
}
