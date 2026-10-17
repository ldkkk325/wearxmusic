package com.wearx.music.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.wearx.music.MainActivity

/**
 * Hosts the single [ExoPlayer] instance behind a [MediaSession].
 *
 * Because this is a [MediaSessionService], Media3 builds and posts the media notification (with
 * play/pause/skip actions and the album artwork) on our behalf, and keeps playback alive while the
 * watch UI is not in the foreground.
 *
 * The queue is also owned here, and persisted through [PlaybackQueueStore]: the system kills
 * watch services freely, and without that the next launch would come back with an empty player.
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var queueStore: PlaybackQueueStore

    private val handler = Handler(Looper.getMainLooper())

    /**
     * Periodic cursor save while playing. The listener writes the cursor on every interesting
     * event, but "the track is still where it was five seconds ago" is not an event — without
     * this, a kill mid-song would restore the right track at the start of it.
     */
    private val persistCursor = Runnable {
        mediaSession?.player?.let(queueStore::saveCursor)
        scheduleCursorPersist()
    }

    override fun onCreate() {
        super.onCreate()
        queueStore = PlaybackQueueStore(this)

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        // Restore before the session exists, so the first notification the system sees already
        // carries the old track. Prepared but NOT playing: resuming on our own is not something
        // the user asked for.
        queueStore.load()?.let { saved ->
            player.setMediaItems(saved.items, saved.index, saved.positionMs)
            player.prepare()
        }

        player.addListener(playerListener(queueStore, player))

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(openAppIntent())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        mediaSession?.run {
            // Guarded: if onCreate died before `queueStore` was assigned, this path must not add
            // an UninitializedPropertyAccessException on top of whatever went wrong.
            if (::queueStore.isInitialized) queueStore.saveCursor(player)
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    /**
     * Mirrors the player into [PlaybackQueueStore].
     *
     * The item list is written only for a real playlist change (a new `setMediaItems`); every
     * other signal is just the cursor, which is cheap enough to write on every transition, pause
     * and periodic tick.
     */
    private fun playerListener(
        store: PlaybackQueueStore,
        player: Player,
    ): Player.Listener = object : Player.Listener {
        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
                store.saveItems(player)
                store.saveCursor(player)
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            store.saveCursor(player)
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            store.saveCursor(player)
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            handler.removeCallbacks(persistCursor)
            if (isPlaying) {
                scheduleCursorPersist()
            } else {
                store.saveCursor(player)
            }
        }
    }

    private fun scheduleCursorPersist() {
        handler.removeCallbacks(persistCursor)
        handler.postDelayed(persistCursor, CURSOR_SAVE_INTERVAL_MS)
    }

    /** Tapping the media notification re-opens the watch UI. */
    private fun openAppIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        return PendingIntent.getActivity(
            this,
            /* requestCode = */ 0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private companion object {
        /**
         * How far behind the cursor save may trail while playing. Five seconds of position is
         * cheaper to re-listen to than the disk traffic of saving more often on a watch.
         */
        const val CURSOR_SAVE_INTERVAL_MS = 5_000L
    }
}
