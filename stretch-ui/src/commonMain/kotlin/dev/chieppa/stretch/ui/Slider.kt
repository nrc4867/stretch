package dev.chieppa.stretch.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchSizes

/**
 * A soft slider: a thin track, an accent fill up to the value and a round knob. [onValueChange] fires live while
 * dragging; [onValueChangeFinished] fires once at the end, which is where a gesture's undo step is committed.
 * Pair it with a readout so the exact value is visible.
 */
@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: () -> Unit = {},
) {
    val c = Stretch.colors
    val state = rememberControlState()
    val stroke = Stretch.stroke
    val span = valueRange.endInclusive - valueRange.start
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)
    val change by rememberUpdatedState(onValueChange)
    val finished by rememberUpdatedState(onValueChangeFinished)
    val knobRadius = 8.dp
    val rtl = isRtl()

    fun valueAt(x: Float, width: Float, inset: Float): Float {
        val f = ((x - inset) / (width - 2 * inset)).coerceIn(0f, 1f)
        return valueRange.start + f * span
    }

    Canvas(
        modifier = modifier
            .widthIn(min = 96.dp)
            .fillMaxWidth()
            .height(StretchSizes.chip)
            .mirrored(rtl)
            .focusRing(state, Stretch.shapes.pillShape)
            .hoverable(state.source)
            .focusable(interactionSource = state.source)
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val step = span / if (event.isShiftPressed) 10f else 100f
                val next = when (along(event.key, rtl)) {
                    Key.DirectionRight, Key.DirectionUp -> value + step
                    Key.DirectionLeft, Key.DirectionDown -> value - step
                    Key.MoveHome -> valueRange.start
                    Key.MoveEnd -> valueRange.endInclusive
                    else -> return@onKeyEvent false
                }
                change(next.coerceIn(valueRange))
                finished()
                true
            }
            .pointerInput(valueRange) {
                val inset = knobRadius.toPx()
                detectTapGestures { offset ->
                    change(valueAt(offset.x, size.width.toFloat(), inset))
                    finished()
                }
            }
            .pointerInput(valueRange) {
                val inset = knobRadius.toPx()
                detectHorizontalDragGestures(
                    onDragStart = { change(valueAt(it.x, size.width.toFloat(), inset)) },
                    onDragEnd = { finished() },
                    onDragCancel = { finished() },
                ) { input, _ -> change(valueAt(input.position.x, size.width.toFloat(), inset)) }
            }
            .semantics {
                this.contentDescription = contentDescription
                progressBarRangeInfo = ProgressBarRangeInfo(value, valueRange)
                setProgress { target ->
                    change(target.coerceIn(valueRange))
                    finished()
                    true
                }
            },
    ) {
        val inset = knobRadius.toPx()
        val trackHeight = 8.dp.toPx()
        val top = (size.height - trackHeight) / 2
        val radius = CornerRadius(trackHeight / 2)
        drawRoundRect(c.track, Offset(0f, top), Size(size.width, trackHeight), radius)
        drawRoundRect(c.trackEdge, Offset(0.5f, top + 0.5f), Size(size.width - 1f, trackHeight - 1f), radius, style = Stroke(1f))
        val x = inset + fraction * (size.width - 2 * inset)
        drawRoundRect(c.accent, Offset(0f, top), Size(x, trackHeight), radius)
        val edge = stroke.toPx()
        drawCircle(c.knob, inset, Offset(x, size.height / 2))
        drawCircle(c.knobEdge, inset - edge / 2, Offset(x, size.height / 2), style = Stroke(edge))
    }
}
