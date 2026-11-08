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
import dev.chieppa.stretch.code.CodeSpot
import dev.chieppa.stretch.code.CodeSymbolPointer
import dev.chieppa.stretch.code.revealColumn
import dev.chieppa.stretch.code.tabOffsets

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

    /** What the viewer knows of the pointer over its code, for clicking through to symbols. */
    internal val symbols = CodeSymbolPointer<DiffFileSide>()

    /**
     * Where [clickSymbolAtCaret] starts: where the last plain press in the code landed, which is where a selection made
     * by dragging from it starts, or wherever the app puts it, such as on a definition it has gone to. Null until then.
     */
    var caret: DiffPosition?
        get() = symbols.caret?.let { DiffPosition(it.group.fileIndex, it.group.side, it.line, it.column) }
        set(value) {
            symbols.caret = value?.let { CodeSpot(DiffFileSide(it.fileIndex, it.side), it.line, it.column) }
        }

    /**
     * Clicks through to the symbol at [caret], as a Ctrl or Cmd click there would: asks the viewer's
     * [DiffSlots.symbolAt], scrolls the symbol into view, opening its file and fold when they hide it, and calls
     * [DiffSlots.onSymbolClick] with its bounds. For a key the app binds, such as F12. Whether there was a symbol.
     */
    suspend fun clickSymbolAtCaret(): Boolean = symbols.clickAtCaret { spot, range ->
        scrollToMatch(DiffMatch(spot.group.fileIndex, spot.group.side, range.line, range.start, range.end))
    }

    /** The lines selected over the line numbers, if any. Set it to null to clear the selection. */
    var lineSelection by mutableStateOf<DiffLineRange?>(null)

    /** Where a Shift-click extends the selection from. */
    internal var selectionAnchor = 0

    /** Where the list is in its window, to find the line under a dragging pointer. */
    internal var listCoordinates: LayoutCoordinates? = null

    /** Rows currently laid out, for moving between changes. */
    internal var rows by mutableStateOf(emptyList<DiffRow>())

    /** The files showing, to open the file and fold a match is in. */
    internal var files: List<FileDiff> = emptyList()

    /** One character's width in pixels, to scroll sideways to a column. */
    internal var charWidth = 0f

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

    /**
     * Scrolls so [match] shows, opening its file and the fold hiding its line when need be: to its row, with rows above
     * it, unless it already shows away from the edges, and sideways to it while lines do not wrap.
     */
    suspend fun scrollToMatch(match: DiffMatch) {
        val file = files.getOrNull(match.fileIndex) ?: return
        val before = rows
        if (match.fileIndex in collapsedFiles) collapsedFiles = collapsedFiles - match.fileIndex
        hiddenBy(match, diffRows(files, split, unfolded, collapsedFiles))?.let { unfold(it) }
        val laidOut = diffRows(files, split, unfolded, collapsedFiles)
        val index = laidOut.indexOfFirst { it.fileIndex == match.fileIndex && it.numberOn(match.side) == match.line }
            .takeIf { it >= 0 } ?: rowOfLine(laidOut, match.fileIndex, match.side, match.line) ?: return
        val visible = list.layoutInfo.visibleItemsInfo
        val lead = (index - (visible.size / 2).coerceAtLeast(LEAD_IN_ROWS)).coerceAtLeast(0)
        if (laidOut.size != before.size) {
            // The rows change at the next layout; the request lands on them rather than on the rows showing now.
            list.requestScrollToItem(lead)
        } else if (visible.size <= 2 || index !in visible[1].index..visible[visible.size - 2].index) {
            list.scrollToItem(lead)
        }
        if (!wrap) {
            val text = file.hunks.asSequence().flatMap { it.lines }.firstOrNull { it.numberOn(match.side) == match.line }?.text.orEmpty()
            val scroll = if (split && match.side == DiffSide.New) rightScroll else leftScroll
            scroll.revealColumn(tabOffsets(text)[match.start.coerceIn(0, text.length)], charWidth)
        }
    }

    /** The fold among [laidOut] that hides [match]'s line, if one does. */
    private fun hiddenBy(match: DiffMatch, laidOut: List<DiffRow>): FoldId? {
        val file = files[match.fileIndex]
        return laidOut.asSequence().filterIsInstance<DiffRow.Fold>().map { it.id to it.lineCount }.firstOrNull { (id, count) ->
            id.fileIndex == match.fileIndex &&
                file.hunks[id.hunkIndex].lines.subList(id.firstLine, id.firstLine + count).any { it.numberOn(match.side) == match.line }
        }?.first
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
