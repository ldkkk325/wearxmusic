package com.wearx.music.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wearx.music.data.Lyrics
import com.wearx.music.data.LyricsRepository
import com.wearx.music.data.model.Album
import com.wearx.music.data.model.Track
import com.wearx.music.playback.PlayerConnection
import com.wearx.music.playback.PlayerPosition
import com.wearx.music.playback.PlayerUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Playback façade plus per-track lyrics.
 *
 * [PlayerConnection] already owns the cross-process player state; this ViewModel adds the one
 * thing that needs a library — resolving the current media id back to a [Track] so lyrics can
 * be loaded — and keeps that lookup out of the composition tree. The lyrics load is keyed off
 * track identity, so it cancels into the repository's cache instead of re-parsing on every
 * recomposition.
 */
class PlayerViewModel(
    private val player: PlayerConnection,
    private val lyricsRepository: LyricsRepository,
    albums: StateFlow<List<Album>>,
) : ViewModel() {

    val state: StateFlow<PlayerUiState> = player.state
    val position: StateFlow<PlayerPosition> = player.position

    private val _lyrics = MutableStateFlow<Lyrics>(Lyrics.None)
    val lyrics: StateFlow<Lyrics> = _lyrics.asStateFlow()

    private val _lyricsLoading = MutableStateFlow(false)
    val lyricsLoading: StateFlow<Boolean> = _lyricsLoading.asStateFlow()

    init {
        val currentTrack = combine(albums, player.state) { library, playback ->
            val id = playback.mediaId?.toLongOrNull()
            library.firstNotNullOfOrNull { album -> album.tracks.firstOrNull { it.id == id } }
        }.distinctUntilChanged()

        viewModelScope.launch {
            currentTrack.collect { track ->
                if (track == null) {
                    _lyrics.value = Lyrics.None
                    _lyricsLoading.value = false
                } else {
                    _lyricsLoading.value = true
                    _lyrics.value = lyricsRepository.load(track)
                    _lyricsLoading.value = false
                }
            }
        }
    }

    fun playPause() = player.playPause()

    fun next() = player.next()

    fun previous() = player.previous()

    fun seekBy(deltaMs: Long) = player.seekBy(deltaMs)

    fun cyclePlaybackMode() = player.cyclePlaybackMode()

    fun playQueue(queue: List<Track>, startIndex: Int) = player.playQueue(queue, startIndex)

    fun playNext(track: Track) = player.playNext(track)
}
