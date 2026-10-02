package com.wearx.music.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.ColorScheme
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.QuantizerCelebi
import com.google.android.material.color.utilities.SchemeTonalSpot
import com.google.android.material.color.utilities.Score
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The wallpaper-independent way to theme from artwork: pull one *seed* colour out of the cover, then
 * let Material 3's **Tonal Spot** scheme derive every role from it.
 *
 * Using a palette swatch directly as a container colour is what looks wrong — a raw, saturated
 * colour has no relationship to how light or dark a dark-theme container should be. Tonal Spot
 * keeps the seed's hue but assigns each role its own tone, which is why the result stays harmonious
 * no matter how garish the cover is.
 *
 * The same generator serves light mode: pass `isDark = false`.
 *
 * The seed is read from the **original, un-blurred** artwork (the blur is a rendering-only effect on
 * the backdrop); decoding the sharp image keeps the quantiser from averaging distinct hues together.
 *
 * The colour utilities come from Material Components, which is the only published home for them
 * (`material-color-utilities` has no standalone artifact). Only the `color.utilities` classes are
 * referenced; R8 removes the rest in release builds.
 */
@Composable
fun rememberArtworkSeedColor(artUri: Uri?): Color? {
    val context = LocalContext.current
    var seed by remember { mutableStateOf<Color?>(null) }

    LaunchedEffect(artUri) {
        seed = if (artUri == null) {
            null
        } else {
            withContext(Dispatchers.IO) { extractSeed(context, artUri)?.let(::Color) }
        }
    }

    return seed
}

/**
 * Rebuilds [base] as a **Tonal Spot** scheme derived from [seed], light or dark.
 *
 * The [isDark] flag is the only thing that decides the mode — every role comes from the generator,
 * so a light scheme is the same derivation with a different tone mapping rather than a second
 * hand-tuned palette that could drift away from this one.
 */
fun tonalSpotScheme(base: ColorScheme, seed: Color, isDark: Boolean = true): ColorScheme {
    val scheme = SchemeTonalSpot(
        Hct.fromInt(seed.toArgb()),
        isDark,
        /* contrastLevel = */ 0.0,
    )

    return base.copy(
        primary = Color(scheme.primary),
        primaryDim = Color(scheme.primaryFixedDim),
        primaryContainer = Color(scheme.primaryContainer),
        onPrimary = Color(scheme.onPrimary),
        onPrimaryContainer = Color(scheme.onPrimaryContainer),
        secondary = Color(scheme.secondary),
        secondaryDim = Color(scheme.secondaryFixedDim),
        secondaryContainer = Color(scheme.secondaryContainer),
        onSecondary = Color(scheme.onSecondary),
        onSecondaryContainer = Color(scheme.onSecondaryContainer),
        tertiary = Color(scheme.tertiary),
        tertiaryDim = Color(scheme.tertiaryFixedDim),
        tertiaryContainer = Color(scheme.tertiaryContainer),
        onTertiary = Color(scheme.onTertiary),
        onTertiaryContainer = Color(scheme.onTertiaryContainer),
        surfaceContainerLow = Color(scheme.surfaceContainerLow),
        surfaceContainer = Color(scheme.surfaceContainer),
        surfaceContainerHigh = Color(scheme.surfaceContainerHigh),
        onSurface = Color(scheme.onSurface),
        onSurfaceVariant = Color(scheme.onSurfaceVariant),
        outline = Color(scheme.outline),
        outlineVariant = Color(scheme.outlineVariant),
        // Dark mode forces the page background to pure black rather than Tonal Spot's tinted
        // near-black; the surface containers keep their tint, which is what gives the controls their
        // colour and their hierarchy. Light mode takes the generated background as-is — a forced
        // white would lose the tint that makes the light scheme belong to the artwork.
        background = if (isDark) Color.Black else Color(scheme.background),
        onBackground = Color(scheme.onBackground),
        error = Color(scheme.error),
        errorDim = Color(scheme.error),
        errorContainer = Color(scheme.errorContainer),
        onError = Color(scheme.onError),
        onErrorContainer = Color(scheme.onErrorContainer),
    )
}

/**
 * Picks the seed the way Material You does: quantise the image down to a handful of clusters, then
 * score them (favouring pleasant, mid-tone, reasonably chromatic colours) and take the winner.
 */
private fun extractSeed(context: Context, uri: Uri): Int? {
    val key = uri.toString()
    seedCache[key]?.let { return it }

    val seed = runCatching {
        val bitmap = decodeSampled(context, uri) ?: return@runCatching null
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        bitmap.recycle()

        val quantized = QuantizerCelebi.quantize(pixels, MaxQuantizedColors)
        Score.score(quantized).firstOrNull()
    }.getOrNull()

    if (seedCache.size >= SeedCacheSize) seedCache.clear()
    seedCache[key] = seed
    return seed
}

private fun decodeSampled(context: Context, uri: Uri): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, bounds)
    }
    if (bounds.outWidth <= 0) return null

    var sample = 1
    var longest = maxOf(bounds.outWidth, bounds.outHeight)
    while (longest / 2 >= SeedSampleWidth) {
        longest /= 2
        sample *= 2
    }

    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    return context.contentResolver.openInputStream(uri)
        ?.use { BitmapFactory.decodeStream(it, null, options) }
}

private const val SeedSampleWidth = 112
private const val MaxQuantizedColors = 128
private const val SeedCacheSize = 16
private val seedCache = LinkedHashMap<String, Int?>()
