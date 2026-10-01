package com.wearx.music.ui

/** The destinations of the app. Navigation is a plain back stack of these. */
sealed interface Route {
    data object Library : Route
    data class AlbumDetail(val albumId: Long) : Route
    data object NowPlaying : Route
    data object More : Route
    data object Volume : Route
    data object Settings : Route

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
        is AlbumDetail -> "album:$albumId"
    }

    companion object {
        const val ALBUM_PREFIX = "album:"

        /** Inverse of [encode]; anything unrecognised falls back to the library. */
        fun decode(key: String): Route = when {
            key.startsWith(ALBUM_PREFIX) ->
                key.removePrefix(ALBUM_PREFIX).toLongOrNull()?.let(::AlbumDetail) ?: Library

            key == "now_playing" -> NowPlaying
            key == "more" -> More
            key == "volume" -> Volume
            key == "settings" -> Settings
            else -> Library
        }
    }
}
