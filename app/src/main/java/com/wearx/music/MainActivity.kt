package com.wearx.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.wearx.music.ui.WearXMusicRoot
import com.wearx.music.ui.theme.WearXMusicTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearXMusicTheme {
                WearXMusicRoot()
            }
        }
    }
}
