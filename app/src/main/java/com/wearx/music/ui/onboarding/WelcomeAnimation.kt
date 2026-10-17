package com.wearx.music.ui.onboarding

import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addSvg
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import com.wearx.music.ui.components.rememberPressShape
import com.wearx.music.ui.components.rememberTapFeedback
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ---------------------------------------------------------------------- data

/**
 * Stroke data for 欢 and 迎, from hanzi-writer-data (MIT, Make Me a Hanzi derived).
 *
 * [x]_OUTLINES are the SVG path strings of each stroke — the actual ink shape, one sub-path per
 * stroke in writing order. [x]_MEDIANS are the strokes' centre lines in the same 1024-unit box,
 * used only to steer the reveal: ink appears where the pen has passed along its own centre line,
 * which is how the iOS strokeEnd handwriting effect is built when it uses real glyph shapes.
 * y grows downward, matching Compose's Canvas.
 */
private val HUAN_OUTLINES = listOf(
    "M 332 341 Q 366 413 398 520 Q 408 550 425 569 Q 441 585 428 597 Q 415 607 383 616 Q 359 622 335 606 Q 308 593 272 580 Q 235 568 174 558 Q 161 557 156 549 Q 152 542 171 535 Q 208 523 255 540 Q 309 562 318 563 Q 343 564 341 536 Q 323 439 292 370 L 269 328 Q 206 229 78 184 Q 66 177 78 172 Q 84 165 125 169 Q 224 181 303 293 L 332 341 Z",
    "M 303 293 Q 405 182 415 179 Q 422 178 429 186 Q 439 196 430 234 Q 421 273 332 341 L 292 370 Q 213 428 168 455 Q 161 459 158 449 Q 157 439 166 429 Q 215 384 269 328 L 303 293 Z",
    "M 526 595 Q 548 637 572 692 Q 584 726 596 743 Q 603 755 596 766 Q 590 778 559 794 Q 531 806 512 802 Q 496 798 507 778 Q 531 745 485 615 Q 463 554 429 486 Q 422 476 420 469 Q 419 457 432 463 Q 459 475 511 568 L 526 595 Z",
    "M 511 568 Q 538 552 566 561 Q 641 600 728 615 Q 741 619 749 619 Q 765 609 764 601 Q 765 598 761 585 Q 737 516 745 513 Q 752 510 767 523 Q 815 563 855 578 Q 889 593 887 601 Q 886 611 813 653 Q 789 668 723 647 Q 695 647 526 595 C 497 586 484 581 511 568 Z",
    "M 616 342 Q 628 414 647 449 Q 657 468 640 477 Q 622 492 607 498 Q 585 513 564 506 Q 551 502 556 486 Q 589 426 543 275 Q 542 272 540 266 Q 494 145 371 62 Q 359 53 348 44 Q 335 35 347 32 Q 408 32 489 102 Q 535 139 565 197 Q 604 288 608 305 L 616 342 Z",
    "M 608 305 Q 618 295 626 279 Q 776 33 808 24 Q 892 24 934 30 Q 949 31 954 38 Q 958 44 949 48 Q 802 130 742 195 Q 691 247 629 328 Q 623 338 616 342 C 594 363 589 328 608 305 Z",
)

private val YING_OUTLINES = listOf(
    "M 457 618 Q 503 643 533 663 Q 603 711 631 720 Q 653 729 644 747 Q 631 766 606 782 Q 584 798 570 796 Q 557 795 560 781 Q 561 747 499 689 Q 472 664 443 634 C 422 612 431 604 457 618 Z",
    "M 443 634 Q 421 653 405 656 Q 392 657 386 654 Q 376 647 388 633 Q 421 573 408 423 Q 401 375 389 357 Q 376 336 385 314 Q 397 283 414 270 Q 424 263 437 282 Q 467 322 568 423 Q 586 439 587 446 Q 588 459 567 447 Q 530 425 487 401 Q 468 388 459 392 Q 452 396 451 416 Q 452 561 459 598 Q 460 610 457 618 L 443 634 Z",
    "M 680 581 Q 750 605 802 614 Q 815 615 817 613 Q 827 600 823 562 Q 817 460 795 414 Q 782 384 755 387 Q 731 391 710 398 Q 688 404 704 383 Q 747 347 767 319 Q 786 300 802 317 Q 851 359 868 458 Q 883 602 894 618 Q 904 630 899 638 Q 890 647 845 664 Q 827 671 811 658 Q 787 640 670 613 C 641 606 652 571 680 581 Z",
    "M 627 126 Q 633 104 639 94 Q 643 88 651 90 Q 663 99 670 153 Q 676 213 674 273 Q 671 324 674 522 Q 674 538 680 581 C 683 605 683 605 670 613 Q 642 632 628 634 Q 616 638 602 630 Q 595 623 601 613 Q 620 580 621 539 Q 625 128 627 126 Z",
    "M 238 718 Q 262 696 287 671 Q 302 656 319 657 Q 331 658 337 673 Q 341 689 332 721 Q 320 749 232 770 Q 216 774 208 771 Q 202 767 204 753 Q 208 740 238 718 Z",
    "M 296 187 Q 309 191 314 212 Q 320 245 284 309 Q 254 370 335 452 Q 335 453 337 454 Q 359 470 340 487 Q 321 503 287 520 Q 265 533 247 517 Q 213 495 169 482 Q 111 461 93 466 Q 86 467 83 462 Q 82 456 89 452 Q 126 431 235 464 Q 245 467 253 454 Q 272 432 251 406 Q 203 360 253 278 Q 280 214 278 194 Q 277 193 276 189 C 274 169 274 169 296 187 Z",
    "M 276 189 Q 236 192 192 179 Q 149 164 85 162 Q 79 162 75 154 Q 71 147 79 137 Q 95 118 119 104 Q 129 97 149 111 Q 233 162 308 144 Q 374 128 505 66 Q 734 -48 816 -21 Q 879 -2 933 35 Q 957 50 932 51 Q 697 51 529 107 Q 462 129 381 163 Q 339 182 296 187 L 276 189 Z",
)

private val HUAN_MEDIANS = listOf(
    listOf(Offset(164f, 547f), Offset(229f, 550f), Offset(339f, 583f), Offset(376f, 574f), Offset(368f, 520f), Offset(339f, 423f), Offset(313f, 357f), Offset(279f, 299f), Offset(246f, 259f), Offset(189f, 214f), Offset(113f, 180f), Offset(85f, 178f)),
    listOf(Offset(167f, 445f), Offset(363f, 275f), Offset(399f, 234f), Offset(419f, 189f)),
    listOf(Offset(515f, 789f), Offset(551f, 759f), Offset(550f, 729f), Offset(490f, 567f), Offset(428f, 470f)),
    listOf(Offset(521f, 571f), Offset(630f, 609f), Offset(757f, 638f), Offset(785f, 629f), Offset(808f, 605f), Offset(749f, 517f)),
    listOf(Offset(568f, 493f), Offset(588f, 479f), Offset(606f, 454f), Offset(586f, 329f), Offset(547f, 217f), Offset(499f, 144f), Offset(453f, 99f), Offset(412f, 68f), Offset(353f, 38f)),
    listOf(Offset(616f, 335f), Offset(625f, 308f), Offset(678f, 231f), Offset(747f, 145f), Offset(815f, 77f), Offset(837f, 66f), Offset(946f, 40f)),
)

private val YING_MEDIANS = listOf(
    listOf(Offset(569f, 786f), Offset(592f, 745f), Offset(530f, 686f), Offset(461f, 634f), Offset(453f, 636f)),
    listOf(Offset(392f, 645f), Offset(413f, 631f), Offset(431f, 599f), Offset(428f, 392f), Offset(437f, 352f), Offset(477f, 366f), Offset(578f, 445f)),
    listOf(Offset(687f, 589f), Offset(690f, 602f), Offset(706f, 608f), Offset(821f, 636f), Offset(833f, 637f), Offset(855f, 620f), Offset(843f, 473f), Offset(835f, 436f), Offset(812f, 381f), Offset(787f, 355f), Offset(704f, 392f)),
    listOf(Offset(611f, 621f), Offset(643f, 594f), Offset(649f, 575f), Offset(647f, 98f)),
    listOf(Offset(214f, 763f), Offset(296f, 710f), Offset(316f, 679f)),
    listOf(Offset(90f, 459f), Offset(150f, 459f), Offset(256f, 490f), Offset(269f, 489f), Offset(293f, 469f), Offset(284f, 424f), Offset(254f, 369f), Offset(252f, 351f), Offset(258f, 313f), Offset(289f, 247f), Offset(295f, 219f), Offset(292f, 201f), Offset(281f, 190f)),
    listOf(Offset(88f, 148f), Offset(131f, 136f), Offset(231f, 167f), Offset(300f, 167f), Offset(353f, 153f), Offset(589f, 58f), Offset(693f, 28f), Offset(756f, 17f), Offset(812f, 17f), Offset(936f, 43f)),
)

/** One character: outline paths + medians + where it sits in the shared glyph box. */
private class Glyph(
    val outlines: List<Path>,
    val medians: List<List<Offset>>,
    val originX: Float,
)

/** The shared box: 欢 at x=0, a gap, then 迎 — 1024 units tall, ~2112 wide. */
private const val GlyphBox = 1024f
private const val GlyphGap = 64f
private const val GlyphSpan = GlyphBox + GlyphGap + GlyphBox

private val HandwritingWidth = 200.dp
private val HandwritingHeight = HandwritingWidth * (GlyphBox / GlyphSpan)

private const val HandwritingMs = 2_400
private val InkFill = Color(0xFFF4EFFF)
private val InkGlow = Color(0xFFC9BFFF)

/**
 * The welcome hero: aurora background, 欢迎 written stroke-by-stroke from real SVG glyph
 * outlines, then the enter button.
 *
 * The reveal is the Compose form of the iOS CAShapeLayer.strokeEnd handwriting trick: each
 * stroke's filled outline is clipped to a mask that grows along that stroke's centre line, so
 * ink appears exactly where the (invisible) pen has travelled — in writing order, with no
 * connectors between strokes, and the finished glyphs ARE the animation's end state (no cross
 * fade to a system font, which is what made the old version jump).
 */
@Composable
fun WelcomeAnimation(
    reduceMotion: Boolean,
    onEnter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var written by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AuroraBackground(Modifier.fillMaxSize())

        HandwritingWelcome(
            reduceMotion = reduceMotion,
            onWritten = { written = true },
            modifier = Modifier.size(width = HandwritingWidth, height = HandwritingHeight),
        )

        EnterButton(
            visible = written,
            onEnter = onEnter,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp),
        )
    }
}

// ------------------------------------------------------------------ writing

@Composable
private fun HandwritingWelcome(
    reduceMotion: Boolean,
    onWritten: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val glyphs = remember {
        fun outlines(svgs: List<String>) = svgs.map { svg -> Path().apply { addSvg(svg) } }
        listOf(
            Glyph(
                outlines = outlines(HUAN_OUTLINES),
                medians = HUAN_MEDIANS,
                originX = 0f,
            ),
            Glyph(
                outlines = outlines(YING_OUTLINES),
                medians = YING_MEDIANS,
                originX = GlyphBox + GlyphGap,
            ),
        )
    }

    var written by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) progress.snapTo(1f)
        else progress.animateTo(1f, tween(HandwritingMs, easing = LinearEasing))
        written = true
        onWritten()
    }

    // The glow softens once the word is complete; the ink itself never leaves.
    val glowAlpha by animateFloatAsState(
        targetValue = if (written) 0.35f else 1f,
        animationSpec = tween(600),
        label = "inkGlow",
    )
    val tipAlpha = if (written) 0f else 1f

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            drawInk(glyphs, progress.value, glowAlpha)
        }
        // Pen tip lives on its own layer so the blur-free core and the halo stay cheap.
        if (tipAlpha > 0f) {
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .blur(6.dp),
            ) {
                drawPenTip(glyphs, progress.value)
            }
        }
    }
}

/**
 * Draws every stroke's outline, clipped to the part of its centre line already written.
 * `glowPass` renders the same ink wide and soft underneath; the sharp pass sits on top.
 */
private fun DrawScope.drawInk(glyphs: List<Glyph>, progress: Float, glowAlpha: Float) {
    val scale = size.width / GlyphSpan

    val penRadius = GlyphBox * 0.09f * scale
    val totalLength = glyphs.sumOf { glyph ->
        glyph.medians.sumOf { median -> median.polylineLength(scale, glyph.originX).toDouble() }
    }.toFloat()
    if (totalLength <= 0f) return
    val travelled = progress.coerceIn(0f, 1f) * totalLength

    var cursor = 0f
    for (glyph in glyphs) {
        for (i in glyph.medians.indices) {
            val median = glyph.medians[i]
            val strokeLength = median.polylineLength(scale, glyph.originX)
            val local = if (strokeLength <= 0f) {
                if (travelled >= cursor) 1f else 0f
            } else {
                ((travelled - cursor) / strokeLength).coerceIn(0f, 1f)
            }
            cursor += strokeLength
            if (local <= 0f) continue

            val mask = Path()
            val walked = median.walked(scale, glyph.originX, local)
            for (j in 1 until walked.size) {
                mask.addCapsule(walked[j - 1], walked[j], penRadius)
            }
            if (walked.size == 1) mask.addOvalCircle(walked[0], penRadius)

            if (glowAlpha > 0.01f) {
                clipPath(mask) {
                    drawPath(
                        path = glyph.outlines[i],
                        color = InkGlow.copy(alpha = glowAlpha * 0.9f),
                    )
                }
            }
            clipPath(mask) {
                drawPath(path = glyph.outlines[i], color = InkFill)
            }
        }
    }
}

/** Where the pen is right now — the tip of the in-progress centre line, haloed. */
private fun DrawScope.drawPenTip(glyphs: List<Glyph>, progress: Float) {
    val scale = size.width / GlyphSpan

    val totalLength = glyphs.sumOf { glyph ->
        glyph.medians.sumOf { median -> median.polylineLength(scale, glyph.originX).toDouble() }
    }.toFloat()
    if (totalLength <= 0f || progress >= 1f) return
    val travelled = progress.coerceIn(0f, 1f) * totalLength

    var cursor = 0f
    for (glyph in glyphs) {
        for (median in glyph.medians) {
            val strokeLength = median.polylineLength(scale, glyph.originX)
            val local = if (strokeLength <= 0f) 0f
            else ((travelled - cursor) / strokeLength).coerceIn(0f, 1f)
            cursor += strokeLength
            if (local in 0f..<1f) {
                val tip = median.walked(scale, glyph.originX, local).lastOrNull() ?: return
                val r = GlyphBox * 0.09f * scale
                drawCircle(InkGlow, radius = r * 2.4f, center = tip, alpha = 0.5f)
                drawCircle(Color.White, radius = r * 1.1f, center = tip)
                return
            }
        }
    }
}

private fun List<Offset>.polylineLength(scale: Float, originX: Float): Float {
    var total = 0f
    for (i in 1 until size) {
        val a = Offset((originX + this[i - 1].x) * scale, this[i - 1].y * scale)
        val b = Offset((originX + this[i].x) * scale, this[i].y * scale)
        total += (b - a).getDistance()
    }
    return total
}

/** The centre line walked to `fraction` of its length, in canvas coordinates. */
private fun List<Offset>.walked(scale: Float, originX: Float, fraction: Float): List<Offset> {
    if (size == 0) return emptyList()
    fun map(p: Offset) = Offset((originX + p.x) * scale, p.y * scale)
    val points = mutableListOf(map(first()))
    if (size == 1 || fraction <= 0f) return points

    val total = polylineLength(scale, originX)
    var remaining = total * fraction
    for (i in 1 until size) {
        val a = map(this[i - 1])
        val b = map(this[i])
        val segment = (b - a).getDistance()
        if (remaining >= segment) {
            points += b
            remaining -= segment
        } else {
            val t = if (segment <= 0f) 0f else remaining / segment
            points += Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
            break
        }
    }
    return points
}

/**
 * A capsule (rounded thick line) from a to b, as ONE contour — several contours would union by
 * winding and overlapping opposite-wound pieces punch holes under NonZero fill.
 */
private fun Path.addCapsule(a: Offset, b: Offset, radius: Float, arcSegments: Int = 12) {
    val dx = b.x - a.x
    val dy = b.y - a.y
    val length = kotlin.math.hypot(dx, dy)
    if (length < 0.001f) {
        addOvalCircle(a, radius)
        return
    }
    val ux = dx / length
    val uy = dy / length
    val nx = -uy
    val ny = ux
    val thetaN = kotlin.math.atan2(ny.toDouble(), nx.toDouble())

    moveTo(a.x + nx * radius, a.y + ny * radius)
    lineTo(b.x + nx * radius, b.y + ny * radius)
    // Swing around b from +n through +u (the direction of travel) to −n.
    for (i in 1..arcSegments) {
        val angle = thetaN - PI * i / arcSegments
        lineTo(
            b.x + kotlin.math.cos(angle).toFloat() * radius,
            b.y + kotlin.math.sin(angle).toFloat() * radius,
        )
    }
    lineTo(a.x - nx * radius, a.y - ny * radius)
    // Swing around a from −n through −u back to +n.
    val thetaNegN = thetaN + PI
    for (i in 1..arcSegments) {
        val angle = thetaNegN + PI * i / arcSegments
        lineTo(
            a.x + kotlin.math.cos(angle).toFloat() * radius,
            a.y + kotlin.math.sin(angle).toFloat() * radius,
        )
    }
    close()
}

private fun Path.addOvalCircle(center: Offset, radius: Float) {
    addOval(
        Rect(
            left = center.x - radius,
            top = center.y - radius,
            right = center.x + radius,
            bottom = center.y + radius,
        ),
    )
}

// ----------------------------------------------------------------- background

/**
 * The aurora: deep base with large, slowly drifting colour fields, blurred into one soft
 * gradient — its own blurred canvas, so the blur never reaches the ink above it. The three
 * colours are the app's own seed family (violet / rose / teal), not stock Material primaries.
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
        // Explicit centring at an explicit size: the raw Icon was stretching inside the button
        // and reading as off-centre.
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "进入 WearX Music",
                modifier = Modifier.size(26.dp),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}
