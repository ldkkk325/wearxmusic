package com.wearx.music.ui.onboarding

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.wearx.music.R
import com.wearx.music.data.ScreenShape
import com.wearx.music.ui.components.MorphButton
import com.wearx.music.ui.components.rememberTapFeedback
import kotlinx.coroutines.launch

/** The three first-run pages, in order. */
private enum class OnboardingStep { AGREEMENT, SHAPE, WELCOME }

/**
 * The app's page-open curve, reused so onboarding reads as the same product as the pages behind
 * it: iOS's open/close curve, `cubic-bezier(0.32, 0.72, 0, 1)`.
 */
private val OnboardingEasing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
private const val OnboardingDurationMs = 420

/**
 * First-run flow: user agreement, screen shape, then the welcome animation — built from the
 * same Wear M3 Expressive pieces as the rest of the app ([ScreenScaffold],
 * [TransformingLazyColumn], [ListHeader], [MorphButton], the same page curve), so it does not
 * read as a different product bolted on the front.
 *
 * Shown INSTEAD of the whole main UI (see WearXMusicRoot) until the user finishes — disagreeing
 * exits the app, choosing a shape records it, and the welcome's enter button is what finally
 * completes onboarding.
 */
@Composable
fun OnboardingFlow(
    reduceMotion: Boolean,
    onFinished: (ScreenShape) -> Unit,
) {
    var step by rememberSaveable { mutableStateOf(OnboardingStep.AGREEMENT) }
    var chosenShape by rememberSaveable { mutableStateOf(ScreenShape.ROUND) }

    // The exit fade: the enter-button click runs this to zero, THEN reports completion — so the
    // user watches the flow leave rather than the app teleporting.
    val exitAlpha = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    // Only the shape page steps back, to the agreement. The welcome page is the commit point.
    BackHandler(enabled = step == OnboardingStep.SHAPE) {
        step = OnboardingStep.AGREEMENT
    }

    Box(
        modifier = Modifier.graphicsLayer {
            alpha = exitAlpha.value
            scaleX = 0.94f + 0.06f * exitAlpha.value
            scaleY = 0.94f + 0.06f * exitAlpha.value
        },
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val forward = targetState.ordinal > initialState.ordinal
                val enter = if (forward) {
                    slideInHorizontally(tween(OnboardingDurationMs, easing = OnboardingEasing)) { it / 3 } +
                        fadeIn(tween(OnboardingDurationMs, easing = OnboardingEasing))
                } else {
                    slideInHorizontally(tween(OnboardingDurationMs, easing = OnboardingEasing)) { -it / 4 } +
                        fadeIn(tween(OnboardingDurationMs, easing = OnboardingEasing))
                }
                val exit = if (forward) {
                    slideOutHorizontally(tween(OnboardingDurationMs, easing = OnboardingEasing)) { -it / 4 } +
                        fadeOut(tween(OnboardingDurationMs / 2))
                } else {
                    slideOutHorizontally(tween(OnboardingDurationMs, easing = OnboardingEasing)) { it / 3 } +
                        fadeOut(tween(OnboardingDurationMs / 2))
                }
                enter togetherWith exit
            },
            label = "onboarding",
        ) { current ->
            when (current) {
                OnboardingStep.AGREEMENT -> AgreementStep(
                    onAgree = { step = OnboardingStep.SHAPE },
                    onDisagree = { context -> context.exitApp() },
                )

                OnboardingStep.SHAPE -> ShapeStep(
                    onSelect = { shape ->
                        chosenShape = shape
                        step = OnboardingStep.WELCOME
                    },
                )

                OnboardingStep.WELCOME -> WelcomeAnimation(
                    reduceMotion = reduceMotion,
                    onEnter = {
                        if (reduceMotion) {
                            onFinished(chosenShape)
                        } else {
                            scope.launch {
                                exitAlpha.animateTo(
                                    0f,
                                    tween(480, easing = OnboardingEasing),
                                )
                                onFinished(chosenShape)
                            }
                        }
                    },
                )
            }
        }
    }
}

// ------------------------------------------------------------------ agreement

@Composable
private fun AgreementStep(
    onAgree: () -> Unit,
    onDisagree: (Context) -> Unit,
) {
    val context = LocalContext.current
    val scrollState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(scrollState = scrollState) { padding ->
        TransformingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = scrollState,
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                ListHeader {
                    Text(
                        text = stringResource(R.string.onboarding_terms_title),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            item {
                Card(
                    modifier = Modifier
                        .transformedHeight(this, transformationSpec)
                        .fillMaxWidth(),
                    transformation = SurfaceTransformation(transformationSpec),
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_terms_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                MorphButton(
                    onClick = onAgree,
                    modifier = Modifier
                        .transformedHeight(this, transformationSpec)
                        .fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(),
                    icon = {
                        Icon(Icons.Filled.Check, contentDescription = null)
                    },
                    label = {
                        Text(stringResource(R.string.onboarding_agree))
                    },
                )
            }
            item {
                MorphButton(
                    onClick = { onDisagree(context) },
                    modifier = Modifier
                        .transformedHeight(this, transformationSpec)
                        .fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    icon = {
                        Icon(Icons.Filled.Close, contentDescription = null)
                    },
                    label = {
                        Text(stringResource(R.string.onboarding_disagree))
                    },
                )
            }
        }
    }
}

/** Force-quits: agreement is a precondition, not a suggestion. */
private fun Context.exitApp() {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) {
            context.finishAndRemoveTask()
            return
        }
        context = context.baseContext
    }
}

// --------------------------------------------------------------- shape picker

@Composable
private fun ShapeStep(
    onSelect: (ScreenShape) -> Unit,
) {
    val scrollState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(scrollState = scrollState) { padding ->
        TransformingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = scrollState,
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                ListHeader {
                    Text(
                        text = stringResource(R.string.onboarding_shape_title),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            item {
                ShapeCard(
                    label = stringResource(R.string.onboarding_shape_round),
                    preview = PreviewShape.ROUND,
                    onClick = { onSelect(ScreenShape.ROUND) },
                    modifier = Modifier
                        .transformedHeight(this, transformationSpec)
                        .fillMaxWidth(),
                )
            }
            item {
                ShapeCard(
                    label = stringResource(R.string.onboarding_shape_square),
                    preview = PreviewShape.SQUARE,
                    onClick = { onSelect(ScreenShape.SQUARE) },
                    modifier = Modifier
                        .transformedHeight(this, transformationSpec)
                        .fillMaxWidth(),
                )
            }
        }
    }
}

private enum class PreviewShape { ROUND, SQUARE }

@Composable
private fun ShapeCard(
    label: String,
    preview: PreviewShape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val tapFeedback = rememberTapFeedback()

    Card(
        onClick = {
            tapFeedback()
            onClick()
        },
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Canvas(Modifier.size(36.dp)) {
                val inset = 4.dp.toPx()
                when (preview) {
                    PreviewShape.ROUND -> drawCircle(
                        color = primary,
                        radius = size.minDimension / 2 - inset,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            join = StrokeJoin.Round,
                        ),
                    )

                    PreviewShape.SQUARE -> drawRoundRect(
                        color = primary,
                        cornerRadius = CornerRadius(10.dp.toPx()),
                        size = Size(size.width - inset * 2, size.height - inset * 2),
                        topLeft = Offset(inset, inset),
                        style = Stroke(
                            width = 3.dp.toPx(),
                            join = StrokeJoin.Round,
                        ),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                )
                Text(
                    text = stringResource(
                        when (preview) {
                            PreviewShape.ROUND -> R.string.onboarding_shape_round_hint
                            PreviewShape.SQUARE -> R.string.onboarding_shape_square_hint
                        },
                    ),
                    style = MaterialTheme.typography.bodyExtraSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
    }
}
