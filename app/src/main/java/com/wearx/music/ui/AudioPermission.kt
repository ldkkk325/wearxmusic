package com.wearx.music.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
    /** The system will no longer show a dialog; asking again would silently do nothing. */
    val permanentlyDenied: Boolean,
    /** Opens the system dialog — or the app's settings page once that dialog is off the table. */
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
 *
 * Permanent denial is detected through `shouldShowRequestPermissionRationale`: it turns false
 * exactly when the system has decided not to ask again (twice denied, or "don't ask again"), and
 * is the only signal that makes a second [request] pointless. In that state the request routes to
 * the app's own settings page instead — without that, the button on [com.wearx.music.ui.screens.PermissionScreen]
 * is a dead end and the app can never be used.
 */
@Composable
fun rememberAudioPermissionState(): AudioPermissionState {
    val context = LocalContext.current
    val activity = context as? Activity
    var granted by remember { mutableStateOf(context.hasAudioPermission()) }
    var hasRequested by rememberSaveable { mutableStateOf(false) }
    var rationaleAvailable by remember {
        mutableStateOf(activity?.shouldShowRequestPermissionRationale(audioPermission) ?: true)
    }

    fun refresh() {
        granted = context.hasAudioPermission()
        rationaleAvailable = activity
            ?.shouldShowRequestPermissionRationale(audioPermission)
            ?: true
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { result ->
        granted = result
        rationaleAvailable = activity?.shouldShowRequestPermissionRationale(audioPermission) ?: !result
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permanentlyDenied = hasRequested && !granted && !rationaleAvailable

    return AudioPermissionState(
        granted = granted,
        permanentlyDenied = permanentlyDenied,
        request = {
            hasRequested = true
            if (permanentlyDenied) {
                context.openAppSettings()
            } else {
                launcher.launch(audioPermission)
            }
        },
    )
}

private fun Context.hasAudioPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, audioPermission) == PackageManager.PERMISSION_GRANTED

/** Straight to this app's page in system settings, where the permission can be granted by hand. */
private fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) }
        .onFailure { startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
