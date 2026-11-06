package dev.chieppa.stretch.diff

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.onGloballyPositioned
import dev.chieppa.stretch.code.CodeMetrics
import dev.chieppa.stretch.code.LineTone
import dev.chieppa.stretch.code.longestLine
import dev.chieppa.stretch.code.rememberCodeMetrics
import dev.chieppa.stretch.theme.StretchSpacing
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.ui.IconButton
import dev.chieppa.stretch.ui.Label
import dev.chieppa.stretch.ui.Scrollbar
import dev.chieppa.stretch.ui.Segmented
import dev.chieppa.stretch.ui.Toggle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What the app can add to a [DiffViewer]. Everything is optional. */
class DiffSlots(
    /** Called when someone clicks a line's number, with the side of the number clicked. */
    val onLineClick: ((DiffLineRef) -> Unit)? = null,
    /** Shown under a line, across the viewer, such as a thread of notes. Emit nothing for a line with none. */
    val lineAnnotation: (@Composable (DiffLineRef) -> Unit)? = null,
    /** Shown at the end of a file's header, such as Stage or Revert. */
    val fileActions: (@Composable RowScope.(FileDiff) -> Unit)? = null,
    /** Shown at the end of a hunk's header, such as Stage hunk. */
    val hunkActions: (@Composable RowScope.(FileDiff, DiffHunk) -> Unit)? = null,
    /**
     * Turns on selecting lines over their numbers: press and drag, or Shift-click, along one side of one file. Called
     * with the range when the pointer lets go; [DiffViewerState.lineSelection] holds it until cleared.
     */
    val onLineRangeSelect: ((DiffLineRange) -> Unit)? = null,
    /** Drawn in a column before each side's line numbers, such as a dot for a line with notes. Emit nothing for none. */
    val gutterMark: (@Composable (DiffLineRef) -> Unit)? = null,
    /** How wide the [gutterMark] column is. */
    val gutterWidth: Dp = StretchSpacing.space16,
    /** A wash for a line in place of the added or removed one, such as to set apart lines already seen. Null keeps it. */
    val lineTone: ((DiffLineRef) -> LineTone?)? = null,
)

/**
 * The changes to one or more files, with code colored, the changed words within a line marked, and long runs of
 * unchanged lines folded. Make [files] with [diffTexts] from two texts or [parseUnifiedDiff] from a patch. Only rows on
 * screen are built, so large diffs stay smooth. [showBar] adds a bar with the totals, layout and wrap controls, and
 * moving between changes; an app can drive the same things from [state] with its own controls. [tokens] colors files
 * from outside, by whole file per side (see [FileTokens]); files and sides without them are tokenized hunk by hunk by
 * the language of their path. Tokens may arrive after the files; the view keeps its place when they do.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DiffViewer(
    files: List<FileDiff>,
    modifier: Modifier = Modifier,
    state: DiffViewerState = rememberDiffViewerState(),
    showBar: Boolean = true,
    strings: DiffStrings = DiffStrings(),
    slots: DiffSlots = remember { DiffSlots() },
    tokens: Map<FileDiff, FileTokens> = emptyMap(),
) {
    val c = Stretch.colors
    val rows = remember(files, state.split, state.unfolded, state.collapsedFiles) {
        diffRows(files, state.split, state.unfolded, state.collapsedFiles)
    }
    SideEffect { state.rows = rows }
    val builtIn by produceState(DiffTokens.None, files) { value = withContext(Dispatchers.Default) { tokenizeDiff(files) } }
    val supplied = remember(files, tokens) { files.map { tokens[it] } }
    val lineTokens = remember(builtIn, supplied) { LineTokens(builtIn, supplied) }
    val longest = remember(files) { files.maxOfOrNull { file -> file.hunks.maxOfOrNull { hunk -> longestLine(hunk.lines.map { it.text }) } ?: 0 } ?: 0 }
    val highest = remember(files) { files.maxOfOrNull { file -> file.hunks.maxOfOrNull { hunk -> hunk.lines.maxOfOrNull { maxOf(it.oldNumber ?: 0, it.newNumber ?: 0) } ?: 0 } ?: 0 } ?: 0 }
    val metrics = rememberCodeMetrics(longest, highest)
    val look = rememberLineLook()
    Column(modifier.background(c.surfacePanel)) {
        if (showBar) DiffBar(files, state, strings)
        Row(Modifier.weight(1f).fillMaxWidth()) {
            Box(Modifier.weight(1f).fillMaxHeight()) {
                SelectionContainer {
                    LazyColumn(Modifier.fillMaxSize().onGloballyPositioned { state.listCoordinates = it }, state.list) {
                        for (row in rows) {
                            when (row) {
                                is DiffRow.FileHeader -> stickyHeader(row.key, "file") { FileHeaderRow(row, state, strings, slots) }
                                else -> item(row.key, row::class.simpleName) {
                                    DiffRowContent(row, files, lineTokens, metrics, look, state, strings, slots)
                                }
                            }
                        }
                    }
                }
                Scrollbar(state.list, Modifier.align(Alignment.CenterEnd))
            }
            ChangeRuler(rows, state)
        }
    }
}

@Composable
private fun DiffRowContent(
    row: DiffRow,
    files: List<FileDiff>,
    tokens: LineTokens,
    metrics: CodeMetrics,
    look: LineLook,
    state: DiffViewerState,
    strings: DiffStrings,
    slots: DiffSlots,
) {
    when (row) {
        is DiffRow.HunkHeader -> HunkHeaderRow(row, files[row.fileIndex], slots)
        is DiffRow.Fold -> FoldRow(row, strings) { state.unfold(row.id) }
        is DiffRow.Binary -> Label(strings.binary, Stretch.type.bodySm, Stretch.colors.inkMuted, Modifier.padding(12.dp))
        is DiffRow.Unified -> {
            val file = files[row.fileIndex]
            val line = row.line
            val ref = DiffLineRef(file, file.hunks[row.hunkIndex], line)
            val extras = LineExtras(
                tone = lineToneOf(state, slots, ref, row.fileIndex, line.oldNumber, line.newNumber),
                gutter = slots.gutterMark?.let { mark -> { mark(ref) } },
                gutterWidth = slots.gutterWidth,
                oldNumber = numberInput(state, files, row.fileIndex, ref.copy(side = DiffSide.Old), line.oldNumber, strings, slots),
                newNumber = numberInput(state, files, row.fileIndex, ref.copy(side = DiffSide.New), line.newNumber, strings, slots),
            )
            UnifiedLine(line, tokens.of(row.fileIndex, line), metrics, look, state.wrap, state.leftScroll, extras)
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
                )
            }
            // Wrapped, the row is as tall as its taller side, and both sides fill it so their washes and numbers line up.
            Row(Modifier.fillMaxWidth().then(if (state.wrap) Modifier.height(IntrinsicSize.Min) else Modifier.height(metrics.lineHeight))) {
                SplitSide(DiffSide.Old, row.left, row.left?.let { tokens.of(row.fileIndex, it) }.orEmpty(), metrics, look, state.wrap, state.leftScroll, extrasOf(left))
                Box(Modifier.width(Stretch.stroke).fillMaxHeight().background(Stretch.colors.linePanel))
                SplitSide(DiffSide.New, row.right, row.right?.let { tokens.of(row.fileIndex, it) }.orEmpty(), metrics, look, state.wrap, state.rightScroll, extrasOf(right))
            }
            (right ?: left)?.let { slots.lineAnnotation?.invoke(it) }
        }
        is DiffRow.FileHeader -> Unit
    }
}

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

@Composable
private fun DiffBar(files: List<FileDiff>, state: DiffViewerState, strings: DiffStrings) {
    val c = Stretch.colors
    val scope = rememberCoroutineScope()
    val added = remember(files) { files.sumOf { it.added } }
    val removed = remember(files) { files.sumOf { it.removed } }
    Row(
        Modifier.fillMaxWidth().background(c.surfaceRail).padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Label(strings.summary(files.size, added, removed), Stretch.type.bodySm, c.inkMuted, Modifier.weight(1f))
        IconButton(StretchIcons.ArrowUp, strings.previousChange, { scope.launch { state.previousChange() } })
        IconButton(StretchIcons.ArrowDown, strings.nextChange, { scope.launch { state.nextChange() } })
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Label(strings.wrap, Stretch.type.bodySm, c.inkMuted)
            Toggle(state.wrap, { state.wrap = it }, strings.wrap)
        }
        Segmented(listOf(false, true), state.split, { state.split = it }, { if (it) strings.split else strings.unified })
    }
    Box(Modifier.fillMaxWidth().height(Stretch.stroke).background(c.linePanel))
}
