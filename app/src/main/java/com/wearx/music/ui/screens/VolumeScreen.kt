package com.wearx.music.ui.screens

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Slider
import androidx.wear.compose.material3.Text
import com.wearx.music.R
import com.wearx.music.ui.components.MorphButton
import com.wearx.music.ui.components.ScreenBackButton
import kotlin.math.roundToInt

/**
 * Media-volume page. Drives the real `STREAM_MUSIC` volume and follows changes made elsewhere (the
 * rotating crown, system UI, another app) through a content observer.
 *
 * A scrolling [Column] rather than a scaling list: with this little content the scroll range is
 * zero, so the page cannot be dragged into empty space.
 */
@Composable
fun VolumeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val maxVolume = remember(audioManager) {
        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    }
    var volume by remember { mutableIntStateOf(audioManager.mediaVolume()) }

    // The last level the user actually chose, kept so that unmuting can return to it. It has to be
    // kept here: muting works by writing 0 to the stream, which overwrites the level the system
    // would otherwise restore. `rememberSaveable` rather than `remember`, because leaving the page
    // disposes the composition and a plain `remember` would take the value with it — mute, go back,
    // come forward, and the level would be gone again.
    var lastAudibleVolume by rememberSaveable {
        mutableIntStateOf(
            audioManager.mediaVolume().takeIf { it > 0 }
                ?: (maxVolume / 2).coerceAtLeast(1),
        )
    }

    DisposableEffect(context, audioManager) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                val current = audioManager.mediaVolume()
                volume = current
                // Only ever record a level that is audible. This observer also fires for the
                // writes this screen makes itself, so our own mute would come back through here as
                // 0 and overwrite the very value the unmute needs.
                if (current > 0) lastAudibleVolume = current
            }
        }
        context.contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, observer)
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }

    fun applyVolume(target: Int) {
        val clamped = target.coerceIn(0, maxVolume)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, clamped, 0)
        volume = clamped
        if (clamped > 0) lastAudibleVolume = clamped
    }

    // Same state for scaffold and list: the `edgeButton` slot is driven by the scaffold's scroll
    // info, so a second scroll source leaves it untriggered and the button never appears.
    val scrollState = rememberTransformingLazyColumnState()

    ScreenScaffold(
        scrollState = scrollState,
        edgeButton = { ScreenBackButton(onClick = onBack) },
    ) { padding ->
        TransformingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = scrollState,
            contentPadding = padding,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                Icon(
                    imageVector = when {
                        volume == 0 -> Icons.AutoMirrored.Filled.VolumeOff
                        volume * 2 < maxVolume -> Icons.AutoMirrored.Filled.VolumeDown
                        else -> Icons.AutoMirrored.Filled.VolumeUp
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp),
                )
            }

            item {
                Text(
                    text = if (volume == 0) {
                        stringResource(R.string.volume_muted)
                    } else {
                        stringResource(R.string.volume_percent, volume * 100 / maxVolume)
                    },
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center,
                )
            }

            item {
                Text(
                    text = stringResource(R.string.volume_stream_media),
                    style = MaterialTheme.typography.bodyExtraSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            item {
                Slider(
                    value = volume.toFloat(),
                    onValueChange = { raw -> applyVolume(raw.roundToInt()) },
                    steps = (maxVolume - 1).coerceAtLeast(0),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                MorphButton(
                    // Unmute restores the level the user had set, not an arbitrary halfway point. The
                    // `coerceIn` is the fallback for a device that was already muted before this page
                    // was ever opened, where there is no previous level to return to.
                    onClick = { applyVolume(if (volume == 0) lastAudibleVolume.coerceIn(1, maxVolume) else 0) },
                    modifier = Modifier.fillMaxWidth(),
                    icon = {
                        Icon(
                            imageVector = if (volume == 0) {
                                Icons.AutoMirrored.Filled.VolumeUp
                            } else {
                                Icons.AutoMirrored.Filled.VolumeOff
                            },
                            contentDescription = null,
                        )
                    },
                    label = {
                        Text(
                            stringResource(
                                if (volume == 0) R.string.action_unmute else R.string.action_mute,
                            ),
                        )
                    },
                )
            }
        }
    }
}

private fun AudioManager.mediaVolume(): Int =
    getStreamVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(0)
