package dev.chieppa.stretch.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch

/**
 * A spinner for work of unknown length that blocks one spot: a button that is saving, a list that is loading.
 * Use [ProgressBar] when the fraction is known and a [Skeleton] when the layout of the result is.
 */
@Composable
fun Spinner(contentDescription: String, modifier: Modifier = Modifier, size: Dp = 16.dp) {
    val c = Stretch.colors
    val turn by rememberInfiniteTransition().animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)))
    Canvas(
        modifier.size(size).semantics {
            this.contentDescription = contentDescription
            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
        },
    ) {
        val width = (this.size.minDimension / 8).coerceAtLeast(1.5.dp.toPx())
        val inset = width / 2
        val arcSize = androidx.compose.ui.geometry.Size(this.size.width - width, this.size.height - width)
        val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
        drawArc(c.track, 0f, 360f, false, topLeft, arcSize, style = Stroke(width))
        rotate(turn) { drawArc(c.inkControl, -90f, 100f, false, topLeft, arcSize, style = Stroke(width, cap = StrokeCap.Round)) }
    }
}

/**
 * A placeholder in the shape of content that is on its way: a thumbnail, a row, a stat. It pulses gently in
 * `surface-control`. Match the real layout so nothing jumps when the content lands.
 */
@Composable
fun Skeleton(modifier: Modifier = Modifier, shape: Shape = Stretch.shapes.rowShape) {
    val pulse by rememberInfiniteTransition().animateFloat(1f, 0.55f, infiniteRepeatable(tween(800), RepeatMode.Reverse))
    Box(modifier.alpha(pulse).clip(shape).background(Stretch.colors.surfaceControl))
}

/** Skeleton lines for a block of text; the last line is shorter. */
@Composable
fun SkeletonText(lines: Int, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(lines) { i -> Skeleton(Modifier.fillMaxWidth(if (i == lines - 1 && lines > 1) 0.6f else 1f).height(10.dp)) }
    }
}
