package com.wearx.music.ui

/**
 * The pages the settings screen opens into.
 *
 * Each is its own destination rather than a section of one long page: six settings as one scroll
 * means a long way to travel to change one thing, and the categories are the unit people actually
 * think in. Adding a setting means putting it in a section — no change to this enum, and none to
 * the navigation.
 */
enum class SettingsSection { APPEARANCE, MOTION, PLAYBACK, ABOUT }

/** The destinations of the app. Navigation is a plain back stack of these. */
sealed interface Route {
    data object Library : Route
    data class AlbumDetail(val albumId: Long) : Route
    data object NowPlaying : Route
    data object More : Route
    data object Volume : Route
    data object Settings : Route
    data class SettingsSectionPage(val section: SettingsSection) : Route
    data object Search : Route

    /**
     * Serialises this route to a `String` so the back stack can go through `rememberSaveable` and
     * survive process death.
     */
    fun encode(): String = when (this) {
        Library -> "library"
        NowPlaying -> "now_playing"
        More -> "more"
        Volume -> "volume"
        Settings -> "settings"
        Search -> "search"
        is AlbumDetail -> "album:$albumId"
        is SettingsSectionPage -> "settings:${section.name}"
    }

    companion object {
        const val ALBUM_PREFIX = "album:"
        const val SETTINGS_PREFIX = "settings:"

        /** Inverse of [encode]; anything unrecognised falls back to the library. */
        fun decode(key: String): Route = when {
            key.startsWith(SETTINGS_PREFIX) ->
                key.removePrefix(SETTINGS_PREFIX)
                    .let { name -> SettingsSection.entries.firstOrNull { it.name == name } }
                    ?.let(::SettingsSectionPage)
                    ?: Library

            key.startsWith(ALBUM_PREFIX) ->
                key.removePrefix(ALBUM_PREFIX).toLongOrNull()?.let(::AlbumDetail) ?: Library

            key == "now_playing" -> NowPlaying
            key == "more" -> More
            key == "volume" -> Volume
            key == "settings" -> Settings
            key == "search" -> Search
            else -> Library
        }
    }
}
