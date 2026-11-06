package dev.chieppa.stretch.code

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import dev.chieppa.stretch.ui.OverviewRuler
import dev.chieppa.stretch.ui.RulerLanes
import dev.chieppa.stretch.ui.RulerMark
import kotlinx.coroutines.launch

/** A mark an app puts on a [CodeViewer]'s ruler at line [line], counting from 1, in [color], such as for a note. */
@Immutable
data class CodeMark(val line: Int, val color: Color)

/** The ruler beside a [CodeViewer]: toned lines in its start lane, find matches and the app's [marks] in its end lane. */
@Composable
internal fun CodeRuler(
    text: CodeText,
    tones: (line: Int) -> LineTone?,
    matches: List<CodeRange>,
    current: CodeRange?,
    marks: List<CodeMark>,
    state: CodeViewerState,
    columns: Int,
) {
    val colors = rulerColors
    val wrap = state.wrap
    val rows = remember(text, wrap, columns) {
        RulerRows(FloatArray(text.lineCount) { if (wrap) wrappedRows(text.lines[it], columns).toFloat() else 1f })
    }
    val toneMarks = remember(rows, tones, colors) { toneMarks(text.lineCount, tones, rows, colors) }
    val findMarks = remember(rows, matches, current, marks, colors) {
        marks.map { RulerMark(rows.start(it.line - 1), rows.end(it.line - 1), it.color, RulerLanes.End) } +
            matches.map { RulerMark(rows.start(it.line - 1), rows.end(it.line - 1), colors.match, RulerLanes.End) } +
            listOfNotNull(current?.let { RulerMark(rows.start(it.line - 1), rows.end(it.line - 1), colors.currentMatch, RulerLanes.Whole) })
    }
    val scope = rememberCoroutineScope()
    OverviewRuler(toneMarks + findMarks, rows.length, { position -> scope.launch { rows.jump(state.list, position) } }, shown = { rows.shown(state.list) })
}

/** One mark per run of lines in the same tone. */
private fun toneMarks(lineCount: Int, tones: (Int) -> LineTone?, rows: RulerRows, colors: RulerColors): List<RulerMark> = buildList {
    var runStart = 0
    var runTone: LineTone? = null
    for (index in 0..lineCount) {
        val tone = if (index < lineCount) tones(index + 1) else null
        if (tone == runTone) continue
        runTone?.let { add(RulerMark(rows.start(runStart), rows.start(index), colors.of(it), RulerLanes.Start)) }
        runStart = index
        runTone = tone
    }
}
