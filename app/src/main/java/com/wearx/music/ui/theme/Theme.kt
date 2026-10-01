package com.wearx.music.ui.theme

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.dynamicColorScheme

/**
 * Material 3 Expressive theme for Wear OS.
 *
 * The palette is taken from the user's wallpaper ("Material You") whenever the platform can supply
 * one, and otherwise falls back to [ExpressiveFallbackScheme] — a hand-tuned, high-chroma dark
 * scheme in the spirit of M3 Expressive. Everything else (type scale, shapes, motion) comes from
 * the Wear Material 3 defaults, which already follow the Expressive design language.
 */
@Composable
fun WearXMusicTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colorScheme = remember(context) {
        val dynamic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dynamicColorScheme(context)
        } else {
            null
        }
        dynamic ?: ExpressiveFallbackScheme
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}

/** Dark, high-chroma fallback used when dynamic colour is unavailable (API 30). */
private val ExpressiveFallbackScheme = ColorScheme(
    primary = Color(0xFFC9BFFF),
    primaryDim = Color(0xFFA99BFF),
    primaryContainer = Color(0xFF5B3DF5),
    onPrimary = Color(0xFF2A1D6E),
    onPrimaryContainer = Color(0xFFEDE7FF),
    secondary = Color(0xFFFFB0C9),
    secondaryDim = Color(0xFFFF8FB3),
    secondaryContainer = Color(0xFFE8467C),
    onSecondary = Color(0xFF3A0A1C),
    onSecondaryContainer = Color(0xFFFFE7EE),
    tertiary = Color(0xFF8FE8D5),
    tertiaryDim = Color(0xFF6FD9C3),
    tertiaryContainer = Color(0xFF0E8C74),
    onTertiary = Color(0xFF00382C),
    onTertiaryContainer = Color(0xFFD8FFF5),
    surfaceContainerLow = Color(0xFF14121B),
    surfaceContainer = Color(0xFF1B1824),
    surfaceContainerHigh = Color(0xFF241F2E),
    onSurface = Color(0xFFE8E1EC),
    onSurfaceVariant = Color(0xFFCBC3D6),
    outline = Color(0xFF948F9C),
    outlineVariant = Color(0xFF494551),
    background = Color(0xFF000000),
    onBackground = Color(0xFFE8E1EC),
    error = Color(0xFFFFB4AB),
    errorDim = Color(0xFFFF8A80),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
)
