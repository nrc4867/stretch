package dev.chieppa.stretch.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// Ramp

/** One stop of a shading ramp: where its band starts (0 to 1) and its color. */
@Immutable
data class RampStop(val position: Float, val color: Color)

/** The stop whose band covers [t]: the last stop at or before it. Stops must be sorted. */
fun bandAt(stops: List<RampStop>, t: Float): Int = stops.indexOfLast { it.position <= t }.coerceAtLeast(0)

/** [stops] with stop [index] moved to [position], kept between its neighbors. The first stop stays at 0. */
fun List<RampStop>.movedStop(index: Int, position: Float): List<RampStop> {
    if (index == 0) return this
    val low = this[index - 1].position + 0.01f
    val high = getOrNull(index + 1)?.position?.minus(0.01f) ?: 1f
    return mapIndexed { i, s -> if (i == index) s.copy(position = position.coerceIn(low, high)) else s }
}

/**
 * A shading ramp: bands of flat color, one per stop, from shadow to light. This is the cel-shading control: each
 * band is a tone, each stop the threshold where the next begins. Drag a stop's handle to move the threshold, click a
 * handle to pick it (the caller edits its color, usually in a popover [ColorPicker]), double-click the bar to add a
 * stop and press Delete to remove the picked one. Left and Right nudge it (Shift for bigger steps). With [smooth] the
 * bands blend, for a gradient editor.
 */
@Composable
fun RampEditor(
    stops: List<RampStop>,
    onStopsChange: (List<RampStop>) -> Unit,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    smooth: Boolean = false,
    onChangeFinished: () -> Unit = {},
    description: String = "Shading ramp",
) {
    val c = Stretch.colors
    val ctlRadius = Stretch.shapes.ctl
    val strokeWidth = Stretch.stroke
    val current by rememberUpdatedState(stops)
    val change by rememberUpdatedState(onStopsChange)
    val pick by rememberUpdatedState(onSelect)
    val finished by rememberUpdatedState(onChangeFinished)
    val picked by rememberUpdatedState(selected)
    val state = rememberControlState()
    val focus = remember { FocusRequester() }
    var lastTap by remember { mutableLongStateOf(0L) }
    val shape = RoundedCornerShape(Stretch.shapes.ctl)
    Canvas(
        modifier
            .widthIn(min = 200.dp)
            .fillMaxWidth()
            .height(48.dp)
            .focusRing(state, shape)
            .focusRequester(focus)
            .focusable(interactionSource = state.source)
            .semantics {
                contentDescription = description
                stateDescription = stops.joinToString { "${(it.position * 100).roundToInt()}% ${it.color.toHex()}" }
            }
            .onKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                val step = if (e.isShiftPressed) 0.05f else 0.01f
                when (e.key) {
                    Key.DirectionLeft -> change(current.movedStop(picked, current[picked].position - step))
                    Key.DirectionRight -> change(current.movedStop(picked, current[picked].position + step))
                    Key.Delete, Key.Backspace -> if (current.size > 2 && picked > 0) {
                        change(current.filterIndexed { i, _ -> i != picked })
                        pick(picked - 1)
                    }
                    Key.Tab -> return@onKeyEvent false
                    else -> return@onKeyEvent false
                }
                finished()
                true
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    focus.requestFocus()
                    val barBottom = size.height - 16.dp.toPx()
                    val t = (down.position.x / size.width).coerceIn(0f, 1f)
                    val nearest = current.indices.minBy { abs(current[it].position * size.width - down.position.x) }
                    val onHandle = down.position.y > barBottom - 4.dp.toPx() && abs(current[nearest].position * size.width - down.position.x) < 10.dp.toPx()
                    if (onHandle) {
                        pick(nearest)
                        var moved = false
                        drag(down.id) { ch ->
                            ch.consume()
                            moved = true
                            change(current.movedStop(nearest, (ch.position.x / size.width).coerceIn(0f, 1f)))
                        }
                        if (moved) finished()
                    } else {
                        val now = down.uptimeMillis
                        if (now - lastTap < viewConfiguration.doubleTapTimeoutMillis) {
                            val band = bandAt(current, t)
                            val next = (current + RampStop(t, current[band].color)).sortedBy { it.position }
                            change(next)
                            pick(next.indexOfFirst { it.position == t })
                            finished()
                        } else {
                            pick(bandAt(current, t))
                        }
                        lastTap = now
                    }
                }
            },
    ) {
        val bar = size.height - 16.dp.toPx()
        val r = ctlRadius.toPx()
        if (smooth) {
            drawRoundRect(Brush.horizontalGradient(*stops.map { it.position to it.color }.toTypedArray(), startX = 0f, endX = size.width), size = Size(size.width, bar), cornerRadius = androidx.compose.ui.geometry.CornerRadius(r))
        } else {
            stops.forEachIndexed { i, s ->
                val x0 = s.position * size.width
                val x1 = (stops.getOrNull(i + 1)?.position ?: 1f) * size.width
                drawRect(s.color, Offset(x0, 0f), Size(x1 - x0, bar))
            }
        }
        drawRoundRect(c.lineControl, size = Size(size.width, bar), cornerRadius = androidx.compose.ui.geometry.CornerRadius(r), style = Stroke(strokeWidth.toPx()))
        stops.forEachIndexed { i, s ->
            val x = s.position * size.width
            val tip = bar + 2.dp.toPx()
            val handle = Path().apply {
                moveTo(x, tip)
                lineTo(x + 6.dp.toPx(), tip + 8.dp.toPx())
                lineTo(x + 6.dp.toPx(), size.height - 1.dp.toPx())
                lineTo(x - 6.dp.toPx(), size.height - 1.dp.toPx())
                lineTo(x - 6.dp.toPx(), tip + 8.dp.toPx())
                close()
            }
            drawPath(handle, s.color)
            drawPath(handle, if (i == selected) c.ringSelected else c.lineStrong, style = Stroke(if (i == selected) 2.dp.toPx() else strokeWidth.toPx(), join = StrokeJoin.Round))
        }
    }
}

// Curve

/**
 * The value of the curve through [points] at [x], all in 0 to 1. Points are sorted by x and include both ends; the
 * curve is a monotone cubic, so it never overshoots between points (a pressure curve stays in range).
 */
fun evaluateCurve(points: List<Offset>, x: Float): Float {
    if (points.size < 2) return x
    val n = points.size
    val dx = FloatArray(n - 1) { points[it + 1].x - points[it].x }
    val m = FloatArray(n - 1) { if (dx[it] == 0f) 0f else (points[it + 1].y - points[it].y) / dx[it] }
    val t = FloatArray(n) { i ->
        when (i) {
            0 -> m[0]
            n - 1 -> m[n - 2]
            else -> if (m[i - 1] * m[i] <= 0f) 0f else 3 * (dx[i - 1] + dx[i]) / ((2 * dx[i] + dx[i - 1]) / m[i - 1] + (dx[i] + 2 * dx[i - 1]) / m[i])
        }
    }
    val k = points.indexOfLast { it.x <= x }.coerceIn(0, n - 2)
    val h = dx[k].takeIf { it > 0f } ?: return points[k].y
    val s = ((x - points[k].x) / h).coerceIn(0f, 1f)
    val s2 = s * s
    val s3 = s2 * s
    return ((2 * s3 - 3 * s2 + 1) * points[k].y + (s3 - 2 * s2 + s) * h * t[k] + (-2 * s3 + 3 * s2) * points[k + 1].y + (s3 - s2) * h * t[k + 1]).coerceIn(0f, 1f)
}

/**
 * A curve editor for pressure, easing and taper: a square with the curve through its points. Drag a point to move it
 * (the end points move only up and down), click empty space to add one, double-click a point or press Delete to
 * remove the picked one. Arrow keys nudge the picked point.
 */
@Composable
fun CurveEditor(
    points: List<Offset>,
    onPointsChange: (List<Offset>) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    onChangeFinished: () -> Unit = {},
    description: String = "Curve",
) {
    val c = Stretch.colors
    val ctlRadius = Stretch.shapes.ctl
    val strokeWidth = Stretch.stroke
    val current by rememberUpdatedState(points)
    val change by rememberUpdatedState(onPointsChange)
    val finished by rememberUpdatedState(onChangeFinished)
    var picked by remember { mutableIntStateOf(-1) }
    var lastTap by remember { mutableLongStateOf(0L) }
    val state = rememberControlState()
    val focus = remember { FocusRequester() }
    val shape = RoundedCornerShape(Stretch.shapes.ctl)
    fun moved(list: List<Offset>, i: Int, p: Offset): List<Offset> {
        val low = if (i == 0) 0f else list[i - 1].x + 0.01f
        val high = if (i == list.lastIndex) 1f else list[i + 1].x - 0.01f
        val x = if (i == 0) 0f else if (i == list.lastIndex) 1f else p.x.coerceIn(low, high)
        return list.mapIndexed { j, o -> if (j == i) Offset(x, p.y.coerceIn(0f, 1f)) else o }
    }
    Canvas(
        modifier
            .size(size)
            .focusRing(state, shape)
            .clip(shape)
            .border(Stretch.stroke, c.lineControl, shape)
            .focusRequester(focus)
            .focusable(interactionSource = state.source)
            .semantics {
                contentDescription = description
                stateDescription = points.joinToString { "${(it.x * 100).roundToInt()}, ${(it.y * 100).roundToInt()}" }
            }
            .onKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown || picked !in current.indices) return@onKeyEvent false
                val step = if (e.isShiftPressed) 0.05f else 0.01f
                val p = current[picked]
                when (e.key) {
                    Key.DirectionLeft -> change(moved(current, picked, p.copy(x = p.x - step)))
                    Key.DirectionRight -> change(moved(current, picked, p.copy(x = p.x + step)))
                    Key.DirectionUp -> change(moved(current, picked, p.copy(y = p.y + step)))
                    Key.DirectionDown -> change(moved(current, picked, p.copy(y = p.y - step)))
                    Key.Delete, Key.Backspace -> if (picked in 1 until current.lastIndex) {
                        change(current.filterIndexed { i, _ -> i != picked })
                        picked = -1
                    }
                    else -> return@onKeyEvent false
                }
                finished()
                true
            }
            .pointerInput(Unit) {
                fun unit(p: Offset) = Offset((p.x / this.size.width).coerceIn(0f, 1f), (1 - p.y / this.size.height).coerceIn(0f, 1f))
                awaitEachGesture {
                    val down = awaitFirstDown()
                    focus.requestFocus()
                    val hit = current.indices.firstOrNull { i ->
                        val q = Offset(current[i].x * this.size.width, (1 - current[i].y) * this.size.height)
                        (q - down.position).getDistance() < 10.dp.toPx()
                    }
                    val now = down.uptimeMillis
                    if (hit != null && hit == picked && now - lastTap < viewConfiguration.doubleTapTimeoutMillis && hit in 1 until current.lastIndex) {
                        change(current.filterIndexed { i, _ -> i != hit })
                        picked = -1
                        finished()
                        return@awaitEachGesture
                    }
                    lastTap = now
                    val index = hit ?: run {
                        val p = unit(down.position)
                        val next = (current + p).sortedBy { it.x }
                        change(next)
                        next.indexOf(p)
                    }
                    picked = index
                    drag(down.id) { ch ->
                        ch.consume()
                        change(moved(current, index, unit(ch.position)))
                    }
                    finished()
                }
            },
    ) {
        val w = this.size.width
        val h = this.size.height
        for (i in 1..3) {
            drawLine(c.chartGrid, Offset(w * i / 4, 0f), Offset(w * i / 4, h), 1f)
            drawLine(c.chartGrid, Offset(0f, h * i / 4), Offset(w, h * i / 4), 1f)
        }
        drawLine(c.chartAxis, Offset(0f, h), Offset(w, 0f), 1f)
        val path = Path()
        val steps = 64
        for (i in 0..steps) {
            val x = i / steps.toFloat()
            val y = evaluateCurve(points, x)
            if (i == 0) path.moveTo(x * w, (1 - y) * h) else path.lineTo(x * w, (1 - y) * h)
        }
        drawPath(path, c.inkStrong, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        points.forEachIndexed { i, p ->
            val q = Offset(p.x * w, (1 - p.y) * h)
            drawCircle(c.surfaceFloat, 5.dp.toPx(), q)
            drawCircle(if (i == picked) c.ringSelected else c.inkStrong, 5.dp.toPx(), q, style = Stroke(if (i == picked) 2.5.dp.toPx() else 1.5.dp.toPx()))
        }
    }
}

// Angle

/** The angle in degrees (0 to 360, 0 pointing right, counterclockwise) of [point] around [center]. */
fun angleOf(point: Offset, center: Offset): Float {
    val a = atan2(-(point.y - center.y), point.x - center.x) * 180f / PI.toFloat()
    return (a + 360f) % 360f
}

/** [degrees] rounded to the nearest [step]. */
fun snapAngle(degrees: Float, step: Float): Float = ((degrees / step).roundToInt() * step) % 360f

/**
 * An angle dial for rotation and light direction: drag around it to turn (Shift snaps to 15°), or use the arrow keys
 * (Shift for 15°). Pair it with a [NumberField] for typing an exact angle.
 */
@Composable
fun AngleDial(
    degrees: Float,
    onDegreesChange: (Float) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    onChangeFinished: () -> Unit = {},
) {
    val c = Stretch.colors
    val ctlRadius = Stretch.shapes.ctl
    val strokeWidth = Stretch.stroke
    val change by rememberUpdatedState(onDegreesChange)
    val finished by rememberUpdatedState(onChangeFinished)
    val current by rememberUpdatedState(degrees)
    val state = rememberControlState()
    val focus = remember { FocusRequester() }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(
            Modifier
                .size(size)
                .focusRing(state, CircleShape)
                .focusRequester(focus)
            .focusable(interactionSource = state.source)
                .semantics {
                    this.contentDescription = contentDescription
                    stateDescription = "${formatNumber(degrees)}°"
                }
                .onKeyEvent { e ->
                    if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                    val step = if (e.isShiftPressed) 15f else 1f
                    val next = when (e.key) {
                        Key.DirectionUp, Key.DirectionLeft -> current + step
                        Key.DirectionDown, Key.DirectionRight -> current - step
                        else -> return@onKeyEvent false
                    }
                    change(((next % 360f) + 360f) % 360f)
                    finished()
                    true
                }
                .pointerInput(Unit) {
                    val center = Offset(this.size.width / 2f, this.size.height / 2f)
                    awaitEachGesture {
                        val down = awaitFirstDown()
                    focus.requestFocus()
                        fun set(p: Offset, snap: Boolean) {
                            val a = angleOf(p, center)
                            change(if (snap) snapAngle(a, 15f) else a)
                        }
                        set(down.position, currentEvent.keyboardModifiers.isShiftPressed)
                        drag(down.id) { ch ->
                            ch.consume()
                            set(ch.position, currentEvent.keyboardModifiers.isShiftPressed)
                        }
                        finished()
                    }
                },
        ) {
            val r = this.size.minDimension / 2 - 2.dp.toPx()
            val center = this.center
            drawCircle(c.surfaceFloat, r, center)
            drawCircle(c.lineControl, r, center, style = Stroke(strokeWidth.toPx()))
            for (i in 0 until 8) {
                val a = i * PI / 4
                val inner = if (i % 2 == 0) r - 6.dp.toPx() else r - 4.dp.toPx()
                drawLine(c.inkSubtle, center + Offset((cos(a) * inner).toFloat(), -(sin(a) * inner).toFloat()), center + Offset((cos(a) * r).toFloat(), -(sin(a) * r).toFloat()), 1.dp.toPx())
            }
            val rad = degrees * PI / 180
            val tip = center + Offset((cos(rad) * (r - 3.dp.toPx())).toFloat(), -(sin(rad) * (r - 3.dp.toPx())).toFloat())
            drawLine(c.inkStrong, center, tip, 2.dp.toPx(), StrokeCap.Round)
            drawCircle(c.accent, 4.dp.toPx(), tip)
            drawCircle(c.inkStrong, 4.dp.toPx(), tip, style = Stroke(1.dp.toPx()))
            drawCircle(c.inkStrong, 2.dp.toPx(), center)
        }
    }
}
