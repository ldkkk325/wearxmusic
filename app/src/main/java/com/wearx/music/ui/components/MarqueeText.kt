package com.wearx.music.ui.components

import androidx.compose.foundation.basicMarquee
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material3.Text

/**
 * A single-line [Text] that scrolls itself horizontally when it does not fit.
 *
 * `softWrap = false` with `TextOverflow.Clip` is deliberate: an ellipsis would trim the string
 * before the marquee ever gets a chance to scroll it, so the trade is "no ellipsis, but the title
 * stays readable end to end".
 */
@Composable
fun MarqueeText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
) {
    Text(
        text = text,
        modifier = modifier.basicMarquee(),
        color = color,
        style = style,
        textAlign = textAlign,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
    )
}
