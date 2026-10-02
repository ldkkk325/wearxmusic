package com.wearx.music.ui

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.hierarchicalFocusGroup
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.wearx.music.BuildConfig
import com.wearx.music.R
import com.wearx.music.WearXMusicApp
import com.wearx.music.data.Lyrics
import com.wearx.music.data.model.Album
import com.wearx.music.data.model.Track
import com.wearx.music.ui.components.rememberArtworkSeedColor
import com.wearx.music.ui.components.tonalSpotScheme
import com.wearx.music.ui.screens.AlbumDetailScreen
import com.wearx.music.ui.screens.LibraryScreen
import com.wearx.music.ui.screens.MoreScreen
import com.wearx.music.ui.screens.NowPlayingScreen
import com.wearx.music.ui.screens.PermissionScreen
import com.wearx.music.ui.screens.SettingsScreen
import com.wearx.music.ui.screens.VolumeScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Root of the watch UI.
 *
 * Navigation is a small explicit back stack of [Route]s rather than the Navigation library: the
 * app has a handful of destinations, and the Wear back gesture is wired straight to [BackHandler].
 * [AnimatedContent] gives each destination a slide-and-fade transition whose direction follows the
 * navigation (push slides in from the right, pop slides back out to the right).
 */
@Composable
fun WearXMusicRoot() {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as WearXMusicApp }

    val permission = rememberAudioPermissionState()
    val settings by app.settings.settings.collectAsStateWithLifecycle()

    var albums by remember { mutableStateOf(emptyList<Album>()) }
    var isLoading by remember { mutableStateOf(false) }
    var isRescanning by remember { mutableStateOf(false) }
    var reloadToken by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    // Bind to the playback service while the UI is on screen; the service keeps playing after.
    DisposableEffect(Unit) {
        app.player.connect()
        onDispose { app.player.release() }
    }

    val playback by app.player.state.collectAsStateWithLifecycle()

    LaunchedEffect(permission.granted, reloadToken) {
        if (!permission.granted) {
            albums = emptyList()
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        albums = app.library.loadLibrary()
        isLoading = false
    }

    // The back stack is saved as plain strings so the position you were at survives process death,
    // and `rememberSaveableStateHolder` preserves each destination's own saveable state (which is
    // what brings a list's scroll position back when you return to it).
    val backStack = rememberSaveable(saver = RouteStackSaver) {
        mutableStateListOf<Route>(Route.Library)
    }
    val stateHolder = rememberSaveableStateHolder()
    val current = backStack.last()

    // One queue for the whole library, in album order, so playback carries on across album
    // boundaries and a repeat mode loops everything rather than a single album.
    val libraryQueue = remember(albums) { albums.flatMap { it.tracks } }
    val tracks = remember(libraryQueue) { libraryQueue.sortedBy { it.title.lowercase() } }

    // Which way the last navigation went, so the transition plays in the matching direction.
    var navigatingForward by remember { mutableStateOf(true) }

    // Where the current push grew out of, and which route it was for. The point is recorded from the
    // pointer itself (see the observer below) rather than from each control: every navigation
    // trigger in the app is a control sitting under the finger, so one observer covers all of them —
    // and any control added later — instead of a modifier that has to be remembered at each call
    // site. On a 454 px dial the difference between the touch point and the middle of a full-width
    // row is a few percent of the screen.
    //
    // Both are stored as FRACTIONS, not pixels: `AnimatedContent.scaleIn` takes a `TransformOrigin`
    // (fractions of the content's own size) and an initial scale as a fraction, and the observer has
    // the container's size to hand, so converting here keeps the conversion out of the transition.
    var lastPress by remember { mutableStateOf<Offset?>(null) }
    var pressStartScale by remember { mutableStateOf(1f) }
    var pushOrigin by remember { mutableStateOf<Offset?>(null) }
    var pushStartScale by remember { mutableStateOf(1f) }
    var pushedRoute by remember { mutableStateOf<Route?>(null) }

    // The origin is only meaningful while the push that used it is still running; leaving it set
    // would make a later pop grow out of a stale point.
    LaunchedEffect(pushedRoute) {
        if (pushedRoute != null) {
            delay(RouteTransitionMs.toLong())
            pushedRoute = null
            pushOrigin = null
        }
    }

    val currentTrack = remember(albums, playback.mediaId) {
        val id = playback.mediaId?.toLongOrNull()
        albums.firstNotNullOfOrNull { album -> album.tracks.firstOrNull { it.id == id } }
    }

    var lyrics by remember { mutableStateOf<Lyrics>(Lyrics.None) }
    var lyricsLoading by remember { mutableStateOf(false) }
    LaunchedEffect(currentTrack?.id) {
        if (currentTrack == null) {
            lyrics = Lyrics.None
            lyricsLoading = false
        } else {
            lyricsLoading = true
            lyrics = app.lyrics.load(currentTrack)
            lyricsLoading = false
        }
    }

    fun navigate(route: Route) {
        navigatingForward = true
        pushOrigin = lastPress
        pushStartScale = pressStartScale
        pushedRoute = route
        backStack.add(route)
    }

    fun goBack() {
        navigatingForward = false
        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
    }

    fun play(track: Track) {
        val index = libraryQueue.indexOfFirst { it.id == track.id }
        if (index < 0) return
        app.player.playQueue(libraryQueue, index)
        navigate(Route.NowPlaying)
    }

    fun playFromAlbum(album: Album) {
        album.tracks.firstOrNull()?.let(::play)
    }

    val previousRoute = if (backStack.size >= 2) backStack[backStack.size - 2] else null

    // "Reduce motion" has to cover the page transitions too, or the setting only half works: it
    // reaches the player's flip and the wave ring, and nothing else. Every transition below reads
    // this, and with it on they land on their final state instead of travelling to it. The morph is
    // already initialised to 1f in that case, so the page simply appears.
    val reduceMotion = settings.reduceMotion

    // Predictive back, wired into the route transition itself. This is the structure the platform's
    // own Wear NavHost uses (`PredictiveBackNavHost` in androidx.wear.compose:compose-navigation):
    // the transition is a *seekable* object, and the back gesture seeks it. That is the whole point
    // — a hand-rolled offset that the gesture drags around plus a separate route transition that
    // then takes over are two animations that can never agree, which is why the old shape read as
    // "the back animation and the predictive back animation don't match". Here there is exactly one.
    val routeTransitionState = remember { SeekableTransitionState(current) }
    val routeTransition = rememberTransition(routeTransitionState, label = "route")

    // Stacking order per route, raised by each push and lowered by each pop. Not snapshot state: it
    // is read and written inside `transitionSpec`, which must not trigger a recomposition.
    val routeZIndices = remember { mutableMapOf<String, Float>() }

    var backProgress by remember { mutableFloatStateOf(0f) }
    var inPredictiveBack by remember { mutableStateOf(false) }
    val backScope = rememberCoroutineScope()

    PredictiveBackHandler(enabled = previousRoute != null) { events ->
        val previous = previousRoute ?: return@PredictiveBackHandler
        var committed = false
        try {
            events.collect { event ->
                inPredictiveBack = true
                backProgress = event.progress
                // Seek, never animate: the transition has to sit exactly where the finger is. With
                // motion reduced there is nothing to seek — the whole pop is decided at once, so the
                // gesture only decides *whether* to go back, never where the page is.
                if (reduceMotion) {
                    routeTransitionState.snapTo(previous)
                } else {
                    routeTransitionState.seekTo(event.progress, previous)
                }
            }
            committed = true
        } finally {
            inPredictiveBack = false
            if (committed) {
                // Popping is all that is needed. The transition already targets the previous route,
                // so the navigation path below carries it the rest of the way from wherever the
                // finger left it — one continuous motion, with no restart and no snap back.
                goBack()
            } else if (!reduceMotion) {
                // The gesture was cancelled. This coroutine is cancelled too, so the glide back has
                // to run in the screen's scope; it drives the same transition object, so nothing can
                // disagree with it mid-flight.
                backScope.launch {
                    val travelled = routeTransitionState.fraction.coerceIn(0f, 1f)
                    animate(routeTransitionState.fraction, 0f, animationSpec = settleSpec(travelled)) { value, _ ->
                        // `animate`'s block is not a suspending context, so each seek is dispatched.
                        backScope.launch {
                            if (value > 0f) routeTransitionState.seekTo(value)
                            if (value == 0f) routeTransitionState.snapTo(current)
                        }
                    }
                }
            }
        }
    }

    // Any navigation that did not come from the gesture — taps, and the pop the gesture committed —
    // finishes the same transition object, so the seeked position is carried into the animation.
    LaunchedEffect(current) {
        if (routeTransitionState.currentState != current) {
            when {
                reduceMotion -> routeTransitionState.snapTo(current)

                // A push is not finishing a hand-off. The page is growing out of the pressed point on
                // its own clock, so the play head has to stay long enough to carry the outgoing page's
                // exit along with it; shortening this would cut the growth off mid-way and leave the
                // circle expanding over nothing.
                navigatingForward -> routeTransitionState.animateTo(current, RouteSettleSpec)

                // A committed pop has only the tail left, and the finger is already off the glass —
                // this is the half-second of "the page is still creeping" that reads as slow. Time it
                // to the distance that is actually left, with a floor so a late release cannot turn
                // the hand-off into a jump.
                else -> {
                    val remaining = (1f - routeTransitionState.fraction).coerceIn(0f, 1f)
                    routeTransitionState.animateTo(current, settleSpec(remaining))
                }
            }
        }
    }

    // Global UI scale: overriding LocalDensity rescales every dp and sp in the tree at once.
    val density = LocalDensity.current
    val scaledDensity = remember(density, settings.uiScale) {
        Density(
            density = density.density * settings.uiScale,
            fontScale = density.fontScale * settings.uiScale,
        )
    }

    // Dynamic colour: pull one seed out of the current cover and rebuild the whole scheme with
    // Material 3's Tonal Spot variant, so every role—not just the button containers—is derived and
    // stays in harmony. With no artwork (or the setting off) the theme is left untouched.
    val baseScheme = MaterialTheme.colorScheme
    val seed = rememberArtworkSeedColor(playback.artworkUri.takeIf { settings.dynamicColor })

    // The seed is animated and the whole scheme is regenerated from the MOVING seed, rather than
    // tweening 29 colour roles one by one: `tonalSpotScheme` is a pure function of the seed, so one
    // tween drives every role together and they cannot drift out of step. Skipping a track used to
    // snap all twenty-nine at once, which is far more jarring than any one of them changing.
    //
    // The clock is deliberately short. An animated scheme invalidates every reader of
    // `MaterialTheme.colorScheme`, so each frame of this transition recomposes the whole visible
    // screen — the same machinery that has caused the stutter elsewhere. Keep it under a third of a
    // second and it reads as a shift rather than a fade; lengthen it and it becomes a frame sink.
    val animatedSeed by animateColorAsState(
        targetValue = seed ?: baseScheme.primary,
        animationSpec = tween(ThemeShiftMs, easing = RouteEasing),
        label = "artworkSeed",
    )
    val colorScheme = remember(baseScheme, animatedSeed) {
        if (seed == null) baseScheme else tonalSpotScheme(baseScheme, animatedSeed)
    }

    MaterialTheme(colorScheme = colorScheme) {
        CompositionLocalProvider(LocalDensity provides scaledDensity) {
        AppScaffold {
            // The route bodies on their own, so they can be rendered either as the live destination
            // (with its saved state) or as the screen peeking out from behind it.
            val routeContent: @Composable (Route) -> Unit = { route ->
                when (route) {
                        Route.Library -> when {
                            !permission.granted -> PermissionScreen(
                                permanentlyDenied = permission.permanentlyDenied,
                                onRequestPermission = permission.request,
                            )

                            isLoading -> LoadingIndicator()

                            else -> LibraryScreen(
                                albums = albums,
                                tracks = tracks,
                                playback = playback,
                                isRescanning = isRescanning,
                                onAlbumClick = { album -> navigate(Route.AlbumDetail(album.id)) },
                                onTrackClick = ::play,
                                onOpenVolume = { navigate(Route.Volume) },
                                onOpenSettings = { navigate(Route.Settings) },
                                onTogglePlay = app.player::playPause,
                                onOpenNowPlaying = { navigate(Route.NowPlaying) },
                                onRescan = {
                                    if (!isRescanning) {
                                        isRescanning = true
                                        scope.launch {
                                            app.library.requestRescan()
                                            reloadToken++
                                            isRescanning = false
                                        }
                                    }
                                },
                            )
                        }

                        is Route.AlbumDetail -> {
                            val album = albums.firstOrNull { it.id == route.albumId }
                            if (album == null) {
                                LaunchedEffect(route.albumId) { goBack() }
                            } else {
                                AlbumDetailScreen(
                                    album = album,
                                    nowPlayingMediaId = playback.mediaId,
                                    onPlayAll = { playFromAlbum(album) },
                                    onTrackClick = ::play,
                                )
                            }
                        }

                        Route.NowPlaying -> NowPlayingScreen(
                            state = playback,
                            lyrics = lyrics,
                            lyricsLoading = lyricsLoading,
                            reduceMotion = settings.reduceMotion,
                            onPlayPause = app.player::playPause,
                            onNext = app.player::next,
                            onPrevious = app.player::previous,
                            onOpenMore = { navigate(Route.More) },
                            onOpenVolume = { navigate(Route.Volume) },
                            onOpenSettings = { navigate(Route.Settings) },
                        )

                        Route.More -> MoreScreen(
                            state = playback,
                            onSeekBy = app.player::seekBy,
                            onCyclePlaybackMode = app.player::cyclePlaybackMode,
                        )

                        Route.Volume -> VolumeScreen()

                        Route.Settings -> SettingsScreen(
                            settings = settings,
                            versionName = BuildConfig.VERSION_NAME,
                            onReduceMotionChange = app.settings::setReduceMotion,
                            onUiScaleChange = app.settings::setUiScale,
                            onDynamicColorChange = app.settings::setDynamicColor,
                        )
                }
            }

            val destination: @Composable (Route) -> Unit = { route ->
                // Rotary (bezel) input is delivered to the *focused* element, and Wear's
                // `requestFocusOnHierarchyActive` only fires inside an ACTIVE `hierarchicalFocusGroup`
                // ancestor. AppScaffold and ScreenScaffold do not provide one (only SwipeToDismissBox
                // does), so without this box nothing in the app would answer the bezel at all.
                // `active` follows the current destination so the outgoing screen of a transition
                // does not compete for focus.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .hierarchicalFocusGroup(active = route == current),
                ) {
                    stateHolder.SaveableStateProvider(route.encode()) { routeContent(route) }
                }
            }

            // A full-dial opaque backdrop UNDER the transition, so no point of the screen is ever
            // see-through. Both pages are clipped to a circle and each one is scaled and moved
            // during a transition, which leaves gaps they do not cover — and because the incoming
            // page also starts part-transparent, those gaps used to show the window itself. That is
            // the "background goes transparent where the two overlap" glitch, and it showed up most
            // on the first gesture because that is when the previous page is composed and measured
            // for the first time, so a frame or two can pass before it covers anything. With this
            // underneath, the worst case is that a gap shows the page background instead of the
            // window, which cannot be perceived as a hole.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // Records where the finger went down, without consuming anything, so taps,
                    // drags and bezel focus all behave exactly as before.
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            // `size` here is the whole dial, so the press can be kept as a fraction
                            // and paired with the scale of the circle it is about to grow into.
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            if (w > 0f && h > 0f) {
                                pressStartScale = OriginDiameter.toPx() / w
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    if (event.type == PointerEventType.Press) {
                                        event.changes.firstOrNull()?.let {
                                            lastPress = Offset(it.position.x / w, it.position.y / h)
                                        }
                                    }
                                }
                            }
                        }
                    },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background),
                )

                routeTransition.AnimatedContent(
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        // `inPredictiveBack` selects the pop spec as well as `navigatingForward`: the
                        // gesture retargets the transition before the stack is actually popped, so at
                        // that moment the flag is still true from the push that got us here.
                        val pop = !navigatingForward || inPredictiveBack
                        // Z-indices rise with each push and fall with each pop, exactly as the
                        // platform NavHost does it. Two constants would TIE whenever two pushes
                        // happen in a row, and a tie hands the stacking order to composition order —
                        // which is how the page being dismissed can end up drawn under the one being
                        // revealed, leaving the semi-transparent incoming page composited against
                        // nothing.
                        val targetKey = targetState.encode()
                        val initialKey = initialState.encode()
                        val initialZ = routeZIndices.getOrPut(initialKey) { 0f }
                        val targetZ =
                            if (targetKey == initialKey) {
                                initialZ
                            } else {
                                (if (pop) initialZ - 1f else initialZ + 1f).also {
                                    routeZIndices[targetKey] = it
                                }
                            }
                        ContentTransform(
                            // A push grows the incoming page out of the pressed point — the container
                            // transform — and the TRANSITION owns that scale now, rather than a
                            // `graphicsLayer` applied to the live page below.
                            //
                            // `transformOrigin` is what keeps this a container transform instead of
                            // just a zoom: without it `scaleIn` grows the page out of its own centre
                            // and the pressed point stops mattering. With motion reduced every part
                            // of the transform collapses to "already there", so the outgoing page
                            // neither swells nor fades on its way out either.
                            targetContentEnter =
                                when {
                                    reduceMotion -> EnterTransition.None
                                    pop -> RoutePopEnter
                                    else -> scaleIn(
                                        animationSpec = tween(
                                            durationMillis = RouteTransitionMs,
                                            easing = MorphEasing,
                                        ),
                                        initialScale = pushStartScale,
                                        transformOrigin = TransformOrigin(
                                            pushOrigin?.x ?: 0.5f,
                                            pushOrigin?.y ?: 0.5f,
                                        ),
                                    )
                                },
                            initialContentExit =
                                if (reduceMotion) ExitTransition.None
                                else if (pop) RoutePopExit
                                else RouteExit,
                            targetContentZIndex = targetZ,
                            sizeTransform = null,
                        )
                    },
                    contentAlignment = Alignment.Center,
                    contentKey = { it.encode() },
                ) { route ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            // Clip only. The scale that used to live here as a `graphicsLayer` is now
                            // applied by `AnimatedContent` around this content, which keeps the order
                            // this project relies on: the clip is the inner layer and the scale the
                            // outer one, so what gets scaled is already a circle.
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.background)
                            // Drawn rather than recomposed, and as a circle to match the dial. The
                            // two scrim alphas are worked out inside the draw block on purpose:
                            // reading the gesture's progress here is a deferred read, so a moving
                            // finger invalidates a draw instead of recomposing the whole navigation
                            // tree once per frame.
                            .drawWithContent {
                                drawContent()
                                val alpha =
                                    if (inPredictiveBack) {
                                        // Wear's own swipe-to-dismiss values: the page being
                                        // dismissed darkens progressively, and the page being
                                        // revealed starts half dark and brightens as the gesture
                                        // commits, so the screen being returned to reads as "behind"
                                        // until it is the one you are on.
                                        if (route == current) {
                                            (backProgress / 2f).coerceAtMost(DismissScrimAlpha)
                                        } else {
                                            RevealScrimAlpha * (1f - backProgress)
                                        }
                                    } else {
                                        0f
                                    }
                                if (alpha > 0f) drawCircle(Color.Black, alpha = alpha)
                            },
                    ) {
                        destination(route)
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.loading),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The curve the page-opening morph runs on: **easeOutQuint**, `cubic-bezier(0.23, 1, 0.32, 1)`.
 *
 * It leaves the gate at roughly four times its average speed and decelerates smoothly to a stop —
 * 49 % of the distance by 13 % of the duration, 78 % by 26 %, 94 % by 42 %. That steep opening is
 * the point: an earlier curve here was `(0.2, 0, 0, 1)`, whose initial tangent is horizontal, so it
 * sat still for the first frames and then lurched, which reads as jank even with no dropped frames.
 * This one answers the tap immediately and then settles.
 *
 * The trade is the tail: the last third of the duration carries the final 1 %, so if the settle
 * feels like a hover, shorten [RouteTransitionMs] rather than reaching for another curve.
 */
private val RouteEasing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
/**
 * How long a page transition runs. Slowed from 420 ms to 560 ms at the user's request: with the
 * scale now driven by the transition itself, a longer clock is what turns the growth from "quick"
 * into "unhurried", and the overshoot at the end gets more time to read as a settle rather than a
 * flick. It drives the push's `scaleIn` tween, the play head and the pushed-origin cleanup together,
 * so they cannot drift apart.
 */
private const val RouteTransitionMs = 560

/** How long the palette takes to shift to a new track's cover. See the note where it is used. */
private const val ThemeShiftMs = 320

/**
 * The back gesture's curve. Linear, which is what Wear's own `PredictiveBackNavHost` uses, and the
 * reason is structural rather than a matter of taste: the transition is seeked, so the gesture's
 * progress *is* the animation's time and the curve is literally the finger mapping. Any curve that
 * starts slowly is felt as a dead patch — easeInCubic leaves the page at 1.6 % of its travel after
 * a quarter of the drag and at 42 % after three quarters, which reads as "the page has stopped
 * responding" and then a rush at the end. Linear keeps the page exactly where the finger is.
 *
 * The push morph keeps [RouteEasing] (easeOutQuint): nothing drives it, so there is no finger to
 * fall behind, and an immediate response is what makes it feel alive. Same screen, two different
 * rules, because one is gesture-driven and the other is not.
 */
private val BackEasing = LinearEasing

/**
 * The push's own curve: easeOutQuint plus a small ring, which is what a spring was doing and what
 * `scaleIn` can no longer do.
 *
 * `AnimatedContent` samples an enter transition at the transition's FRACTION, so a `spring` spec
 * inside `scaleIn` is evaluated as a pure function of that fraction — which means it loses the
 * `initialVelocity` that gave the spring its immediate start, and a spring with no initial velocity
 * eases in from rest (the exact dead start this curve exists to avoid). The `easeOutBack` family
 * does not help: it couples the initial slope to the overshoot, and matching easeOutQuint's slope of
 * 5 forces 10.8 % of overshoot. So the two properties are separated instead — easeOutQuint for the
 * approach, plus a ring term that is zero (with zero derivative) at both ends, so the initial slope
 * stays 5 and the peak is a single, chosen amount:
 *
 * | time | value |
 * |---|---|
 * | 50 ms | 0.470 (easeOutQuint alone: 0.469) |
 * | 100 ms | 0.749 (easeOutQuint alone: 0.743) |
 * | peak | 1.011 at 63 % — about **0.9 % past full size** |
 * | 420 ms | 1.000 |
 *
 * One smooth function, so there is no seam: the rebound is produced by the same motion that reaches
 * the finish, and the curve is monotone from the peak onwards.
 */
private val MorphEasing = Easing { t ->
    val quint = 1f - (1f - t) * (1f - t) * (1f - t) * (1f - t) * (1f - t)
    val ring = sin(PI.toFloat() * t).let { it * it * it * it }
    quint + MorphRingAmount * ring
}

/** How much the push overshoots past its finished size. Larger gives a more visible rebound. */
private const val MorphRingAmount = 0.025f

/**
 * The circle a pushed page grows out of. The real origin is the pressed point; this is how big that
 * circle starts, and it is the height of the rows and buttons that trigger navigation on Wear, so
 * the page reads as growing out of the control rather than out of a dot.
 */
private val OriginDiameter = 40.dp

/**
 * The page being replaced during a push: it swells a little and fades, so the new page reads as
 * coming forward over it. This is the exit half of Material's container transform — no travel,
 * because travel would compete with the growing circle.
 */
private val RouteExit =
    scaleOut(targetScale = 1.05f, animationSpec = tween(RouteTransitionMs, easing = RouteEasing)) +
        fadeOut(animationSpec = tween(RouteTransitionMs, easing = RouteEasing))

/**
 * Back: a full width out to the right while shrinking, with the page being returned to coming in
 * from half a width away — so the page being dismissed has left the dial by the time the transition
 * completes and needs no fade to hide its removal. The distances are Wear's own; the curve is linear
 * for the reason on [BackEasing], and the duration is ours (Wear's `tween` default of 300 ms only
 * covers the commit hand-off, which is over well before this finishes).
 */
private val RoutePopEnter =
    scaleIn(initialScale = 0.8f, animationSpec = tween(RouteTransitionMs, easing = BackEasing)) +
        slideInHorizontally(
            initialOffsetX = { -it / 2 },
            animationSpec = tween(RouteTransitionMs, easing = BackEasing),
        ) +
        fadeIn(initialAlpha = 0.5f, animationSpec = tween(RouteTransitionMs, easing = BackEasing))
private val RoutePopExit =
    slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = tween(RouteTransitionMs, easing = BackEasing),
    ) +
        scaleOut(
            targetScale = 0.8f,
            animationSpec = tween(RouteTransitionMs, easing = BackEasing),
        )

/**
 * The play head for a push: the transition's full length, linear, because the page is growing out
 * of the pressed point on its own clock and the head has to stay long enough to carry the outgoing
 * page's exit with it.
 */
private val RouteSettleSpec = tween<Float>(durationMillis = RouteTransitionMs, easing = LinearEasing)

/**
 * The play head for a pop that is finishing — a committed gesture, or an abandoned one gliding back.
 *
 * A fixed duration here is what made the back feel slow no matter which curve was on it: the
 * transition was seeked to wherever the finger let go, and then the head walked the *whole* length
 * regardless. Releasing at 50 % therefore cost the same 420 ms as releasing at 99 %, and the last
 * stretch of the page's exit crawled along at a quarter of its full speed.
 *
 * So the time is proportional to the distance that is left, and — because the pop's own animations
 * are linear — the head's curve IS the curve the page ends up drawing. [RouteEasing] therefore
 * gives an immediate, decisive exit that settles rather than creeping, which is what "fast" has to
 * mean once the finger is no longer on the glass.
 */
private fun settleSpec(remaining: Float) = tween<Float>(
    durationMillis = (SettleCeilingMs * remaining.coerceIn(0f, 1f)).toInt()
        .coerceAtLeast(SettleFloorMs),
    easing = RouteEasing,
)

/** The longest a pop's hand-off may take, when the whole transition is still ahead of it. */
private const val SettleCeilingMs = 260

/** The shortest, so releasing at the very end cannot turn the hand-off into a jump. */
private const val SettleFloorMs = 90

/** How dark the page being dismissed gets. */
private const val DismissScrimAlpha = 0.3f

/** How dark the page being revealed starts out. */
private const val RevealScrimAlpha = 0.5f

/**
 * Persists the navigation back stack as a list of plain strings, so being deep in the app survives
 * both a configuration change and process death.
 */
private val RouteStackSaver = listSaver<MutableList<Route>, String>(
    save = { stack -> stack.map { route -> route.encode() } },
    restore = { keys ->
        mutableStateListOf<Route>().apply {
            keys.forEach { key -> add(Route.decode(key)) }
        }
    },
)
