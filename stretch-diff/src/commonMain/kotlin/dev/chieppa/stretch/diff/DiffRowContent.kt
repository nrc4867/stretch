package dev.chieppa.stretch.diff

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.code.CodeLineKey
import dev.chieppa.stretch.code.CodeMetrics
import dev.chieppa.stretch.code.LineTone
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.ui.Label

/** What every row of a [DiffViewer] draws with, gathered once per composition of the viewer. */
internal data class DiffRowContext(
    val files: List<FileDiff>,
    val tokens: LineTokens,
    val finds: DiffFinds,
    val decorations: DiffDecorations,
    val metrics: CodeMetrics,
    val look: LineLook,
    val state: DiffViewerState,
    val strings: DiffStrings,
    val slots: DiffSlots,
)

/** One row of a [DiffViewer] other than a file's header. */
@Composable
internal fun DiffRowContent(row: DiffRow, context: DiffRowContext) {
    val (files, tokens, finds, decorations, metrics, look, state, strings, slots) = context
    when (row) {
        is DiffRow.HunkHeader -> HunkHeaderRow(row, files[row.fileIndex], slots)
        is DiffRow.Fold -> FoldRow(row, strings) { state.unfold(row.id) }
        is DiffRow.Binary -> Label(strings.binary, Stretch.type.bodySm, Stretch.colors.inkMuted, Modifier.padding(12.dp))
        is DiffRow.Unified -> {
            val file = files[row.fileIndex]
            val line = row.line
            val ref = DiffLineRef(file, file.hunks[row.hunkIndex], line)
            val oldInput = numberInput(state, files, row.fileIndex, ref.copy(side = DiffSide.Old), line.oldNumber, strings, slots)
            val newInput = numberInput(state, files, row.fileIndex, ref.copy(side = DiffSide.New), line.newNumber, strings, slots)
            // A line on one side only lets its one number answer over the whole gutter, blank column included.
            val extras = LineExtras(
                tone = lineToneOf(state, slots, ref, row.fileIndex, line.oldNumber, line.newNumber),
                gutter = slots.gutterMark?.let { mark -> { mark(ref) } },
                gutterWidth = slots.gutterWidth,
                oldNumber = if (line.oldNumber != null) oldInput else newInput,
                newNumber = if (line.newNumber != null) newInput else oldInput,
                finds = finds.highlights(row.fileIndex, line, look.code),
                decorations = decorations.on(row.fileIndex, DiffSide.Old, line.oldNumber) + decorations.on(row.fileIndex, DiffSide.New, line.newNumber),
                keys = unifiedKeys(row.fileIndex, ref),
            )
            UnifiedLine(line, tokens.of(row.fileIndex, line), metrics, look, state.wrap, state.leftScroll, extras, state.symbols)
            slots.lineAnnotation?.invoke(ref)
        }
        is DiffRow.Split -> {
            val file = files[row.fileIndex]
            val hunk = file.hunks[row.hunkIndex]
            val left = row.left?.let { DiffLineRef(file, hunk, it, DiffSide.Old) }
            val right = row.right?.let { DiffLineRef(file, hunk, it, DiffSide.New) }
            fun extrasOf(ref: DiffLineRef?): LineExtras {
                if (ref == null) return LineExtras()
                val number = ref.number
                val input = numberInput(state, files, row.fileIndex, ref, number, strings, slots)
                val old = if (ref.side == DiffSide.Old) number else null
                val new = if (ref.side == DiffSide.New) number else null
                return LineExtras(
                    tone = lineToneOf(state, slots, ref, row.fileIndex, old, new),
                    gutter = slots.gutterMark?.let { mark -> { mark(ref) } },
                    gutterWidth = slots.gutterWidth,
                    oldNumber = input,
                    newNumber = input,
                    finds = finds.highlights(row.fileIndex, ref.line, look.code, ref.side),
                    decorations = decorations.on(row.fileIndex, ref.side, number),
                    keys = listOfNotNull(number?.let { CodeLineKey(DiffFileSide(row.fileIndex, ref.side), it) }),
                )
            }
            // Wrapped, the row is as tall as its taller side, and both sides fill it so their washes and numbers line up.
            Row(Modifier.fillMaxWidth().then(if (state.wrap) Modifier.height(IntrinsicSize.Min) else Modifier.height(metrics.lineHeight))) {
                SplitSide(DiffSide.Old, row.left, row.left?.let { tokens.of(row.fileIndex, it) }.orEmpty(), metrics, look, state.wrap, state.leftScroll, extrasOf(left), state.symbols)
                Box(Modifier.width(Stretch.stroke).fillMaxHeight().background(Stretch.colors.linePanel))
                SplitSide(DiffSide.New, row.right, row.right?.let { tokens.of(row.fileIndex, it) }.orEmpty(), metrics, look, state.wrap, state.rightScroll, extrasOf(right), state.symbols)
            }
            slots.lineAnnotation?.let { annotate ->
                left?.let { annotate(it) }
                right?.let { annotate(it) }
            }
        }
        is DiffRow.FileHeader -> Unit
    }
}

/**
 * What a line in one column answers to for clicking through to symbols: first its number on its own side, then, for a
 * line both sides share, its old number too.
 */
private fun unifiedKeys(fileIndex: Int, ref: DiffLineRef): List<CodeLineKey<DiffFileSide>> = listOfNotNull(
    ref.number?.let { CodeLineKey(DiffFileSide(fileIndex, ref.side), it) },
    ref.line.oldNumber?.takeIf { ref.side == DiffSide.New }?.let { CodeLineKey(DiffFileSide(fileIndex, DiffSide.Old), it) },
)

/** The wash for a line: the selection's when it holds the line, else the app's [DiffSlots.lineTone]. */
private fun lineToneOf(state: DiffViewerState, slots: DiffSlots, ref: DiffLineRef, fileIndex: Int, oldNumber: Int?, newNumber: Int?): LineTone? {
    val selection = state.lineSelection
    if (selection != null && selection.fileIndex == fileIndex) {
        val number = if (selection.side == DiffSide.Old) oldNumber else newNumber
        if (number != null && number in selection) return LineTone.Selected
    }
    return slots.lineTone?.invoke(ref)
}

/** Pointer input for the number of [ref] on its side, when there is a number there. */
private fun numberInput(state: DiffViewerState, files: List<FileDiff>, fileIndex: Int, ref: DiffLineRef, number: Int?, strings: DiffStrings, slots: DiffSlots): Modifier {
    if (number == null) return Modifier
    val click = slots.onLineClick
    return Modifier.lineNumberInput(state, files, fileIndex, ref.side, number, strings.selectLine, click?.let { { it(ref) } }, slots.onLineRangeSelect)
}
