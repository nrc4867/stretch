package dev.chieppa.stretch.frame

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color

/** A straight line in a [ViewportOverlay], [width] pixels wide. */
@Immutable
data class OverlayLine(val from: Offset, val to: Offset, val color: Color, val width: Float = 1f)

/** A filled rectangle in a [ViewportOverlay]. */
@Immutable
data class OverlayRect(val rect: Rect, val color: Color)

/** A filled circle in a [ViewportOverlay], such as a handle. */
@Immutable
data class OverlayDot(val center: Offset, val radius: Float, val color: Color)

/**
 * What a renderer draws over its scene in a native viewport surface, where Compose cannot draw: gizmos, selection
 * handles, a marquee, guides and previews. Positions are in the surface's pixels from its top-left corner. A renderer
 * draws it last, rects first, then lines, then dots, each list in order.
 */
@Immutable
data class ViewportOverlay(
    val rects: List<OverlayRect> = emptyList(),
    val lines: List<OverlayLine> = emptyList(),
    val dots: List<OverlayDot> = emptyList(),
) {
    val isEmpty: Boolean get() = rects.isEmpty() && lines.isEmpty() && dots.isEmpty()

    /** Both overlays in one; within each kind of part, [other]'s draw after this one's. */
    operator fun plus(other: ViewportOverlay): ViewportOverlay =
        if (other.isEmpty) this else if (isEmpty) other else ViewportOverlay(rects + other.rects, lines + other.lines, dots + other.dots)

    companion object {
        val Empty = ViewportOverlay()
    }
}

/** Collects the parts of a [ViewportOverlay]; see [buildOverlay]. */
class OverlayBuilder internal constructor() {
    private val rects = mutableListOf<OverlayRect>()
    private val lines = mutableListOf<OverlayLine>()
    private val dots = mutableListOf<OverlayDot>()

    fun rect(rect: Rect, color: Color) {
        rects += OverlayRect(rect, color)
    }

    fun line(from: Offset, to: Offset, color: Color, width: Float = 1f) {
        lines += OverlayLine(from, to, color, width)
    }

    /** Lines along [path], [dash] long and [gap] apart, with the pattern running on around its corners. */
    fun dashed(path: List<Offset>, color: Color, width: Float, dash: Float, gap: Float) {
        require(dash > 0f && gap >= 0f) { "A dash has length" }
        val period = dash + gap
        var walked = 0f
        for ((from, to) in path.zipWithNext()) {
            val length = (to - from).getDistance()
            if (length == 0f) continue
            val along = (to - from) / length
            var at = 0f
            while (at < length) {
                val phase = (walked + at) % period
                val step = minOf(length - at, if (phase < dash) dash - phase else period - phase)
                if (phase < dash) line(from + along * at, from + along * (at + step), color, width)
                at += step
            }
            walked += length
        }
    }

    fun dot(center: Offset, radius: Float, color: Color) {
        dots += OverlayDot(center, radius, color)
    }

    /** The four edges of [rect] as lines. */
    fun outline(rect: Rect, color: Color, width: Float = 1f) {
        line(rect.topLeft, rect.topRight, color, width)
        line(rect.topRight, rect.bottomRight, color, width)
        line(rect.bottomRight, rect.bottomLeft, color, width)
        line(rect.bottomLeft, rect.topLeft, color, width)
    }

    internal fun build() = ViewportOverlay(rects.toList(), lines.toList(), dots.toList())
}

fun buildOverlay(block: OverlayBuilder.() -> Unit): ViewportOverlay = OverlayBuilder().apply(block).build()
