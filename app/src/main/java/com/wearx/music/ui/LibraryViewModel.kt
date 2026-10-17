package com.wearx.music.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wearx.music.WearXMusicApp
import com.wearx.music.data.model.Album
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Owns the music library: the initial load, silent reloads when MediaStore changes underneath
 * us, and the manual rescan.
 *
 * Living in a ViewModel rather than in `remember` inside [WearXMusicRoot] is what makes a
 * rotation (or any configuration change) keep the library instead of re-walking the disk, and
 * what makes the load logic testable at all. The MediaStore observer is registered once, on
 * first grant, and closed when the ViewModel is finished for good.
 */
class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val library = (application as WearXMusicApp).library

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRescanning = MutableStateFlow(false)
    val isRescanning: StateFlow<Boolean> = _isRescanning.asStateFlow()

    private var granted = false

    /** CONFLATED: a full rescan signals once per file, and one reload is the point. */
    private val libraryChanged = Channel<Unit>(Channel.CONFLATED)

    private var stopObserving: (() -> Unit)? = null

    init {
        viewModelScope.launch {
            for (change in libraryChanged) {
                delay(LIBRARY_CHANGE_DEBOUNCE_MS)
                if (granted) reload(silent = true)
            }
        }
    }

    /**
     * Called from the UI whenever the audio permission's truth value changes. A grant loads the
     * library (with the loading indicator when there is nothing on screen yet); a revoke clears
     * it — the tracks are no longer readable, so keeping them listed would be a lie.
     */
    fun onPermissionChanged(isGranted: Boolean) {
        granted = isGranted
        if (!isGranted) {
            _albums.value = emptyList()
            _isLoading.value = false
            return
        }
        if (stopObserving == null) {
            stopObserving = library.observe { libraryChanged.trySend(Unit) }
        }
        viewModelScope.launch {
            reload(silent = _albums.value.isNotEmpty())
        }
    }

    /**
     * Re-indexes Music and refreshes the library. One entry point for both the empty-state
     * button and the settings row — a non-empty library used to have no way to pick up new
     * files at all.
     */
    fun rescan() {
        if (_isRescanning.value || !granted) return
        viewModelScope.launch {
            _isRescanning.value = true
            runCatching { library.requestRescan() }
                .onFailure { error -> Log.w(TAG, "Rescan failed", error) }
            reload(silent = true)
            _isRescanning.value = false
        }
    }

    private suspend fun reload(silent: Boolean) {
        if (!silent) _isLoading.value = true
        _albums.value = runCatching { library.loadLibrary() }
            .getOrElse { error ->
                Log.w(TAG, "Library load failed", error)
                emptyList()
            }
        if (!silent) _isLoading.value = false
    }

    override fun onCleared() {
        stopObserving?.invoke()
        stopObserving = null
    }

    private companion object {
        const val TAG = "LibraryViewModel"

        /** How long a burst of MediaStore-change signals is collapsed into one reload. */
        const val LIBRARY_CHANGE_DEBOUNCE_MS = 1_500L
    }
}
