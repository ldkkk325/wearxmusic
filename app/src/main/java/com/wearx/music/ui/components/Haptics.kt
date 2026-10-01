package com.wearx.music.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Returns a no-arg callback that plays the platform's standard "control activated" tick.
 *
 * Only taps are wired to this. Rotary (bezel) scrolling already produces its own haptics inside
 * Wear's `rotaryScrollable`, so adding feedback there as well would double up on every detent.
 */
@Composable
fun rememberTapFeedback(): () -> Unit {
    val haptics = LocalHapticFeedback.current
    return remember(haptics) {
        { haptics.performHapticFeedback(HapticFeedbackType.ContextClick) }
    }
}
