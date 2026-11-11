package dev.chieppa.stretch.diff

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import dev.chieppa.stretch.code.ChangeWash
import dev.chieppa.stretch.code.CodeDecorationTooltip
import dev.chieppa.stretch.code.CodeSpot
import dev.chieppa.stretch.code.CodeSymbolCardPopup
import dev.chieppa.stretch.code.codeSymbolKeys
import dev.chieppa.stretch.code.rulerColors
import dev.chieppa.stretch.code.CodeMetrics
import dev.chieppa.stretch.code.longestLine
import dev.chieppa.stretch.code.rememberCodeMetrics
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchSpacing
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.ui.Scrollbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.withContext

/**
 * The changes to one or more files, with code colored, the changed words within a line marked, and long runs of
 * unchanged lines folded. Make [files] with [diffTexts] from two texts or [parseUnifiedDiff] from a patch. Only rows on
 * screen are built, so large diffs stay smooth. [showBar] adds a bar with the totals, layout and wrap controls, and
 * moving between changes; an app can drive the same things from [state] with its own controls. [tokens] colors files
 * from outside, by whole file per side (see [FileTokens]); files and sides without them are tokenized hunk by hunk by
 * the language of their path. Tokens may arrive after the files; the view keeps its place when they do. [matches] are
 * find matches the app finds, drawn in place with [currentMatch] set apart; [DiffViewerState.scrollToMatch] brings one
 * into view, opening its file and fold when they hide it. With [ruler] on, a strip beside the scrollbar marks removed
 * and added lines, find matches and the app's [marks] along the whole diff, where wrapped rows take their real share;
 * pressing or dragging on it scrolls there. [changeWash] puts added and removed lines' wash behind the whole line or
 * only behind its gutter, numbers and sign. [decorations] set runs of code apart in a few themed styles, with a tooltip
 * each (see [DiffDecoration]); warnings and errors also mark the ruler. With [DiffSlots.symbolAt] and
 * [DiffSlots.onSymbolClick], holding Ctrl (Cmd on macOS) over a name underlines it and a click goes through to it;
 * [DiffViewerState.clickSymbolAtCaret] does the same from a key. With [DiffSlots.symbolCard] too, resting the pointer on
 * a name opens the app's card about it. [blocks] are runs of unchanged lines [DiffSlots.lineBlock] draws in place of their
 * code.
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
    matches: List<DiffMatch> = emptyList(),
    currentMatch: DiffMatch? = null,
    marks: List<DiffMark> = emptyList(),
    ruler: Boolean = true,
    changeWash: ChangeWash = ChangeWash.Line,
    decorations: List<DiffDecoration> = emptyList(),
    blocks: List<DiffBlock> = emptyList(),
) {
    val c = Stretch.colors
    val rows = remember(files, state.split, state.unfolded, state.collapsedFiles) {
        diffRows(files, state.split, state.unfolded, state.collapsedFiles)
    }
    val builtIn by produceState(DiffTokens.None, files) { value = withContext(Dispatchers.Default) { tokenizeDiff(files) } }
    val supplied = remember(files, tokens) { files.map { tokens[it] } }
    val lineTokens = remember(builtIn, supplied) { LineTokens(builtIn, supplied) }
    val longest = remember(files) { files.maxOfOrNull { file -> file.hunks.maxOfOrNull { hunk -> longestLine(hunk.lines.map { it.text }) } ?: 0 } ?: 0 }
    val highest = remember(files) { files.maxOfOrNull { file -> file.hunks.maxOfOrNull { hunk -> hunk.lines.maxOfOrNull { maxOf(it.oldNumber ?: 0, it.newNumber ?: 0) } ?: 0 } ?: 0 } ?: 0 }
    val metrics = rememberCodeMetrics(longest, highest)
    val look = rememberLineLook(changeWash)
    val charWidth = with(LocalDensity.current) { metrics.charWidth.toPx() }
    SideEffect {
        state.rows = rows
        state.files = files
        state.charWidth = charWidth
    }
    val finds = remember(matches, currentMatch) { DiffFinds(matches, currentMatch) }
    val decorated = remember(decorations) { DiffDecorations(decorations) }
    val symbols = state.symbols
    val scope = rememberCoroutineScope()
    SideEffect {
        symbols.symbolAt = slots.symbolAt?.let { ask -> { spot -> ask(spot.position()) } }
        symbols.onClick = slots.onSymbolClick?.let { click -> { spot, range, bounds -> click(DiffSymbolClick(spot.position(), range, bounds)) } }
        symbols.frameClock = scope.coroutineContext[MonotonicFrameClock]
    }
    SideEffect { symbols.card.enabled = slots.symbolCard != null && slots.symbolAt != null }
    LaunchedEffect(symbols) { symbols.followPointer() }
    LaunchedEffect(symbols) { symbols.card.follow() }
    LaunchedEffect(symbols, state.list) {
        snapshotFlow { state.list.firstVisibleItemIndex to state.list.firstVisibleItemScrollOffset }.drop(1).collect { symbols.card.dismiss() }
    }
    val bars = rulerColors
    val allMarks = remember(marks, decorations, bars) {
        marks + decorations.mapNotNull { d -> bars.of(d.style)?.let { DiffMark(d.fileIndex, d.side, d.range.line, it) } }
    }
    var listWidth by remember { mutableIntStateOf(0) }
    val columns = wrapColumns(listWidth, state.split, metrics, slots, charWidth)
    val blockRoles = remember(rows, blocks, slots.lineBlock != null) { if (slots.lineBlock == null) emptyMap() else DiffBlockRows.of(rows, blocks) }
    val context = DiffRowContext(files, lineTokens, finds, decorated, metrics, look, state, strings, slots, blockRoles)
    val content = Modifier.onGloballyPositioned { symbols.outer = it }.then(modifier).background(c.surfacePanel)
        .codeSymbolKeys(symbols).onGloballyPositioned { symbols.inner = it }
    Column(content) {
        if (showBar) DiffBar(files, state, strings)
        Row(Modifier.weight(1f).fillMaxWidth()) {
            Box(Modifier.weight(1f).fillMaxHeight().onSizeChanged { listWidth = it.width }) {
                SelectionContainer {
                    LazyColumn(Modifier.fillMaxSize().onGloballyPositioned { state.listCoordinates = it }, state.list) {
                        for (row in rows) {
                            when (row) {
                                is DiffRow.FileHeader -> stickyHeader(row.key, "file") { FileHeaderRow(row, state, strings, slots) }
                                else -> item(row.key, row::class.simpleName) {
                                    DiffRowContent(row, context)
                                }
                            }
                        }
                    }
                }
                Scrollbar(state.list, Modifier.align(Alignment.CenterEnd))
            }
            if (ruler) DiffRuler(rows, files, matches, currentMatch, allMarks, state, state.wrap, columns)
        }
        CodeDecorationTooltip(symbols)
        slots.symbolCard?.let { card -> CodeSymbolCardPopup(symbols) { rest, open -> card(rest.spot.position(), rest.range, open) } }
    }
}

/** How many characters a wrapped line has room for in a list [width] pixels wide, beside its gutter, numbers and sign. */
@Composable
private fun wrapColumns(width: Int, split: Boolean, metrics: CodeMetrics, slots: DiffSlots, charWidth: Float): Int {
    val gutter = if (slots.gutterMark != null) slots.gutterWidth else 0.dp
    val beside = with(LocalDensity.current) { (gutter + metrics.numberWidth * (if (split) 1 else 2) + StretchSpacing.space16).toPx() }
    val room = if (split) width / 2f else width.toFloat()
    return if (charWidth > 0f) ((room - beside) / charWidth).toInt() else 0
}

/** The position this character is at, as an app reads it. */
private fun CodeSpot<DiffFileSide>.position() = DiffPosition(group.fileIndex, group.side, line, column)
