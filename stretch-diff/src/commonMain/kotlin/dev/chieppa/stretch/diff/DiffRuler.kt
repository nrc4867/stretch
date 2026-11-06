package dev.chieppa.stretch.diff

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import dev.chieppa.stretch.code.RulerColors
import dev.chieppa.stretch.code.RulerRows
import dev.chieppa.stretch.code.rulerColors
import dev.chieppa.stretch.code.wrappedRows
import dev.chieppa.stretch.ui.OverviewRuler
import dev.chieppa.stretch.ui.RulerLanes
import dev.chieppa.stretch.ui.RulerMark
import kotlinx.coroutines.launch

/** A mark an app puts on a [DiffViewer]'s ruler at [line] on [side] of the file at [fileIndex], in [color], such as for a note. */
@Immutable
data class DiffMark(val fileIndex: Int, val side: DiffSide, val line: Int, val color: Color)

/**
 * The ruler beside a [DiffViewer]: removed lines in its start lane, added ones in its end lane, and find matches and
 * the app's [marks] down its middle. A line folded away marks its fold. Wrapped rows take their real share when [wrap].
 */
@Composable
internal fun DiffRuler(
    rows: List<DiffRow>,
    files: List<FileDiff>,
    matches: List<DiffMatch>,
    current: DiffMatch?,
    marks: List<DiffMark>,
    state: DiffViewerState,
    wrap: Boolean,
    columns: Int,
) {
    val colors = rulerColors
    val scale = remember(rows, wrap, columns) { rulerRows(rows, wrap, columns) }
    val changes = remember(rows, scale, colors) { changeMarks(rows, scale, colors) }
    val rowOf = remember(rows, files) { lineRows(rows, files) }
    val found = remember(scale, rowOf, matches, current, marks, colors) {
        fun mark(fileIndex: Int, side: DiffSide, line: Int, color: Color, across: ClosedFloatingPointRange<Float>) =
            rowOf[Triple(fileIndex, side, line)]?.let { RulerMark(scale.start(it), scale.end(it), color, across) }
        marks.mapNotNull { mark(it.fileIndex, it.side, it.line, it.color, RulerLanes.Middle) } +
            matches.mapNotNull { mark(it.fileIndex, it.side, it.line, colors.match, RulerLanes.Middle) } +
            listOfNotNull(current?.let { mark(it.fileIndex, it.side, it.line, colors.currentMatch, RulerLanes.Whole) })
    }
    val scope = rememberCoroutineScope()
    OverviewRuler(changes + found, scale.length, { position -> scope.launch { scale.jump(state.list, position) } }, shown = { scale.shown(state.list) })
}

/** Where [rows] lie along the ruler: a row each, or when [wrap] the rows of text each takes at [columns] characters. */
internal fun rulerRows(rows: List<DiffRow>, wrap: Boolean, columns: Int) = RulerRows(FloatArray(rows.size) { if (wrap) rowExtent(rows[it], columns) else 1f })

/** How many rows of text [row] takes wrapped at [columns] characters, each half of a side-by-side row at its own share. */
private fun rowExtent(row: DiffRow, columns: Int): Float = when (row) {
    is DiffRow.Unified -> wrappedRows(row.line.text, columns).toFloat()
    is DiffRow.Split -> maxOf(row.left?.let { wrappedRows(it.text, columns) } ?: 1, row.right?.let { wrappedRows(it.text, columns) } ?: 1).toFloat()
    else -> 1f
}

/** The row showing each line, by file, side and number; a line folded away maps to its fold. */
internal fun lineRows(rows: List<DiffRow>, files: List<FileDiff>): Map<Triple<Int, DiffSide, Int>, Int> = buildMap {
    rows.forEachIndexed { index, row ->
        val file = row.fileIndex ?: return@forEachIndexed
        if (row is DiffRow.Fold) {
            val hunk = files[file].hunks[row.id.hunkIndex]
            for (line in hunk.lines.subList(row.id.firstLine, row.id.firstLine + row.lineCount)) {
                for (side in DiffSide.entries) line.numberOn(side)?.let { put(Triple(file, side, it), index) }
            }
        } else {
            for (side in DiffSide.entries) row.numberOn(side)?.let { put(Triple(file, side, it), index) }
        }
    }
}

/** One mark per run of removed rows in the start lane and per run of added rows in the end lane. */
private fun changeMarks(rows: List<DiffRow>, scale: RulerRows, colors: RulerColors): List<RulerMark> = buildList {
    fun scan(color: Color, lane: ClosedFloatingPointRange<Float>, on: (DiffRow) -> Boolean) {
        var start = -1
        for (index in 0..rows.size) {
            val hit = index < rows.size && on(rows[index])
            if (hit && start < 0) start = index
            if (!hit && start >= 0) {
                add(RulerMark(scale.start(start), scale.start(index), color, lane))
                start = -1
            }
        }
    }
    scan(colors.removed, RulerLanes.Start) { row ->
        (row is DiffRow.Unified && row.line.kind == LineKind.Removed) || (row is DiffRow.Split && row.left?.kind == LineKind.Removed)
    }
    scan(colors.added, RulerLanes.End) { row ->
        (row is DiffRow.Unified && row.line.kind == LineKind.Added) || (row is DiffRow.Split && row.right?.kind == LineKind.Added)
    }
}
