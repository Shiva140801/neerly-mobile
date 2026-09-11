package com.neerly.mobile.core.design

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shimmer placeholders — the loading language the new customer designs use
 * everywhere a spinner used to sit.
 *
 * The canvas draws every placeholder as
 * `linear-gradient(90deg, Ink100 25%, <near-white> 37%, Ink100 63%)` swept over
 * a 720px band on a 1.4s linear loop. [rememberShimmerBrush] reproduces that:
 * a single infinite transition per call site drives a horizontally translating
 * three-stop gradient.
 *
 * The highlight is derived from the existing ink scale rather than introduced
 * as a new hex — Ink50 lifted halfway to Paper lands on the canvas value, and
 * keeps `Tokens.kt` the single source of colour truth.
 */
private val ShimmerBand = 360.dp
private val ShimmerTravel = 720.dp
private const val ShimmerDurationMillis = 1400

private val ShimmerBase: Color get() = NeerlyColors.Ink100
private val ShimmerHighlight: Color get() = lerp(NeerlyColors.Ink50, NeerlyColors.Paper, 0.5f)

/**
 * A horizontally sweeping gradient for placeholder surfaces.
 *
 * @param base the resting colour of the placeholder; pass a tinted value
 *   (e.g. [NeerlyColors.CustomerSoft]) for hero areas the design tints.
 */
@Composable
fun rememberShimmerBrush(
    base: Color = ShimmerBase,
    highlight: Color = ShimmerHighlight
): Brush {
    val density = LocalDensity.current
    val bandPx = with(density) { ShimmerBand.toPx() }
    val travelPx = with(density) { ShimmerTravel.toPx() }

    val transition = rememberInfiniteTransition(label = "shimmer")
    val offset by transition.animateFloat(
        initialValue = -travelPx,
        targetValue = travelPx,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = ShimmerDurationMillis, easing = LinearEasing)
        ),
        label = "shimmer-offset"
    )

    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(offset, 0f),
        end = Offset(offset + bandPx, 0f)
    )
}

/** Paints a shimmering placeholder fill, clipped to [shape]'s corner radius. */
fun Modifier.shimmer(brush: Brush, radius: Dp = NeerlyRadius.sm): Modifier =
    this.clip(RoundedCornerShape(radius)).background(brush)

/**
 * One placeholder rectangle. Width defaults to "fill the row" so callers can
 * describe a skeleton as a stack of heights, the way the canvas does.
 */
@Composable
fun SkeletonBlock(
    height: Dp,
    modifier: Modifier = Modifier,
    width: Dp? = null,
    radius: Dp = NeerlyRadius.sm,
    brush: Brush = rememberShimmerBrush()
) {
    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier)
            .height(height)
            .shimmer(brush, radius)
    )
}

/**
 * Wraps a screen's skeleton so assistive tech announces "Loading" once instead
 * of reading out a dozen anonymous boxes.
 */
fun Modifier.loadingSemantics(label: String = "Loading"): Modifier =
    this.semantics { contentDescription = label }
