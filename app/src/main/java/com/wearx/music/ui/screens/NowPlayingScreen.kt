package com.wearx.music.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.HorizontalPageIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.LocalRippleConfiguration
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.media.ui.material3.components.animated.AnimatedPlayPauseProgressButton
import com.google.android.horologist.media.ui.state.model.TrackPositionUiModel
import com.wearx.music.R
import com.wearx.music.data.Lyrics
import com.wearx.music.playback.PlaybackInterruption
import com.wearx.music.playback.PlayerPosition
import com.wearx.music.playback.PlayerUiState
import com.wearx.music.ui.components.ArtworkBackdrop
import com.wearx.music.ui.components.MarqueeText
import com.wearx.music.ui.components.NavigationRow
import com.wearx.music.ui.components.rememberPressShape
import com.wearx.music.ui.components.rememberTapFeedback
import com.wearx.music.util.formatDuration
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private const val FlipHalfTurn = 90f

private val PlayRingSize = 72.dp
private val SideButtonSize = 52.dp
private val SideIconSize = 24.dp
private val PlayButtonSize = 64.dp

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
    position: PlayerPosition,
    lyrics: Lyrics,
    lyricsLoading: Boolean,
    reduceMotion: Boolean,
    lyricsBlurRadius: Float,
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
                    position = position,
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
                    positionMs = position.positionMs,
                    trackTitle = state.title,
                    blurRadius = lyricsBlurRadius,
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
    position: PlayerPosition,
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
                        formatDuration(position.positionMs),
                        formatDuration(position.durationMs),
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

            TransportRow(state, position, onPrevious, onPlayPause, onNext)

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
 * Transport row: Horologist's animated middle button (play/pause morph over the rotating wavy
 * progress indicator) flanked by the app's own press-morph seek buttons.
 *
 * The seek buttons use the same [rememberPressShape] morph as every other button in the app —
 * pressing a side key deforms THAT key. Horologist's Lottie seek buttons instead bulge the
 * middle scallop on any press, which reads as the wrong button reacting. Explicit sizes keep
 * the side buttons from being squeezed away (ButtonGroupLayout could do exactly that), and the
 * colours stay mapped onto the app's tonal roles.
 */
@OptIn(ExperimentalHorologistApi::class)
@Composable
private fun TransportRow(
    state: PlayerUiState,
    position: PlayerPosition,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
) {
    val trackPosition = TrackPositionUiModel.Actual(
        percent = position.progressFraction,
        duration = position.durationMs.milliseconds,
        position = position.positionMs.milliseconds,
    )

    // The middle scallop only reacts to its OWN button now — the side keys have their own morph.
    val playSource = remember { MutableInteractionSource() }
    val playPressed by playSource.collectIsPressedAsState()
    val playOwnPressed = remember { derivedStateOf { playPressed } }
    val tapFeedback = rememberTapFeedback()

    // Horologist derives the middle button's face from the scheme's primary, which reads as alien
    // next to this app's controls. Nudging the scheme just for that call moves it onto
    // primaryContainer — the same family as MorphButton — while the rotating indicator keeps
    // reading onSecondaryContainer / surfaceContainerLow, which are untouched.
    val scheme = MaterialTheme.colorScheme
    val playScheme = scheme.copy(
        primary = scheme.primaryContainer,
        onPrimary = scheme.onPrimaryContainer,
    )

    // The buttons' own morph animation is the feedback; the stock ripple fought it.
    CompositionLocalProvider(LocalRippleConfiguration provides null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SeekButton(
                icon = Icons.Filled.SkipPrevious,
                contentDescription = stringResource(R.string.action_previous),
                enabled = state.hasPrevious,
                onClick = onPrevious,
                modifier = Modifier.size(SideButtonSize),
            )

            MaterialTheme(colorScheme = playScheme) {
                AnimatedPlayPauseProgressButton(
                    onPlayClick = {
                        tapFeedback()
                        onPlayPause()
                    },
                    onPauseClick = {
                        tapFeedback()
                        onPlayPause()
                    },
                    playing = state.isPlaying,
                    trackPositionUiModel = trackPosition,
                    modifier = Modifier.size(PlayRingSize),
                    interactionSource = playSource,
                    isAnyButtonPressed = playOwnPressed,
                )
            }

            SeekButton(
                icon = Icons.Filled.SkipNext,
                contentDescription = stringResource(R.string.action_next),
                enabled = state.hasNext,
                onClick = onNext,
                modifier = Modifier.size(SideButtonSize),
            )
        }
    }
}

/** A side seek button with the app's press morph — pill at rest, squircle while held. */
@Composable
private fun SeekButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val tapFeedback = rememberTapFeedback()
    CompactButton(
        onClick = {
            tapFeedback()
            onClick()
        },
        enabled = enabled,
        modifier = modifier,
        shape = rememberPressShape(interactionSource),
        colors = ButtonDefaults.filledTonalButtonColors(),
        interactionSource = interactionSource,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(SideIconSize),
            )
        },
    )
}
