package dev.chieppa.stretch.frame

import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize

/** The viewport's corner slots, in the order [placeCorners] takes their sizes and returns their places. */
internal enum class CornerSlot { TopStart, TopEnd, BottomStart, BottomEnd, BottomCenter }

/**
 * How far in from each edge of the viewport its corner UI sits, and the [gap] between two pieces that would meet,
 * in pixels. [start] and [end] follow the reading direction.
 */
internal data class CornerMargins(val top: Int, val start: Int, val end: Int, val bottom: Int, val gap: Int)

/** The room a corner slot can take in a [width] by [height] viewport. */
internal fun cornerRoom(width: Int, height: Int, margins: CornerMargins): IntSize = IntSize(
    (width - margins.start - margins.end).coerceAtLeast(0),
    (height - margins.top - margins.bottom).coerceAtLeast(0),
)

/**
 * Where each corner slot goes in a [width] by [height] viewport, given its measured size (null for an empty slot):
 * a rectangle in the viewport's pixels from its left edge, or null for an empty slot. When a pair does not fit side
 * by side, the end one moves under (or above) the start one; the bottom centre rises above any bottom corner it
 * would touch. [ltr] says which side the start is on.
 */
internal fun placeCorners(sizes: List<IntSize?>, width: Int, height: Int, margins: CornerMargins, ltr: Boolean): List<IntRect?> {
    require(sizes.size == CornerSlot.entries.size) { "One size per corner slot" }
    val (ts, te, bs, be, bc) = sizes
    val room = cornerRoom(width, height, margins).width
    val gap = margins.gap
    val start = margins.start
    val top = margins.top
    val right = width - margins.end
    val bottom = height - margins.bottom
    fun fit(first: IntSize?, end: IntSize) = first == null || first.width + gap + end.width <= room

    // Positions from the start edge first; mirrored at the end for right-to-left layouts.
    val places = arrayOfNulls<Pair<Int, Int>>(CornerSlot.entries.size)
    if (ts != null) places[CornerSlot.TopStart.ordinal] = start to top
    if (te != null) places[CornerSlot.TopEnd.ordinal] = (right - te.width) to (if (fit(ts, te)) top else top + ts!!.height + gap)
    if (bs != null) places[CornerSlot.BottomStart.ordinal] = start to (bottom - bs.height)
    val beTop = when {
        be == null -> bottom
        fit(bs, be) -> bottom - be.height
        else -> bottom - bs!!.height - gap - be.height
    }
    if (be != null) places[CornerSlot.BottomEnd.ordinal] = (right - be.width) to beTop
    if (bc != null) {
        val x = start + (room - bc.width) / 2
        val tops = buildList {
            if (bs != null && x < start + bs.width + gap) add(bottom - bs.height)
            if (be != null && x + bc.width > right - be.width - gap) add(beTop)
        }
        places[CornerSlot.BottomCenter.ordinal] = x to ((tops.minOrNull()?.minus(gap) ?: bottom) - bc.height)
    }
    return sizes.mapIndexed { i, size ->
        val (x, y) = places[i] ?: return@mapIndexed null
        val w = size!!.width
        val left = if (ltr) x else width - x - w
        IntRect(left, y, left + w, y + size.height)
    }
}
