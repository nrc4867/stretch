package dev.chieppa.stretch.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchSpacing

/**
 * A mark on an [OverviewRuler] from [start] until [end], in the ruler's units, drawn in [color]. [across] is the part of
 * the ruler's width it fills, from 0 at the start edge to 1 at the end edge, so kinds of mark can keep to lanes.
 */
@Immutable
data class RulerMark(val start: Float, val end: Float, val color: Color, val across: ClosedFloatingPointRange<Float> = RulerLanes.Whole)

/** Parts of an [OverviewRuler]'s width that kinds of mark keep to, for [RulerMark.across]. */
object RulerLanes {
    val Whole: ClosedFloatingPointRange<Float> = 0f..1f
    val Start: ClosedFloatingPointRange<Float> = 0f..0.5f
    val End: ClosedFloatingPointRange<Float> = 0.5f..1f
    val Middle: ClosedFloatingPointRange<Float> = 0.25f..0.75f
}

/** The shortest a mark draws, so a single line in a long view still shows. */
private val MinMarkHeight = 2.dp

/**
 * A thin strip beside a scrolled view, showing where [marks] lie along the view's whole [length], in units the view
 * chooses, such as rows of text. [shown] gives the part on screen, drawn as a band behind the marks; it is read while
 * drawing, so scrolling redraws the strip without composing it again. Pressing or dragging on the strip calls [onJump]
 * with the position under the pointer, in the same units.
 */
@Composable
fun OverviewRuler(
    marks: List<RulerMark>,
    length: Float,
    onJump: (position: Float) -> Unit,
    modifier: Modifier = Modifier,
    shown: (() -> ClosedFloatingPointRange<Float>?)? = null,
) {
    val c = Stretch.colors
    val jump by rememberUpdatedState(onJump)
    val total by rememberUpdatedState(length)
    Canvas(
        modifier
            .width(StretchSpacing.space10)
            .fillMaxHeight()
            .background(c.surfaceCanvas)
            .pointerInput(Unit) {
                fun at(y: Float) = (y / size.height).coerceIn(0f, 1f) * total
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    jump(at(down.position.y))
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        change.consume()
                        jump(at(change.position.y))
                    }
                }
            },
    ) {
        if (length <= 0f) return@Canvas
        val scale = size.height / length
        shown?.invoke()?.let { band ->
            drawRect(c.surfaceControlHi, Offset(0f, band.start * scale), Size(size.width, (band.endInclusive - band.start) * scale))
        }
        val least = MinMarkHeight.toPx()
        for (mark in marks) {
            val left = mark.across.start * size.width
            drawRect(
                mark.color,
                Offset(left, mark.start * scale),
                Size((mark.across.endInclusive - mark.across.start) * size.width, maxOf((mark.end - mark.start) * scale, least)),
            )
        }
    }
}
