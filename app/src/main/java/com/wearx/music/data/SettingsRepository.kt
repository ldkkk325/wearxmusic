package com.wearx.music.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Which way the app's palette faces.
 *
 * [SYSTEM] follows the watch. [DARK] stays the default, so adding a light scheme cannot change how
 * the app looks for anyone who does not go looking for it.
 */
enum class ThemeMode { SYSTEM, DARK, LIGHT }

/**
 * The physical shape of the watch's display.
 *
 * The transition backdrop, scrim and page clips are cut to a circle on round dials and left
 * square on square ones — the same UI either way, just no longer pretending every screen is a
 * circle. Chosen once during onboarding.
 */
enum class ScreenShape { ROUND, SQUARE }

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
    /**
     * How blurred the page being opened is at the start of the container transform, in dp. It
     * resolves to sharp as the page grows to full size. Zero turns the effect off.
     *
     * Stored as an index rather than the dp value itself so the setting survives a change to the
     * scale's spacing, and so a value from a future build cannot land between the offered steps.
     */
    val morphBlurIndex: Int = DEFAULT_MORPH_BLUR_INDEX,
    /**
     * How long a page transition runs, as an index into [TRANSITION_DURATION_CHOICES].
     *
     * One number covers the growth, the outgoing page's exit and the transition's play head —
     * they cannot be set independently, because a morph that outlives the play head would have its
     * page snap mid-growth when the transition ended underneath it.
     */
    val transitionDurationIndex: Int = DEFAULT_TRANSITION_DURATION_INDEX,
    /** Dark, light, or the watch's own setting. */
    val themeMode: ThemeMode = ThemeMode.DARK,
    /** Whether the first-run flow (agreement, screen shape, welcome) has been completed. */
    val onboardingCompleted: Boolean = false,
    /** Round dial or square; picked during onboarding. */
    val screenShape: ScreenShape = ScreenShape.ROUND,
    /**
     * Continuous font size multiplier applied to every typography slot — separate from
     * [uiScale], which rescales the whole density tree including icons and padding.
     */
    val fontScale: Float = DEFAULT_FONT_SCALE,
    /**
     * Continuous MiSans `wght` axis value (150–700). The font is variable, so this is a real
     * weight, not a choice among bundled statics.
     */
    val fontWeight: Float = DEFAULT_FONT_WEIGHT,
    /** Fade the audio in at track start and out at track end. */
    val playbackFadeEnabled: Boolean = false,
    /** Index into [FADE_DURATION_CHOICES]. */
    val playbackFadeDurationIndex: Int = DEFAULT_FADE_DURATION_INDEX,
) {
    companion object {
        val UI_SCALE_CHOICES = listOf(0.85f, 1f, 1.15f, 1.3f)

        val FONT_SCALE_RANGE = 0.85f..1.25f
        val FONT_WEIGHT_RANGE = 150f..700f

        const val DEFAULT_FONT_SCALE = 1f
        const val DEFAULT_FONT_WEIGHT = 400f

        /** How long each fade in/out runs. */
        val FADE_DURATION_CHOICES = listOf(500, 1_000, 2_000, 3_000)
        val FADE_DURATION_LABELS = listOf("0.5 秒", "1 秒", "2 秒", "3 秒")

        const val DEFAULT_FADE_DURATION_INDEX = 1

        /** Off, then increasingly blurred. Index 0 means "no blur". */
        val MORPH_BLUR_CHOICES = listOf(0f, 3f, 6f, 10f)

        const val DEFAULT_MORPH_BLUR_INDEX = 2

        /** Fast enough to feel instant through to slow enough to watch the circle grow. */
        val TRANSITION_DURATION_CHOICES = listOf(280, 420, 560, 800)

        val TRANSITION_DURATION_LABELS = listOf("很快", "标准", "慢", "很慢")

        const val DEFAULT_TRANSITION_DURATION_INDEX = 2
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

    fun setThemeMode(value: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, value.name).apply()
        _settings.value = read()
    }

    fun setScreenShape(value: ScreenShape) {
        prefs.edit().putString(KEY_SCREEN_SHAPE, value.name).apply()
        _settings.value = read()
    }

    fun completeOnboarding() {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
        _settings.value = read()
    }

    fun setFontScale(value: Float) {
        prefs.edit()
            .putFloat(KEY_FONT_SCALE, value.coerceIn(AppSettings.FONT_SCALE_RANGE))
            .apply()
        _settings.value = read()
    }

    fun setFontWeight(value: Float) {
        prefs.edit()
            .putFloat(KEY_FONT_WEIGHT, value.coerceIn(AppSettings.FONT_WEIGHT_RANGE))
            .apply()
        _settings.value = read()
    }

    fun setPlaybackFadeEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_FADE_ENABLED, value).apply()
        _settings.value = read()
    }

    fun setPlaybackFadeDurationIndex(value: Int) {
        prefs.edit()
            .putInt(KEY_FADE_DURATION, value.coerceIn(AppSettings.FADE_DURATION_CHOICES.indices))
            .apply()
        _settings.value = read()
    }

    fun setTransitionDurationIndex(value: Int) {
        prefs.edit()
            .putInt(KEY_TRANSITION_DURATION, value.coerceIn(AppSettings.TRANSITION_DURATION_CHOICES.indices))
            .apply()
        _settings.value = read()
    }

    fun setMorphBlurIndex(value: Int) {
        prefs.edit().putInt(KEY_MORPH_BLUR, value.coerceIn(AppSettings.MORPH_BLUR_CHOICES.indices)).apply()
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
        morphBlurIndex = prefs.getInt(KEY_MORPH_BLUR, AppSettings.DEFAULT_MORPH_BLUR_INDEX)
            .coerceIn(AppSettings.MORPH_BLUR_CHOICES.indices),
        transitionDurationIndex = prefs.getInt(
            KEY_TRANSITION_DURATION,
            AppSettings.DEFAULT_TRANSITION_DURATION_INDEX,
        ).coerceIn(AppSettings.TRANSITION_DURATION_CHOICES.indices),
        // By name with a fallback, so a stored name that no longer exists cannot crash startup.
        themeMode = prefs.getString(KEY_THEME_MODE, null)
            ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
            ?: ThemeMode.DARK,
        onboardingCompleted = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false),
        screenShape = prefs.getString(KEY_SCREEN_SHAPE, null)
            ?.let { name -> ScreenShape.entries.firstOrNull { it.name == name } }
            ?: ScreenShape.ROUND,
        fontScale = prefs.getFloat(KEY_FONT_SCALE, AppSettings.DEFAULT_FONT_SCALE)
            .coerceIn(AppSettings.FONT_SCALE_RANGE),
        fontWeight = prefs.getFloat(KEY_FONT_WEIGHT, AppSettings.DEFAULT_FONT_WEIGHT)
            .coerceIn(AppSettings.FONT_WEIGHT_RANGE),
        playbackFadeEnabled = prefs.getBoolean(KEY_FADE_ENABLED, false),
        playbackFadeDurationIndex = prefs.getInt(KEY_FADE_DURATION, AppSettings.DEFAULT_FADE_DURATION_INDEX)
            .coerceIn(AppSettings.FADE_DURATION_CHOICES.indices),
    )

    private companion object {
        const val KEY_REDUCE_MOTION = "reduce_motion"
        const val KEY_DYNAMIC_COLOR = "dynamic_color"
        const val KEY_UI_SCALE = "ui_scale"
        const val KEY_PLAYBACK_MODE = "playback_mode"
        const val KEY_MORPH_BLUR = "morph_blur"
        const val KEY_TRANSITION_DURATION = "transition_duration"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_SCREEN_SHAPE = "screen_shape"
        const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        const val KEY_FONT_SCALE = "font_scale"
        const val KEY_FONT_WEIGHT = "font_weight"
        const val KEY_FADE_ENABLED = "fade_enabled"
        const val KEY_FADE_DURATION = "fade_duration"
    }
}
