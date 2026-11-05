package dev.chieppa.stretch.diff

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
    /** Called when someone clicks a line's number, to start a comment or set a breakpoint. */
    val onLineClick: ((DiffLineRef) -> Unit)? = null,
    /** Shown under a line, across the viewer, such as a comment thread. Emit nothing for a line with none. */
    val lineAnnotation: (@Composable (DiffLineRef) -> Unit)? = null,
    /** Shown at the end of a file's header, such as Stage or Revert. */
    val fileActions: (@Composable RowScope.(FileDiff) -> Unit)? = null,
    /** Shown at the end of a hunk's header, such as Stage hunk. */
    val hunkActions: (@Composable RowScope.(FileDiff, DiffHunk) -> Unit)? = null,
)

/**
 * The changes to one or more files, with code colored, the changed words within a line marked, and long runs of
 * unchanged lines folded. Make [files] with [diffTexts] from two texts or [parseUnifiedDiff] from a patch. Only rows on
 * screen are built, so large diffs stay smooth. [showBar] adds a bar with the totals, layout and wrap controls, and
 * moving between changes; an app can drive the same things from [state] with its own controls.
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
) {
    val c = Stretch.colors
    val rows = remember(files, state.split, state.unfolded, state.collapsedFiles) {
        diffRows(files, state.split, state.unfolded, state.collapsedFiles)
    }
    SideEffect { state.rows = rows }
    val tokens by produceState(DiffTokens.None, files) { value = withContext(Dispatchers.Default) { tokenizeDiff(files) } }
    val metrics = rememberDiffMetrics(files)
    val look = rememberLineLook()
    Column(modifier.background(c.surfacePanel)) {
        if (showBar) DiffBar(files, state, strings)
        Row(Modifier.weight(1f).fillMaxWidth()) {
            Box(Modifier.weight(1f).fillMaxHeight()) {
                SelectionContainer {
                    LazyColumn(Modifier.fillMaxSize(), state.list) {
                        for (row in rows) {
                            when (row) {
                                is DiffRow.FileHeader -> stickyHeader(row.key, "file") { FileHeaderRow(row, state, strings, slots) }
                                else -> item(row.key, row::class.simpleName) {
                                    DiffRowContent(row, files, tokens, metrics, look, state, strings, slots)
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
    tokens: DiffTokens,
    metrics: DiffMetrics,
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
            val hunk = file.hunks[row.hunkIndex]
            UnifiedLine(row.line, tokens.of(row.fileIndex, row.line), metrics, look, state.wrap, state.leftScroll, slots.onLineClick?.let { { it(DiffLineRef(file, hunk, row.line)) } })
            slots.lineAnnotation?.invoke(DiffLineRef(file, hunk, row.line))
        }
        is DiffRow.Split -> {
            val file = files[row.fileIndex]
            val hunk = file.hunks[row.hunkIndex]
            Row(Modifier.fillMaxWidth().then(if (state.wrap) Modifier else Modifier.height(metrics.lineHeight))) {
                SplitSide(row.left, row.left?.let { tokens.of(row.fileIndex, it) }.orEmpty(), metrics, look, state.wrap, state.leftScroll, file, hunk, slots)
                Box(Modifier.width(Stretch.stroke).fillMaxHeight().background(Stretch.colors.linePanel))
                SplitSide(row.right, row.right?.let { tokens.of(row.fileIndex, it) }.orEmpty(), metrics, look, state.wrap, state.rightScroll, file, hunk, slots)
            }
            val annotated = row.right ?: row.left
            if (annotated != null) slots.lineAnnotation?.invoke(DiffLineRef(file, hunk, annotated))
        }
        is DiffRow.FileHeader -> Unit
    }
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

/** Room for the code: how wide its longest line is, and how wide the line numbers are. */
internal class DiffMetrics(val codeWidth: Dp, val numberWidth: Dp, val lineHeight: Dp)

@Composable
private fun rememberDiffMetrics(files: List<FileDiff>): DiffMetrics {
    val measurer = rememberTextMeasurer()
    val style = Stretch.type.valueSm
    val density = LocalDensity.current
    return remember(files, style, density) {
        val digit = measurer.measure("0", style).size
        val longest = files.maxOfOrNull { file -> file.hunks.maxOfOrNull { hunk -> hunk.lines.maxOfOrNull { expandTabs(it.text).length } ?: 0 } ?: 0 } ?: 0
        val highest = files.maxOfOrNull { file ->
            file.hunks.maxOfOrNull { hunk -> hunk.lines.maxOfOrNull { maxOf(it.oldNumber ?: 0, it.newNumber ?: 0) } ?: 0 } ?: 0
        } ?: 0
        with(density) {
            DiffMetrics(
                codeWidth = (digit.width * (longest + 2)).toDp(),
                numberWidth = (digit.width * highest.toString().length).toDp() + 12.dp,
                lineHeight = digit.height.toDp() + 2.dp,
            )
        }
    }
}
