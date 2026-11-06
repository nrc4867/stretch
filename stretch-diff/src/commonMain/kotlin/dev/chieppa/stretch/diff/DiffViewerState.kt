package dev.chieppa.stretch.diff

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates

/** How a [DiffViewer] is laid out and where it has scrolled. Changing a property updates the viewer. */
@Stable
class DiffViewerState(split: Boolean = false, wrap: Boolean = false) {
    /** Old and new side by side, rather than one column. */
    var split by mutableStateOf(split)

    /** Long lines wrap, rather than scroll sideways. */
    var wrap by mutableStateOf(wrap)

    /** Files showing only their header, by index. */
    var collapsedFiles by mutableStateOf(emptySet<Int>())

    /** Runs of unchanged lines a person has opened. */
    var unfolded by mutableStateOf(emptySet<FoldId>())

    val list = LazyListState()

    /** The sideways scroll of the unified layout, and of the old side when split. */
    val leftScroll = ScrollState(0)

    /** The sideways scroll of the new side when split. */
    val rightScroll = ScrollState(0)

    /** The lines selected over the line numbers, if any. Set it to null to clear the selection. */
    var lineSelection by mutableStateOf<DiffLineRange?>(null)

    /** Where a Shift-click extends the selection from. */
    internal var selectionAnchor = 0

    /** Where the list is in its window, to find the line under a dragging pointer. */
    internal var listCoordinates: LayoutCoordinates? = null

    /** Rows currently laid out, for moving between changes. */
    internal var rows by mutableStateOf(emptyList<DiffRow>())

    fun toggleFile(index: Int) {
        collapsedFiles = if (index in collapsedFiles) collapsedFiles - index else collapsedFiles + index
    }

    fun unfold(id: FoldId) {
        unfolded = unfolded + id
    }

    /** Scrolls to the next change below the top of the view, if there is one. */
    suspend fun nextChange() {
        val at = list.firstVisibleItemIndex
        changeStarts(rows).firstOrNull { it > at }?.let { list.animateScrollToItem(leadIn(it)) }
    }

    /** Scrolls to the nearest change above the top of the view, if there is one. */
    suspend fun previousChange() {
        val at = list.firstVisibleItemIndex + LEAD_IN_ROWS
        changeStarts(rows).lastOrNull { it < at }?.let { list.animateScrollToItem(leadIn(it)) }
    }

    /** The first line on [side] showing at the top of the list, with the index of its file, or null when none shows. */
    fun firstVisibleLine(side: DiffSide): Pair<Int, Int>? {
        val shown = list.layoutInfo.visibleItemsInfo.map { it.index }.ifEmpty { listOf(list.firstVisibleItemIndex) }
        for (index in shown) {
            val row = rows.getOrNull(index) ?: continue
            val number = row.numberOn(side) ?: continue
            return (row.fileIndex ?: continue) to number
        }
        return null
    }

    /** Scrolls so [line] on [side] of file [fileIndex] is at the top, or the nearest line to it that shows. */
    suspend fun scrollToLine(fileIndex: Int, side: DiffSide, line: Int) {
        rowOfLine(rows, fileIndex, side, line)?.let { list.scrollToItem(it) }
    }

    /** The line number on [side] of file [fileIndex] at [window], a point in the window, if a row there has one. */
    internal fun numberAt(window: Offset, fileIndex: Int, side: DiffSide): Int? {
        val coordinates = listCoordinates?.takeIf { it.isAttached } ?: return null
        val y = coordinates.windowToLocal(window).y
        val items = list.layoutInfo.visibleItemsInfo
        val item = items.firstOrNull { y >= it.offset && y < it.offset + it.size }
            ?: if (items.isNotEmpty() && y < items.first().offset) items.first() else items.lastOrNull()
            ?: return null
        val row = rows.getOrNull(item.index)?.takeIf { it.fileIndex == fileIndex } ?: return null
        return row.numberOn(side)
    }

    private fun leadIn(row: Int) = (row - LEAD_IN_ROWS).coerceAtLeast(0)

    private companion object {
        /** Rows left showing above a change scrolled to, so it reads in context. */
        const val LEAD_IN_ROWS = 2
    }
}

@Composable
fun rememberDiffViewerState(split: Boolean = false, wrap: Boolean = false): DiffViewerState = remember { DiffViewerState(split, wrap) }

/** Text a [DiffViewer] shows. Apps pass their own for other languages. */
data class DiffStrings(
    val unified: String = "Unified",
    val split: String = "Split",
    val wrap: String = "Wrap long lines",
    val previousChange: String = "Previous change",
    val nextChange: String = "Next change",
    val binary: String = "Binary file",
    val selectLine: String = "Select line",
    val toggleFile: String = "Show or hide this file",
    val unfold: (lines: Int) -> String = { "$it unchanged lines" },
    val summary: (files: Int, added: Int, removed: Int) -> String = { files, added, removed ->
        "$files ${if (files == 1) "file" else "files"}, +$added, −$removed"
    },
)

/**
 * A line someone can act on: which file and hunk it is in, and which side it shows on. Side by side, the left half is
 * [DiffSide.Old] and the right [DiffSide.New]; in one column a removed line is old and every other line new.
 */
data class DiffLineRef(val file: FileDiff, val hunk: DiffHunk, val line: DiffLine, val side: DiffSide = if (line.kind == LineKind.Removed) DiffSide.Old else DiffSide.New) {
    /** The line's number on its [side]. */
    val number: Int? get() = if (side == DiffSide.Old) line.oldNumber else line.newNumber
}
