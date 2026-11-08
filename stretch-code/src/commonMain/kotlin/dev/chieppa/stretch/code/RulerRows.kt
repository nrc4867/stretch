package dev.chieppa.stretch.code

import androidx.compose.foundation.lazy.LazyListState
import dev.chieppa.stretch.ui.RulerFit

/** How many rows [line] takes once its tabs expand and it wraps at [columns] characters; one when [columns] is unknown. */
fun wrappedRows(line: String, columns: Int): Int {
    if (columns <= 0) return 1
    val length = if ('\t' in line) expandTabs(line).length else line.length
    return maxOf(1, (length + columns - 1) / columns)
}

/**
 * Where each item of a lazy view lies along an [dev.chieppa.stretch.ui.OverviewRuler]: item i takes [extents]`[i]`
 * units, such as the rows of text a wrapped line takes, so the ruler's marks follow the view's real heights.
 */
class RulerRows(extents: FloatArray) {
    private val starts = FloatArray(extents.size + 1).also { starts ->
        for (index in extents.indices) starts[index + 1] = starts[index] + extents[index]
    }

    /** How long the whole view is. */
    val length: Float get() = starts.last()

    /** Where item [index] starts. */
    fun start(index: Int): Float = starts[index.coerceIn(0, starts.size - 1)]

    /** Where item [index] ends. */
    fun end(index: Int): Float = starts[(index + 1).coerceIn(0, starts.size - 1)]

    /** The item at [position]. */
    fun indexAt(position: Float): Int {
        if (starts.size <= 1) return 0
        var low = 0
        var high = starts.size - 2
        while (low < high) {
            val middle = (low + high + 1) / 2
            if (starts[middle] <= position) low = middle else high = middle - 1
        }
        return low
    }

    /** The part of the view [list] shows, or null before it is laid out. */
    fun shown(list: LazyListState): ClosedFloatingPointRange<Float>? {
        val items = list.layoutInfo.visibleItemsInfo
        val first = items.firstOrNull() ?: return null
        val last = items.last()
        val viewEnd = list.layoutInfo.viewportEndOffset
        fun at(index: Int, offset: Int, size: Int, into: Int) =
            start(index) + (end(index) - start(index)) * if (size > 0) (into - offset).toFloat().coerceIn(0f, size.toFloat()) / size else 0f
        return at(first.index, first.offset, first.size, 0)..at(last.index, last.offset, last.size, viewEnd)
    }

    /**
     * Where this view's units lie in pixels while [list] shows every item with room to spare and nothing to scroll, or
     * null while it scrolls or before it is laid out. A position falls within its item's real height.
     */
    fun fit(list: LazyListState): RulerFit? {
        if (list.canScrollForward || list.canScrollBackward) return null
        val items = list.layoutInfo.visibleItemsInfo
        if (items.isEmpty() || items.size != list.layoutInfo.totalItemsCount) return null
        val top = list.layoutInfo.viewportStartOffset
        return object : RulerFit {
            override val end: Float = (items.last().offset + items.last().size - top).toFloat()
            override fun at(position: Float): Float {
                val index = indexAt(position).coerceIn(0, items.size - 1)
                val item = items[index]
                val extent = end(index) - start(index)
                val into = if (extent > 0f) ((position - start(index)) / extent).coerceIn(0f, 1f) else 0f
                return item.offset - top + into * item.size
            }
        }
    }

    /** Scrolls [list] so the item at [position] shows in the middle of the view. */
    suspend fun jump(list: LazyListState, position: Float) {
        val index = indexAt(position)
        val showing = list.layoutInfo.visibleItemsInfo.size
        list.scrollToItem((index - showing / 2).coerceAtLeast(0))
    }
}
