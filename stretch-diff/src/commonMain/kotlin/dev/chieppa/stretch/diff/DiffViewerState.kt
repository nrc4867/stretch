package dev.chieppa.stretch.diff

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

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
    internal val leftScroll = ScrollState(0)
    internal val rightScroll = ScrollState(0)

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
    val toggleFile: String = "Show or hide this file",
    val unfold: (lines: Int) -> String = { "$it unchanged lines" },
    val summary: (files: Int, added: Int, removed: Int) -> String = { files, added, removed ->
        "$files ${if (files == 1) "file" else "files"}, +$added, −$removed"
    },
)

/** A line someone can act on: which file and hunk it is in, and which side when the diff is split. */
data class DiffLineRef(val file: FileDiff, val hunk: DiffHunk, val line: DiffLine)
