package com.wearx.music.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** Result of the audio-library permission check, plus the way to ask for it. */
data class AudioPermissionState(
    val granted: Boolean,
    val permanentlyDenied: Boolean,
    val request: () -> Unit,
)

/** `READ_MEDIA_AUDIO` on API 33+, the legacy storage permission before that. */
private val audioPermission: String
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

/**
 * Tracks the audio-read permission. It re-checks on every `ON_RESUME` so a grant made from the
 * system Settings screen is picked up without restarting the app.
 */
@Composable
fun rememberAudioPermissionState(): AudioPermissionState {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(context.hasAudioPermission()) }
    var hasRequested by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { result -> granted = result }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) granted = context.hasAudioPermission()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return AudioPermissionState(
        granted = granted,
        permanentlyDenied = hasRequested && !granted,
        request = {
            hasRequested = true
            launcher.launch(audioPermission)
        },
    )
}

private fun Context.hasAudioPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, audioPermission) == PackageManager.PERMISSION_GRANTED
