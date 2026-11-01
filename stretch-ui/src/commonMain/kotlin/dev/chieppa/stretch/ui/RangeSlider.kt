package dev.chieppa.stretch.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchSizes
import kotlin.math.abs
import kotlin.math.roundToInt

private val KnobRadius = 8.dp

/**
 * A slider with two knobs that picks a span, like a taper's start and end. The accent fills between them, and the
 * knobs never cross. Each knob is its own control: it takes focus, steps with the arrow keys (Shift steps ten) and
 * is announced with [startDescription] or [endDescription]. Dragging or clicking the track moves the nearer knob.
 * [onValueChange] fires live; [onValueChangeFinished] once per gesture, where its undo step is committed.
 */
@Composable
fun RangeSlider(
    value: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    startDescription: String,
    endDescription: String,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: () -> Unit = {},
) {
    val c = Stretch.colors
    val span = valueRange.endInclusive - valueRange.start
    val current by rememberUpdatedState(value)
    val change by rememberUpdatedState(onValueChange)
    val finished by rememberUpdatedState(onValueChangeFinished)
    var dragging by remember { mutableStateOf<Boolean?>(null) }

    fun fraction(v: Float) = ((v - valueRange.start) / span).coerceIn(0f, 1f)

    /** Moves the start knob ([start] true) or the end knob to [to], keeping start at or below end. */
    fun move(start: Boolean, to: Float) {
        val now = current
        val next = if (start) to.coerceIn(valueRange.start, now.endInclusive)..now.endInclusive
        else now.start..to.coerceIn(now.start, valueRange.endInclusive)
        change(next)
    }

    val inset = with(LocalDensity.current) { KnobRadius.toPx() }
    var width by remember { mutableStateOf(1f) }
    val usable = (width - 2 * inset).coerceAtLeast(1f)
    fun valueAt(x: Float) = valueRange.start + ((x - inset) / usable).coerceIn(0f, 1f) * span
    fun nearerIsStart(x: Float): Boolean {
        val v = valueAt(x)
        val toStart = abs(v - current.start)
        val toEnd = abs(v - current.endInclusive)
        return if (toStart == toEnd) v <= current.start else toStart < toEnd
    }

    val rtl = isRtl()
    // Laid out left to right and flipped whole, so knobs, fill and pointer agree.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier
                .widthIn(min = 96.dp)
                .fillMaxWidth()
                .height(StretchSizes.chip)
                .mirrored(rtl)
                .onSizeChanged { width = it.width.toFloat() }
                .pointerInput(valueRange, usable) {
                    detectTapGestures { at ->
                        move(nearerIsStart(at.x), valueAt(at.x))
                        finished()
                    }
                }
                .pointerInput(valueRange, usable) {
                    detectHorizontalDragGestures(
                        onDragStart = { at -> dragging = nearerIsStart(at.x).also { move(it, valueAt(at.x)) } },
                        onDragEnd = { dragging = null; finished() },
                        onDragCancel = { dragging = null; finished() },
                    ) { input, _ -> dragging?.let { move(it, valueAt(input.position.x)) } }
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val trackHeight = 8.dp.toPx()
                val top = (size.height - trackHeight) / 2
                val radius = CornerRadius(trackHeight / 2)
                drawRoundRect(c.track, Offset(0f, top), Size(size.width, trackHeight), radius)
                drawRoundRect(c.trackEdge, Offset(0.5f, top + 0.5f), Size(size.width - 1f, trackHeight - 1f), radius, style = Stroke(1f))
                val a = inset + fraction(value.start) * usable
                val b = inset + fraction(value.endInclusive) * usable
                drawRoundRect(c.accent, Offset(a, top), Size(b - a, trackHeight), radius)
            }
            Knob(inset + fraction(value.start) * usable - inset, startDescription, value.start, valueRange.start..value.endInclusive, span, rtl) {
                move(true, it)
                finished()
            }
            Knob(inset + fraction(value.endInclusive) * usable - inset, endDescription, value.endInclusive, value.start..valueRange.endInclusive, span, rtl) {
                move(false, it)
                finished()
            }
        }
    }
}

/** One knob of a [RangeSlider], [x] px from the left; [allowed] is where it may go without crossing the other. */
@Composable
private fun BoxScope.Knob(x: Float, description: String, value: Float, allowed: ClosedFloatingPointRange<Float>, span: Float, rtl: Boolean, onSet: (Float) -> Unit) {
    val c = Stretch.colors
    val state = rememberControlState()
    val edge = Stretch.stroke
    Box(
        Modifier
            .offset { IntOffset(x.roundToInt(), 0) }
            .size(KnobRadius * 2)
            .align(Alignment.CenterStart)
            .focusRing(state, CircleShape)
            .hoverable(state.source)
            .focusable(interactionSource = state.source)
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val step = span / if (event.isShiftPressed) 10f else 100f
                val next = when (along(event.key, rtl)) {
                    Key.DirectionRight, Key.DirectionUp -> value + step
                    Key.DirectionLeft, Key.DirectionDown -> value - step
                    Key.MoveHome -> allowed.start
                    Key.MoveEnd -> allowed.endInclusive
                    else -> return@onKeyEvent false
                }
                onSet(next.coerceIn(allowed))
                true
            }
            .semantics {
                contentDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo(value, allowed)
                setProgress { target -> onSet(target.coerceIn(allowed)); true }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2
            drawCircle(c.knob, r)
            drawCircle(c.knobEdge, r - edge.toPx() / 2, style = Stroke(edge.toPx()))
        }
    }
}
