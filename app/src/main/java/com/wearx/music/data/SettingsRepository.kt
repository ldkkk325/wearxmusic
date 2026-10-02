package com.wearx.music.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * How a freshly started queue should play.
 *
 * Shuffle is folded in with the repeat modes instead of being a separate switch: on a watch the
 * practical question is one-dimensional ("how should this play?"), and three exclusive choices fit
 * a single row of buttons where four would not.
 */
enum class PlaybackMode { SHUFFLE, REPEAT_ALL, REPEAT_ONE }

data class AppSettings(
    /** Turns off the wave ripple, the cover flip and the staggered list entrances. */
    val reduceMotion: Boolean = false,
    /** Tints the app's controls with a colour lifted from the current artwork. */
    val dynamicColor: Boolean = true,
    /** Global UI scale, applied by overriding `LocalDensity` for the whole tree. */
    val uiScale: Float = 1f,
    /**
     * How playback loops, remembered across playback starts and app restarts.
     *
     * This is the authoritative copy. The player's shuffle/repeat flags are what actually drive
     * playback, but they live on the ExoPlayer in the service and are rebuilt from nothing when the
     * process restarts — so the chosen mode has to be stored here and re-applied on connect.
     */
    val playbackMode: PlaybackMode = PlaybackMode.REPEAT_ALL,
) {
    companion object {
        val UI_SCALE_CHOICES = listOf(0.85f, 1f, 1.15f, 1.3f)
    }
}

/**
 * Tiny `SharedPreferences`-backed settings store, exposed as a [StateFlow] so Compose can collect
 * it. Deliberately not DataStore: one dependency less, and the payload is a few primitives.
 */
class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("wearx_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun setReduceMotion(value: Boolean) {
        prefs.edit().putBoolean(KEY_REDUCE_MOTION, value).apply()
        _settings.value = read()
    }

    fun setDynamicColor(value: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, value).apply()
        _settings.value = read()
    }

    fun setUiScale(value: Float) {
        prefs.edit().putFloat(KEY_UI_SCALE, value).apply()
        _settings.value = read()
    }

    fun setPlaybackMode(value: PlaybackMode) {
        prefs.edit().putString(KEY_PLAYBACK_MODE, value.name).apply()
        _settings.value = read()
    }

    private fun read() = AppSettings(
        reduceMotion = prefs.getBoolean(KEY_REDUCE_MOTION, false),
        dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, true),
        uiScale = prefs.getFloat(KEY_UI_SCALE, 1f)
            .takeIf { scale -> AppSettings.UI_SCALE_CHOICES.any { it == scale } }
            ?: 1f,
        // Read by name, and fall back rather than throwing: a stored name that no longer exists
        // (a renamed constant, a downgraded build) must not take the app down on startup.
        playbackMode = prefs.getString(KEY_PLAYBACK_MODE, null)
            ?.let { name -> PlaybackMode.entries.firstOrNull { it.name == name } }
            ?: PlaybackMode.REPEAT_ALL,
    )

    private companion object {
        const val KEY_REDUCE_MOTION = "reduce_motion"
        const val KEY_DYNAMIC_COLOR = "dynamic_color"
        const val KEY_UI_SCALE = "ui_scale"
        const val KEY_PLAYBACK_MODE = "playback_mode"
    }
}
