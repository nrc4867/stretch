package dev.chieppa.stretch.code

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.TextLayoutResult

/**
 * Where one line's code, [text] before tabs expand, is laid out once it shows: its [layout] and the [coordinates] of the
 * text. A code view finds the character under the pointer and the bounds of a range through it.
 */
class CodeLineGeometry(val text: String) {
    var layout: TextLayoutResult? = null
    var coordinates: LayoutCoordinates? = null

    private val offsets by lazy { tabOffsets(text) }

    /** The column of the character under [position], a point on the text, or null when no character is there. */
    fun columnUnder(position: Offset): Int? {
        val layout = layout ?: return null
        val shown = layout.getOffsetForPosition(position)
        val length = layout.layoutInput.text.length
        val under = listOf(shown, shown - 1).firstOrNull { it in 0 until length && layout.getBoundingBox(it).contains(position) } ?: return null
        return columnOf(under)
    }

    /** The column of the place between characters nearest [position], a point on the text. */
    fun columnNearest(position: Offset): Int? = layout?.let { columnOf(it.getOffsetForPosition(position)) }

    /** The bounds of [start] until [end] on this line in [root]'s pixels, or null while the line is not laid out. */
    fun bounds(start: Int, end: Int, root: LayoutCoordinates): Rect? {
        val layout = layout ?: return null
        val coordinates = coordinates?.takeIf { it.isAttached && root.isAttached } ?: return null
        val from = offsets[start.coerceIn(0, text.length)]
        val until = offsets[end.coerceIn(0, text.length)]
        val box = if (until > from) layout.getPathForRange(from, until).getBounds() else layout.getCursorRect(from)
        return Rect(root.localPositionOf(coordinates, box.topLeft), box.size)
    }

    /** The column before tabs expand of [shown], an offset in the text as shown; inside a tab, the tab's own column. */
    private fun columnOf(shown: Int): Int {
        var low = 0
        var high = text.length
        while (low < high) {
            val mid = (low + high + 1) / 2
            if (offsets[mid] <= shown) low = mid else high = mid - 1
        }
        return low
    }
}
