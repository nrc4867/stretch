package dev.chieppa.stretch.gpu

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import dev.chieppa.stretch.frame.ViewportOverlay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * A [ViewportOverlay] as a list of triangles: each vertex is [FLOATS_PER_VERTEX] floats, its x and y in the surface's
 * pixels and its red, green, blue and alpha. Lines become quads and dots become fans, drawn in the overlay's order.
 */
object OverlayGeometry {
    const val FLOATS_PER_VERTEX = 6

    /** Segments in a dot's fan, at least, more for big dots so they stay round. */
    private const val MIN_DOT_SEGMENTS = 12

    fun triangles(overlay: ViewportOverlay): FloatArray {
        val out = FloatList()
        overlay.rects.forEach { (rect, color) ->
            out.quad(rect.topLeft, rect.topRight, rect.bottomRight, rect.bottomLeft, color)
        }
        overlay.lines.forEach { (from, to, color, width) ->
            val along = to - from
            val length = along.getDistance()
            if (length == 0f) return@forEach
            val side = Offset(-along.y, along.x) * (width / 2f / length)
            out.quad(from + side, to + side, to - side, from - side, color)
        }
        overlay.dots.forEach { (center, radius, color) ->
            val segments = max(MIN_DOT_SEGMENTS, radius.toInt())
            for (i in 0 until segments) {
                val a = 2 * PI * i / segments
                val b = 2 * PI * (i + 1) / segments
                out.vertex(center, color)
                out.vertex(center + Offset(cos(a).toFloat(), sin(a).toFloat()) * radius, color)
                out.vertex(center + Offset(cos(b).toFloat(), sin(b).toFloat()) * radius, color)
            }
        }
        return out.toArray()
    }

    private fun FloatList.quad(a: Offset, b: Offset, c: Offset, d: Offset, color: Color) {
        vertex(a, color)
        vertex(b, color)
        vertex(c, color)
        vertex(a, color)
        vertex(c, color)
        vertex(d, color)
    }

    private fun FloatList.vertex(at: Offset, color: Color) {
        add(at.x)
        add(at.y)
        add(color.red)
        add(color.green)
        add(color.blue)
        add(color.alpha)
    }

    /** A growable list of floats without boxing. */
    private class FloatList {
        private var values = FloatArray(256)
        private var size = 0

        fun add(value: Float) {
            if (size == values.size) values = values.copyOf(size * 2)
            values[size++] = value
        }

        fun toArray(): FloatArray = values.copyOf(size)
    }
}
