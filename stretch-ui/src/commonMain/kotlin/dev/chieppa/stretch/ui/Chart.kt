package dev.chieppa.stretch.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchColors
import dev.chieppa.stretch.theme.stretchShadow
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * One series. [slot] picks its color, `chart-1` to `chart-8`, and belongs to the entity: keep it when a filter
 * changes which series are shown.
 */
class ChartSeries(val name: String, val values: List<Float>, val slot: Int) {
    init {
        require(slot in 0 until 8) { "Series slot $slot is outside chart-1 to chart-8; fold extra series into Other" }
    }
}

/** The series color for [slot], 0 for `chart-1`. */
fun StretchColors.chart(slot: Int): Color = when (slot) {
    0 -> chart1
    1 -> chart2
    2 -> chart3
    3 -> chart4
    4 -> chart5
    5 -> chart6
    6 -> chart7
    7 -> chart8
    else -> error("No chart color for slot $slot")
}

/** Pushes label centers apart so no two are closer than [gap], keeping their order. */
internal fun spreadLabels(centers: List<Float>, gap: Float): List<Float> {
    val order = centers.indices.sortedBy { centers[it] }
    val placed = centers.toMutableList()
    for (i in 1 until order.size) {
        val prev = placed[order[i - 1]]
        if (placed[order[i]] - prev < gap) placed[order[i]] = prev + gap
    }
    return placed
}

private class Plot(val left: Float, val right: Float, val top: Float, val bottom: Float, val lo: Float, val hi: Float) {
    fun y(v: Float) = bottom - (v - lo) / (hi - lo) * (bottom - top)
}

@Composable
private fun ChartFrame(title: String, series: List<ChartSeries>, modifier: Modifier, body: @Composable () -> Unit) {
    val c = Stretch.colors
    val shape = Stretch.shapes.cardShape
    Column(
        modifier.clip(shape).background(c.surfacePanel).border(Stretch.stroke, c.linePanel, shape).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Label(title, Stretch.type.bodySm.copy(fontWeight = FontWeight.SemiBold), c.inkStrong)
        if (series.size >= 2) Legend(series)
        body()
    }
}

/** Swatch and name for each series. Charts with two or more series always show one. */
@Composable
fun Legend(series: List<ChartSeries>, modifier: Modifier = Modifier) {
    val c = Stretch.colors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        for (s in series) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(c.chart(s.slot)))
                Label(s.name, Stretch.type.bodySm, c.ink)
            }
        }
    }
}

@Composable
private fun tickStyle() = Stretch.type.value.copy(fontSize = 11.sp, color = Stretch.colors.inkMuted)

@Composable
private fun ChartTooltip(x: Float, heading: String, rows: List<Triple<Color?, String, String>>, width: Float) {
    val c = Stretch.colors
    val shape = Stretch.shapes.ctlShape
    val density = LocalDensity.current
    val tipWidth = with(density) { 150.dp.toPx() }
    val left = if (x + 12 + tipWidth > width) x - 12 - tipWidth else x + 12
    Column(
        Modifier
            .offset { IntOffset(left.roundToInt(), 0) }
            .widthIn(max = 150.dp)
            .stretchShadow(Stretch.shadows.pop, shape)
            .clip(shape)
            .background(c.surfaceFloat)
            .border(Stretch.stroke, c.lineStrong, shape)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Label(heading, Stretch.type.bodySm.copy(fontWeight = FontWeight.SemiBold), c.inkStrong)
        for ((color, name, value) in rows) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if (color != null) Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                Label(name, Stretch.type.bodySm, c.ink, Modifier.weight(1f, fill = false))
                Label(value, Stretch.type.value, c.inkStrong)
            }
        }
    }
}

private fun Modifier.hoverPosition(onHover: (Offset?) -> Unit) = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            onHover(if (event.type == PointerEventType.Exit) null else event.changes.first().position)
        }
    }
}

private fun DrawScope.gridAndTicks(plot: Plot, ticks: List<Float>, measurer: TextMeasurer, style: TextStyle, grid: Color, axis: Color, format: (Float) -> String) {
    for (t in ticks) {
        val y = plot.y(t)
        drawLine(if (t == ticks.first()) axis else grid, Offset(plot.left, y), Offset(plot.right, y), 1.dp.toPx())
        val text = measurer.measure(format(t), style)
        drawText(text, topLeft = Offset(plot.left - 6.dp.toPx() - text.size.width, y - text.size.height / 2))
    }
}

private fun tickWidth(measurer: TextMeasurer, ticks: List<Float>, style: TextStyle, format: (Float) -> String) =
    ticks.maxOf { measurer.measure(format(it), style).size.width }.toFloat()

/**
 * A line chart on one axis. Two or more series get a legend; each line ends in a dot with its value. Hover shows
 * every series at that point. With [table] the same data shows as a [Table] instead.
 */
@Composable
fun LineChart(
    title: String,
    series: List<ChartSeries>,
    xLabels: List<String>,
    modifier: Modifier = Modifier,
    description: String = title,
    table: Boolean = false,
    format: (Float) -> String = ::formatNumber,
    height: Dp = 170.dp,
) {
    require(series.all { it.values.size == xLabels.size }) { "Every series needs one value per x label" }
    ChartFrame(title, series, modifier) {
        if (table) ChartTable(xLabels, series, format) else LinePlot(series, xLabels, description, format, height)
    }
}

@Composable
private fun LinePlot(series: List<ChartSeries>, xLabels: List<String>, description: String, format: (Float) -> String, height: Dp) {
    run {
        val c = Stretch.colors
        val measurer = rememberTextMeasurer()
        val ticksStyle = tickStyle()
        val labelStyle = Stretch.type.value.copy(color = c.ink)
        val values = series.flatMap { it.values }
        val ticks = remember(values) { niceTicks(values.min(), values.max()) }
        var hover by remember { mutableStateOf<Offset?>(null) }
        val density = LocalDensity.current
        BoxWithConstraints(Modifier.fillMaxWidth().height(height)) {
            val width = with(density) { maxWidth.toPx() }
            val plot = with(density) {
                Plot(
                    left = tickWidth(measurer, ticks, ticksStyle, format) + 6.dp.toPx(),
                    right = width - 30.dp.toPx(),
                    top = 8.dp.toPx(),
                    bottom = height.toPx() - 20.dp.toPx(),
                    lo = ticks.first(),
                    hi = ticks.last(),
                )
            }
            val count = xLabels.size
            fun x(i: Int) = if (count == 1) plot.left else plot.left + (plot.right - plot.left) * i / (count - 1)
            val hovered = hover?.let { h ->
                if (h.x < plot.left - 8 || h.x > plot.right + 8) null else (0 until count).minBy { abs(x(it) - h.x) }
            }
            Canvas(Modifier.matchParentSize().hoverPosition { hover = it }.semantics { contentDescription = description }) {
                gridAndTicks(plot, ticks, measurer, ticksStyle, c.chartGrid, c.chartAxis, format)
                val first = measurer.measure(xLabels.first(), ticksStyle)
                drawText(first, topLeft = Offset(plot.left, plot.bottom + 4.dp.toPx()))
                if (count > 1) {
                    val last = measurer.measure(xLabels.last(), ticksStyle)
                    drawText(last, topLeft = Offset(plot.right - last.size.width, plot.bottom + 4.dp.toPx()))
                }
                if (hovered != null) drawLine(c.chartAxis, Offset(x(hovered), plot.top), Offset(x(hovered), plot.bottom), 1.dp.toPx())
                for (s in series) {
                    val path = Path()
                    s.values.forEachIndexed { i, v -> if (i == 0) path.moveTo(x(i), plot.y(v)) else path.lineTo(x(i), plot.y(v)) }
                    drawPath(path, c.chart(s.slot), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
                val markAt = hovered ?: (count - 1)
                for (s in series) {
                    val at = Offset(x(markAt), plot.y(s.values[markAt]))
                    drawCircle(c.surfacePanel, 6.dp.toPx(), at)
                    drawCircle(c.chart(s.slot), 4.dp.toPx(), at)
                }
                val ends = series.map { plot.y(it.values.last()) }
                val texts = series.map { measurer.measure(format(it.values.last()), labelStyle) }
                val centers = spreadLabels(ends, texts.maxOf { it.size.height }.toFloat())
                texts.forEachIndexed { i, text ->
                    drawText(text, topLeft = Offset(x(count - 1) + 9.dp.toPx(), centers[i] - text.size.height / 2))
                }
            }
            if (hovered != null) {
                ChartTooltip(x(hovered), xLabels[hovered], series.map { Triple(c.chart(it.slot), it.name, format(it.values[hovered])) }, width)
            }
        }
    }
}

/**
 * A bar chart of one series over categories, on one axis from zero. Bars are at most 24dp wide with a rounded end;
 * each carries its value. Hover shows the bar's category and value. With [table] it shows as a [Table].
 */
@Composable
fun BarChart(
    title: String,
    bars: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
    slot: Int = 0,
    seriesName: String = title,
    description: String = title,
    table: Boolean = false,
    format: (Float) -> String = ::formatNumber,
    height: Dp = 170.dp,
) {
    val series = ChartSeries(seriesName, bars.map { it.second }, slot)
    ChartFrame(title, listOf(series), modifier) {
        if (table) ChartTable(bars.map { it.first }, listOf(series), format) else BarPlot(bars, slot, seriesName, description, format, height)
    }
}

@Composable
private fun BarPlot(bars: List<Pair<String, Float>>, slot: Int, seriesName: String, description: String, format: (Float) -> String, height: Dp) {
    run {
        val c = Stretch.colors
        val color = c.chart(slot)
        val measurer = rememberTextMeasurer()
        val ticksStyle = tickStyle()
        val labelStyle = Stretch.type.value.copy(color = c.ink)
        val ticks = remember(bars) { niceTicks(bars.minOf { it.second }, bars.maxOf { it.second }) }
        var hover by remember { mutableStateOf<Offset?>(null) }
        val density = LocalDensity.current
        BoxWithConstraints(Modifier.fillMaxWidth().height(height)) {
            val width = with(density) { maxWidth.toPx() }
            val plot = with(density) {
                Plot(
                    left = tickWidth(measurer, ticks, ticksStyle, format) + 6.dp.toPx(),
                    right = width,
                    top = 18.dp.toPx(),
                    bottom = height.toPx() - 20.dp.toPx(),
                    lo = ticks.first(),
                    hi = ticks.last(),
                )
            }
            val band = (plot.right - plot.left) / bars.size
            val barWidth = with(density) { minOf(24.dp.toPx(), band * 0.6f) }
            fun center(i: Int) = plot.left + band * (i + 0.5f)
            val hovered = hover?.let { h -> ((h.x - plot.left) / band).toInt().takeIf { h.x >= plot.left && it in bars.indices } }
            Canvas(Modifier.matchParentSize().hoverPosition { hover = it }.semantics { contentDescription = description }) {
                gridAndTicks(plot, ticks, measurer, ticksStyle, c.chartGrid, c.chartGrid, format)
                val radius = 4.dp.toPx()
                bars.forEachIndexed { i, (name, v) ->
                    val top = plot.y(v)
                    val base = plot.y(0f)
                    val left = center(i) - barWidth / 2
                    val path = Path().apply {
                        addRoundRect(
                            RoundRect(
                                left, minOf(top, base), left + barWidth, maxOf(top, base),
                                topLeftCornerRadius = if (v >= 0) CornerRadius(radius) else CornerRadius.Zero,
                                topRightCornerRadius = if (v >= 0) CornerRadius(radius) else CornerRadius.Zero,
                                bottomLeftCornerRadius = if (v < 0) CornerRadius(radius) else CornerRadius.Zero,
                                bottomRightCornerRadius = if (v < 0) CornerRadius(radius) else CornerRadius.Zero,
                            ),
                        )
                    }
                    drawPath(path, color, alpha = if (hovered == null || hovered == i) 1f else 0.55f)
                    val value = measurer.measure(format(v), labelStyle)
                    drawText(value, topLeft = Offset(center(i) - value.size.width / 2, minOf(top, base) - value.size.height - 2.dp.toPx()))
                    val label = measurer.measure(name, ticksStyle)
                    drawText(label, topLeft = Offset(center(i) - label.size.width / 2, plot.bottom + 4.dp.toPx()))
                }
                drawLine(c.chartAxis, Offset(plot.left, plot.y(0f)), Offset(plot.right, plot.y(0f)), 1.dp.toPx())
            }
            if (hovered != null) {
                ChartTooltip(center(hovered), bars[hovered].first, listOf(Triple(color, seriesName, format(bars[hovered].second))), width)
            }
        }
    }
}

/** A chart's data as a table: one row per x label, one numeric column per series. */
@Composable
fun ChartTable(xLabels: List<String>, series: List<ChartSeries>, format: (Float) -> String = ::formatNumber, modifier: Modifier = Modifier) {
    val columns = listOf(TableColumn<Int>("", sortBy = { it }) { xLabels[it] }) +
        series.map { s -> TableColumn<Int>(s.name, numeric = true, sortBy = { s.values[it] }) { format(s.values[it]) } }
    var sort by remember { mutableStateOf<TableSort?>(null) }
    Table(xLabels.indices.toList(), columns, modifier.fillMaxWidth(), sort = sort, onSortChange = { sort = it }, dense = true)
}
