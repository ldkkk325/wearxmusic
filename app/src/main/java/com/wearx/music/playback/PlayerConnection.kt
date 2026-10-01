package com.wearx.music.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.wearx.music.data.PlaybackMode
import com.wearx.music.data.albumArtUri
import com.wearx.music.data.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * UI-side handle on the playback [PlaybackService].
 *
 * A [MediaController] connects to the service across a binder, so the UI never touches the
 * [androidx.media3.exoplayer.ExoPlayer] directly. All observable playback state is funnelled into
 * a single [StateFlow] that Compose collects, and the current position is polled because Media3
 * does not push continuous position updates.
 */
class PlayerConnection(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val sessionToken =
        SessionToken(context, ComponentName(context, PlaybackService::class.java))

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var positionJob: Job? = null

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        // `onEvents` is the catch-all callback, so one override keeps the snapshot in sync.
        override fun onEvents(player: Player, events: Player.Events) = publish()
    }

    fun connect() {
        if (controllerFuture != null) return

        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                val connected = if (future.isCancelled) null else runCatching { future.get() }.getOrNull()
                if (connected == null) return@addListener
                controller = connected
                connected.addListener(listener)
                publish()
                startPositionUpdates()
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    fun release() {
        positionJob?.cancel()
        positionJob = null
        controller?.removeListener(listener)
        controller = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        _state.value = PlayerUiState()
    }

    fun playPause() {
        val player = controller ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            player.play()
        }
        publish()
    }

    /**
     * Advances to the next track, wrapping to the start of the queue.
     *
     * Deliberately NOT `seekToNextMediaItem()` alone: Media3 computes the next index with
     * `getRepeatModeForNavigation()`, which maps `REPEAT_MODE_ONE` onto `REPEAT_MODE_OFF`. In
     * single-repeat at the last item that yields `INDEX_UNSET`, so `hasNextMediaItem()` is false —
     * which is what used to disable this button and make "next" dead in loop mode.
     */
    fun next() {
        val player = controller ?: return
        val count = player.mediaItemCount
        if (count > 1) {
            player.seekToDefaultPosition((player.currentMediaItemIndex + 1) % count)
        } else {
            player.seekToNextMediaItem()
        }
        publish()
    }

    /** Restarts the current track when past the intro, otherwise steps back (wrapping to the end). */
    fun previous() {
        val player = controller ?: return
        val count = player.mediaItemCount
        if (count > 1 && player.currentPosition <= RESTART_THRESHOLD_MS) {
            val index = player.currentMediaItemIndex
            player.seekToDefaultPosition(if (index - 1 < 0) count - 1 else index - 1)
        } else if (count > 1) {
            player.seekTo(0L)
        } else {
            player.seekToPreviousMediaItem()
        }
        publish()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        publish()
    }

    /** Jumps [deltaMs] relative to the current position, clamped to the track bounds. */
    fun seekBy(deltaMs: Long) {
        val player = controller ?: return
        val duration = player.duration.takeIf { it != C.TIME_UNSET && it > 0L }
        val target = (player.currentPosition + deltaMs).coerceAtLeast(0L)
        player.seekTo(if (duration != null) target.coerceAtMost(duration) else target)
        publish()
    }

    /** Steps the playback mode 随机 → 列表循环 → 单曲循环 → 随机. */
    fun cyclePlaybackMode() {
        val player = controller ?: return
        player.applyPlaybackMode(
            when (player.playbackMode()) {
                PlaybackMode.SHUFFLE -> PlaybackMode.REPEAT_ALL
                PlaybackMode.REPEAT_ALL -> PlaybackMode.REPEAT_ONE
                PlaybackMode.REPEAT_ONE -> PlaybackMode.SHUFFLE
            },
        )
        publish()
    }

    /**
     * Replaces the queue with [queue] and starts playing at [startIndex].
     *
     * The caller passes the **whole library**, not one album, so playback runs on across album
     * boundaries and a repeat mode loops the library rather than a single album.
     */
    fun playQueue(queue: List<Track>, startIndex: Int, mode: PlaybackMode) {
        val player = controller ?: return
        val items = queue.map { it.toMediaItem() }
        if (items.isEmpty()) return
        val index = startIndex.coerceIn(0, items.lastIndex)

        player.applyPlaybackMode(mode)

        // Tapping the track that is already loaded must not restart it from the top — it should
        // just carry on (or resume if paused). Only rebuild the queue when the selection actually
        // changes.
        val alreadyLoaded = player.currentMediaItem?.mediaId == items[index].mediaId
        if (!alreadyLoaded) {
            player.setMediaItems(items, index, /* startPositionMs = */ 0L)
            player.prepare()
        }
        player.play()
        publish()
    }

    private fun publish() {
        val player = controller
        if (player == null) {
            _state.value = PlayerUiState()
            return
        }
        // Read the metadata off the current MediaItem rather than `player.mediaMetadata`: the
        // latter is the *combined* metadata and a MediaController can still be reporting the
        // previous item's value for a moment after a transition, which surfaced as the now-playing
        // text lagging one track behind the audio.
        val currentItem = player.currentMediaItem
        val metadata = currentItem?.mediaMetadata ?: player.mediaMetadata
        _state.value = PlayerUiState(
            isConnected = true,
            isPlaying = player.isPlaying,
            mediaId = currentItem?.mediaId,
            title = metadata.title?.toString(),
            artist = metadata.artist?.toString(),
            album = metadata.albumTitle?.toString(),
            artworkUri = metadata.artworkUri,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = player.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: 0L,
            bufferedMs = player.bufferedPosition.coerceAtLeast(0L),
            shuffleEnabled = player.shuffleModeEnabled,
            repeatMode = player.repeatMode,
            // Derived from the queue size, not `hasNextMediaItem()` — see `next()` for why that
            // predicate is unreliable under REPEAT_MODE_ONE.
            hasNext = player.mediaItemCount > 0,
            hasPrevious = player.mediaItemCount > 0,
            queueSize = player.mediaItemCount,
        )
    }

    private fun startPositionUpdates() {
        if (positionJob != null) return
        positionJob = scope.launch {
            while (isActive) {
                // Poll even while paused so a seek (or a pause) is reflected immediately; the
                // position only actually moves while playing.
                if (controller != null) publish()
                delay(POSITION_POLL_INTERVAL_MS)
            }
        }
    }

    private fun Track.toMediaItem(): MediaItem =
        MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkUri(artUri ?: albumArtUri(albumId))
                    .setIsPlayable(true)
                    .setIsBrowsable(false)
                    .build(),
            )
            .build()

    private companion object {
        const val POSITION_POLL_INTERVAL_MS = 250L
        const val RESTART_THRESHOLD_MS = 3_000L
    }
}
