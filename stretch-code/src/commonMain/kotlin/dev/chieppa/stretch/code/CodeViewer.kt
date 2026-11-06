package dev.chieppa.stretch.code

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchSpacing
import dev.chieppa.stretch.ui.FindBar
import dev.chieppa.stretch.ui.Scrollbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What the app can add to a [CodeViewer], by line number counting from 1. Everything is optional. */
class CodeViewerSlots(
    /** Drawn in a column before the line numbers, such as a dot or an icon. Emit nothing for a line with none. */
    val gutterMark: (@Composable (line: Int) -> Unit)? = null,
    /** How wide the [gutterMark] column is. */
    val gutterWidth: Dp = StretchSpacing.space16,
    /** Shown under a line, across the viewer, such as a note or a thread. Emit nothing for a line with none. */
    val lineAnnotation: (@Composable (line: Int) -> Unit)? = null,
    /** Called when someone clicks a line's number. */
    val onLineNumberClick: ((line: Int) -> Unit)? = null,
)

/**
 * A whole text, read-only, with line numbers and code colored by [language]. Only the lines on screen are built, so
 * long files stay smooth. Text selects across lines and copies. [tones] washes whole lines, such as changed ones.
 * Ctrl F (Cmd F on macOS) opens the find bar, F3 and Shift F3 move between matches, Esc closes it. [state] holds
 * wrapping, scrolling and finding; scroll to a line with [CodeViewerState.scrollToLine]. [tokens] colors the code from
 * outside, such as from a parser the app runs: one list per line from line 1, offsets within the line ([tokensByLine]
 * splits a whole text's spans). Null colors it by [language] with the built-in tokenizer. Tokens may arrive after the
 * text; the view keeps its place when they do.
 */
@Composable
fun CodeViewer(
    text: CodeText,
    modifier: Modifier = Modifier,
    state: CodeViewerState = rememberCodeViewerState(),
    language: CodeLanguage? = null,
    tones: (line: Int) -> LineTone? = { null },
    strings: CodeViewerStrings = CodeViewerStrings(),
    slots: CodeViewerSlots = remember { CodeViewerSlots() },
    tokens: List<List<CodeToken>>? = null,
) {
    val builtIn by produceState(emptyList<List<CodeToken>>(), text, language, tokens == null) {
        value = if (tokens != null) emptyList() else withContext(Dispatchers.Default) { tokenizeLines(text.lines, language) }
    }
    val lineTokensOf = tokens ?: builtIn
    LaunchedEffect(text, state.query, state.matchCase) {
        val found = withContext(Dispatchers.Default) { findInLines(text.lines, state.query, state.matchCase) }
        state.updateMatches(found)
    }
    val matchesByLine = remember(state.matches) { state.matches.withIndex().groupBy { it.value.line } }
    val longest = remember(text) { longestLine(text.lines) }
    val metrics = rememberCodeMetrics(longest, text.lineCount)
    val look = rememberCodeLook()
    val charWidth = with(LocalDensity.current) { metrics.charWidth.toPx() }
    SideEffect { state.charWidth = charWidth }
    val scope = rememberCoroutineScope()
    val findFocus = remember { FocusRequester() }
    var opened by remember { mutableIntStateOf(0) }

    Column(
        modifier.background(Stretch.colors.surfacePanel).onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            when {
                event.key == Key.F && (event.isCtrlPressed || event.isMetaPressed) -> {
                    state.findOpen = true
                    opened++
                }
                event.key == Key.F3 -> scope.launch { if (event.isShiftPressed) state.previousMatch() else state.nextMatch() }
                event.key == Key.Escape && state.findOpen -> state.findOpen = false
                else -> return@onPreviewKeyEvent false
            }
            true
        },
    ) {
        if (state.findOpen) FindBar(state, strings.find, findFocus, opened)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            SelectionContainer {
                LazyColumn(Modifier.fillMaxSize(), state.list) {
                    items(text.lineCount, key = { it }) { index ->
                        val number = index + 1
                        val line = text.lines[index]
                        val highlights = matchesByLine[number].orEmpty().map { (at, range) ->
                            if (at == state.currentMatch) {
                                CodeHighlight(range.start, range.end, look.currentMatch, look.onCurrentMatch)
                            } else {
                                CodeHighlight(range.start, range.end, look.match)
                            }
                        }
                        val lineTokens = lineTokensOf.getOrNull(index).orEmpty()
                        val annotated = remember(line, lineTokens, highlights, look) { codeLineText(line, lineTokens, look.code, highlights) }
                        val click = slots.onLineNumberClick
                        Column {
                            CodeLineRow(
                                number, annotated, metrics, look, state.wrap, state.horizontal,
                                tone = tones(number),
                                gutterWidth = slots.gutterWidth,
                                gutter = slots.gutterMark?.let { mark -> { mark(number) } },
                                numberModifier = if (click != null) Modifier.clickable(role = Role.Button) { click(number) } else Modifier,
                            )
                            slots.lineAnnotation?.let { annotation -> DisableSelection { annotation(number) } }
                        }
                    }
                }
            }
            Scrollbar(state.list, Modifier.align(Alignment.CenterEnd))
        }
    }
}
