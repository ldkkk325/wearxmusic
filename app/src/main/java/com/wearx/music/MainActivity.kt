package com.wearx.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wearx.music.data.ThemeMode
import com.wearx.music.ui.WearXMusicRoot
import com.wearx.music.ui.theme.WearXMusicTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // The theme sits above the root, so the mode has to be read here rather than in
            // `WearXMusicRoot` — which is also where the settings singleton lives.
            val app = application as WearXMusicApp
            val settings by app.settings.settings.collectAsStateWithLifecycle()
            WearXMusicTheme(
                darkTheme = when (settings.themeMode) {
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    ThemeMode.DARK -> true
                    ThemeMode.LIGHT -> false
                },
            ) {
                WearXMusicRoot()
            }
        }
    }
}
