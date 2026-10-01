package com.wearx.music.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.wearx.music.R

/** Shown when the audio-read permission has not been granted yet. */
@Composable
fun PermissionScreen(
    permanentlyDenied: Boolean,
    onRequestPermission: () -> Unit,
) {
    val scrollState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = scrollState) { padding ->
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = scrollState,
            contentPadding = padding,
        ) {
            item {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp),
                )
            }
            item {
                Text(
                    text = stringResource(R.string.permission_title),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
            }
            item {
                Text(
                    text = stringResource(
                        if (permanentlyDenied) R.string.permission_denied else R.string.permission_rationale,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth(),
                    icon = { Icon(Icons.Filled.LibraryMusic, contentDescription = null) },
                    label = { Text(stringResource(R.string.permission_grant)) },
                )
            }
        }
    }
}
