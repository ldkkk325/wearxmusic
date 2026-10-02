package com.wearx.music

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import com.wearx.music.data.LyricsRepository
import com.wearx.music.data.MusicLibraryRepository
import com.wearx.music.data.SettingsRepository
import com.wearx.music.playback.PlayerConnection

/**
 * Owns the process-wide singletons: the MediaStore-backed library, the connection to the playback
 * service, the settings store and the lyrics lookup. Coil is configured here so album artwork gets
 * a shared memory + disk cache.
 */
class WearXMusicApp : Application(), SingletonImageLoader.Factory {

    val library: MusicLibraryRepository by lazy { MusicLibraryRepository(this) }

    val player: PlayerConnection by lazy { PlayerConnection(this, settings) }

    val settings: SettingsRepository by lazy { SettingsRepository(this) }

    val lyrics: LyricsRepository by lazy { LyricsRepository(this) }

    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context).build()
}
