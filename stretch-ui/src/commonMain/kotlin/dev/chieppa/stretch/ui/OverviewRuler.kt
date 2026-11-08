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

/**
 * Where an [OverviewRuler]'s units lie when the whole view fits on screen with room to spare: [at] gives the distance
 * in pixels from the ruler's top of a position in the ruler's units, and [end] where the content ends. The ruler then
 * puts each mark beside its line, rather than spreading the content over its whole height.
 */
interface RulerFit {
    val end: Float
    fun at(position: Float): Float
}

/** The shortest a mark draws, so a single line in a long view still shows. */
private val MinMarkHeight = 2.dp

/**
 * A thin strip beside a scrolled view, showing where [marks] lie along the view's whole [length], in units the view
 * chooses, such as rows of text. [shown] gives the part on screen, drawn as a band behind the marks; it is read while
 * drawing, so scrolling redraws the strip without composing it again. Pressing or dragging on the strip calls [onJump]
 * with the position under the pointer, in the same units. While [fit] gives a [RulerFit], the view's content is shorter
 * than the view and all on screen, so the marks sit beside their lines, the band covers the content and pressing does
 * nothing; the ruler must then be as tall as the view and level with it.
 */
@Composable
fun OverviewRuler(
    marks: List<RulerMark>,
    length: Float,
    onJump: (position: Float) -> Unit,
    modifier: Modifier = Modifier,
    shown: (() -> ClosedFloatingPointRange<Float>?)? = null,
    fit: (() -> RulerFit?)? = null,
) {
    val c = Stretch.colors
    val jump by rememberUpdatedState(onJump)
    val total by rememberUpdatedState(length)
    val fits by rememberUpdatedState(fit)
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
                    if (fits?.invoke() != null) return@awaitEachGesture
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
        val placed = fit?.invoke()
        val scale = size.height / length
        fun y(position: Float) = placed?.at(position) ?: (position * scale)
        if (placed != null) {
            drawRect(c.surfaceControlHi, Offset.Zero, Size(size.width, placed.end))
        } else {
            shown?.invoke()?.let { band -> drawRect(c.surfaceControlHi, Offset(0f, y(band.start)), Size(size.width, y(band.endInclusive) - y(band.start))) }
        }
        val least = MinMarkHeight.toPx()
        for (mark in marks) {
            val left = mark.across.start * size.width
            val top = y(mark.start)
            drawRect(
                mark.color,
                Offset(left, top),
                Size((mark.across.endInclusive - mark.across.start) * size.width, maxOf(y(mark.end) - top, least)),
            )
        }
    }
}
