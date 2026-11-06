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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import dev.chieppa.stretch.code.longestLine
import dev.chieppa.stretch.code.rememberCodeMetrics
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.ui.Scrollbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The changes to one or more files, with code colored, the changed words within a line marked, and long runs of
 * unchanged lines folded. Make [files] with [diffTexts] from two texts or [parseUnifiedDiff] from a patch. Only rows on
 * screen are built, so large diffs stay smooth. [showBar] adds a bar with the totals, layout and wrap controls, and
 * moving between changes; an app can drive the same things from [state] with its own controls. [tokens] colors files
 * from outside, by whole file per side (see [FileTokens]); files and sides without them are tokenized hunk by hunk by
 * the language of their path. Tokens may arrive after the files; the view keeps its place when they do. [matches] are
 * find matches the app finds, drawn in place with [currentMatch] set apart; [DiffViewerState.scrollToMatch] brings one
 * into view, opening its file and fold when they hide it.
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
    val look = rememberLineLook()
    val charWidth = with(LocalDensity.current) { metrics.charWidth.toPx() }
    SideEffect {
        state.rows = rows
        state.files = files
        state.charWidth = charWidth
    }
    val finds = remember(matches, currentMatch) { DiffFinds(matches, currentMatch) }
    val context = DiffRowContext(files, lineTokens, finds, metrics, look, state, strings, slots)
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
                                    DiffRowContent(row, context)
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
