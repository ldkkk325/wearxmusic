package com.wearx.music.ui.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.wearx.music.R
import com.wearx.music.ui.components.rememberPressShape
import com.wearx.music.ui.components.rememberTapFeedback
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The welcome hero: aurora background, a plain 欢迎 title with a short entrance, and the enter
 * button. No stroke-by-stroke writing — a direct title is what was asked for.
 */
@Composable
fun WelcomeAnimation(
    reduceMotion: Boolean,
    onEnter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AuroraBackground(Modifier.fillMaxSize())

        val titleAlpha by animateFloatAsState(
            targetValue = if (shown || reduceMotion) 1f else 0f,
            animationSpec = tween(
                durationMillis = if (reduceMotion) 0 else 700,
                easing = LinearEasing,
            ),
            label = "welcomeTitleAlpha",
        )
        val titleScale by animateFloatAsState(
            targetValue = if (shown || reduceMotion) 1f else 0.9f,
            animationSpec = tween(
                durationMillis = if (reduceMotion) 0 else 700,
                easing = LinearEasing,
            ),
            label = "welcomeTitleScale",
        )

        Text(
            text = stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 72.sp,
                color = Color.White,
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer {
                alpha = titleAlpha
                scaleX = titleScale
                scaleY = titleScale
            },
        )

        EnterButton(
            visible = shown || reduceMotion,
            onEnter = onEnter,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp),
        )
    }
}

// ----------------------------------------------------------------- background

/**
 * The aurora: deep base with large, slowly drifting colour fields, blurred into one soft
 * gradient — its own blurred canvas, so the blur never reaches the title. The three colours
 * are the app's own seed family (violet / rose / teal), not stock Material primaries.
 */
@Composable
private fun AuroraBackground(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "aurora")
    val t1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(11_000, easing = LinearEasing), RepeatMode.Restart),
        label = "aurora1",
    )
    val t2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Restart),
        label = "aurora2",
    )

    Canvas(modifier.blur(48.dp)) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF1B1824), Color(0xFF0B0914))))
        val radius = size.minDimension * 0.58f

        fun blob(color: Color, cx: Float, cy: Float, alpha: Float, r: Float) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(color.copy(alpha = alpha), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = r,
                ),
                radius = r,
                center = Offset(cx, cy),
            )
        }

        blob(
            Color(0xFF8B5CF6),
            size.width * (0.32f + 0.12f * sin(t1)),
            size.height * (0.34f + 0.10f * cos(t1)),
            alpha = 0.50f,
            r = radius,
        )
        blob(
            Color(0xFFE8467C),
            size.width * (0.70f + 0.10f * cos(t2)),
            size.height * (0.62f + 0.12f * sin(t2)),
            alpha = 0.40f,
            r = radius,
        )
        blob(
            Color(0xFF0E8C74),
            size.width * (0.55f + 0.14f * sin(t2 + 1.5f)),
            size.height * (0.85f + 0.06f * cos(t1 + 0.7f)),
            alpha = 0.32f,
            r = radius * 0.9f,
        )
    }
}

// -------------------------------------------------------------------- button

@Composable
private fun EnterButton(
    visible: Boolean,
    onEnter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val tapFeedback = rememberTapFeedback()
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.6f,
        animationSpec = tween(durationMillis = 420, easing = LinearEasing),
        label = "enterScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 360),
        label = "enterAlpha",
    )

    Button(
        onClick = {
            tapFeedback()
            onEnter()
        },
        enabled = visible,
        modifier = modifier
            .size(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
        // Same press-morph as every other button in the app.
        shape = rememberPressShape(interactionSource),
        colors = ButtonDefaults.buttonColors(),
        interactionSource = interactionSource,
        contentPadding = PaddingValues(0.dp),
    ) {
        // Explicit centring at an explicit size, so the glyph sits dead centre of the circle.
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(R.string.onboarding_enter),
                modifier = Modifier.size(26.dp),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}
