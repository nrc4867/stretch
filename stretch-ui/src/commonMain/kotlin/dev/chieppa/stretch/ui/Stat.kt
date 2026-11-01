package dev.chieppa.stretch.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chieppa.stretch.theme.Stretch

/** A signed change against a named period: "+12% vs last week". [good] says whether this direction is good. */
data class Delta(val text: String, val good: Boolean)

/**
 * A stat tile: one number that matters. [value] is already formatted (see [compactNumber]); [trend] is an optional
 * sparkline, usually the last 12 points, with the current point marked.
 */
@Composable
fun Stat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    delta: Delta? = null,
    trend: List<Float>? = null,
    trendDescription: String? = null,
) {
    val c = Stretch.colors
    val shape = Stretch.shapes.cardShape
    Column(
        modifier
            .widthIn(min = 168.dp)
            .clip(shape)
            .background(c.surfacePanel)
            .border(Stretch.stroke, c.linePanel, shape)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Label(label, Stretch.type.bodySm, c.inkMuted)
        Label(value, Stretch.type.body.copy(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold), c.inkStrong)
        if (delta != null) Label(delta.text, Stretch.type.value.copy(fontWeight = FontWeight.Medium), if (delta.good) c.deltaUp else c.deltaDown)
        if (trend != null && trend.size >= 2) Sparkline(trend, trendDescription ?: label)
    }
}

@Composable
private fun Sparkline(values: List<Float>, description: String) {
    val c = Stretch.colors
    Canvas(Modifier.padding(top = 4.dp).width(136.dp).height(28.dp).semantics { contentDescription = description }) {
        val lo = values.min()
        val hi = values.max().takeIf { it > lo } ?: (lo + 1f)
        val inset = 6.dp.toPx()
        val w = size.width - 2 * inset
        val h = size.height - 2 * inset
        fun point(i: Int) = Offset(inset + w * i / (values.size - 1), inset + h * (1 - (values[i] - lo) / (hi - lo)))
        val path = Path().apply {
            moveTo(point(0).x, point(0).y)
            for (i in 1 until values.size) lineTo(point(i).x, point(i).y)
        }
        drawPath(path, c.inkSubtle, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        val last = point(values.lastIndex)
        drawCircle(c.surfacePanel, 6.dp.toPx(), last)
        drawCircle(c.chart1, 4.dp.toPx(), last)
    }
}
