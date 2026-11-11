package dev.chieppa.stretch.code

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchSpacing
import dev.chieppa.stretch.ui.FindBar
import dev.chieppa.stretch.ui.Scrollbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.withContext

/**
 * A whole text, read-only, with line numbers and code colored by [language]. Only the lines on screen are built, so
 * long files stay smooth. Text selects across lines and copies. [tones] washes whole lines, such as changed ones.
 * Ctrl F (Cmd F on macOS) opens the find bar, F3 and Shift F3 move between matches, Esc closes it. [state] holds
 * wrapping, scrolling and finding; scroll to a line with [CodeViewerState.scrollToLine]. [tokens] colors the code from
 * outside, such as from a parser the app runs: one list per line from line 1, offsets within the line ([tokensByLine]
 * splits a whole text's spans). Null colors it by [language] with the built-in tokenizer. Tokens may arrive after the
 * text; the view keeps its place when they do. [matches] are find matches the app finds itself, such as by a regular
 * expression, drawn in place of the find bar's with [currentMatch] set apart; [CodeViewerState.scrollToMatch] brings
 * one into view. With [ruler] on, a strip beside the scrollbar marks toned lines, find matches and the app's [marks]
 * along the whole text, where wrapped lines take their real share; pressing or dragging on it scrolls there. With
 * [changeBars] on, lines toned [LineTone.Added] or [LineTone.Removed] get a strong bar down their start edge, and
 * [changeWash] puts their wash behind the whole line or only behind its gutter and number. [decorations] set runs of
 * code apart in a few themed styles, with a tooltip each (see [CodeDecoration]); warnings and errors also mark the
 * ruler. With [CodeViewerSlots.symbolAt] and [CodeViewerSlots.onSymbolClick], holding Ctrl (Cmd on macOS) over a name
 * underlines it and a click goes through to it; [CodeViewerState.clickSymbolAtCaret] does the same from a key. With
 * [CodeViewerSlots.symbolCard] too, resting the pointer on a name opens the app's card about it. [blocks] are runs of
 * lines [CodeViewerSlots.lineBlock] draws in place of their code; runs that overlap or fall outside the text are left out.
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
    matches: List<CodeRange>? = null,
    currentMatch: CodeRange? = null,
    marks: List<CodeMark> = emptyList(),
    ruler: Boolean = true,
    changeBars: Boolean = true,
    changeWash: ChangeWash = ChangeWash.Line,
    decorations: List<CodeDecoration> = emptyList(),
    blocks: List<IntRange> = emptyList(),
) {
    val builtIn by produceState(emptyList<List<CodeToken>>(), text, language, tokens == null) {
        value = if (tokens != null) emptyList() else withContext(Dispatchers.Default) { tokenizeLines(text.lines, language) }
    }
    val lineTokens = tokens ?: builtIn
    LaunchedEffect(text, state.query, state.matchCase) {
        val found = withContext(Dispatchers.Default) { findInLines(text.lines, state.query, state.matchCase) }
        state.updateMatches(found)
    }
    val shownMatches = matches ?: state.matches
    val current = if (matches != null) currentMatch else state.matches.getOrNull(state.currentMatch)
    val matchesByLine = remember(shownMatches) { shownMatches.groupBy { it.line } }
    val longest = remember(text) { longestLine(text.lines) }
    val metrics = rememberCodeMetrics(longest, text.lineCount)
    val look = rememberCodeLook(changeWash)
    val bars = rulerColors
    val charWidth = with(LocalDensity.current) { metrics.charWidth.toPx() }
    SideEffect {
        state.charWidth = charWidth
        state.lines = text.lines
    }
    val scope = rememberCoroutineScope()
    val symbols = state.symbols
    val decorated = remember(decorations) { decorations.groupBy { it.range.line } }
    val blockAt = remember(blocks, slots.lineBlock != null) { if (slots.lineBlock == null) emptyMap() else CodeBlocks.byLine(blocks, text.lineCount) }
    val decorationLook = rememberCodeDecorationLook()
    SideEffect {
        symbols.symbolAt = slots.symbolAt?.let { ask -> { spot -> ask(CodePosition(spot.line, spot.column)) } }
        symbols.onClick = slots.onSymbolClick?.let { click -> { spot, range, bounds -> click(CodeSymbolClick(CodePosition(spot.line, spot.column), range, bounds)) } }
        symbols.frameClock = scope.coroutineContext[MonotonicFrameClock]
    }
    SideEffect { symbols.card.enabled = slots.symbolCard != null && slots.symbolAt != null }
    LaunchedEffect(symbols) { symbols.followPointer() }
    LaunchedEffect(symbols) { symbols.card.follow() }
    LaunchedEffect(symbols, state.list) {
        snapshotFlow { state.list.firstVisibleItemIndex to state.list.firstVisibleItemScrollOffset }.drop(1).collect { symbols.card.dismiss() }
    }
    val allMarks = remember(marks, decorations, bars) { marks + decorations.mapNotNull { d -> bars.of(d.style)?.let { CodeMark(d.range.line, it) } } }
    val findFocus = remember { FocusRequester() }
    var opened by remember { mutableIntStateOf(0) }
    var listWidth by remember { mutableIntStateOf(0) }
    // Room a wrapped line has, in characters, beside its gutter, number and padding.
    val beside = metrics.numberWidth + StretchSpacing.space8 + if (slots.gutterMark != null) slots.gutterWidth else 0.dp
    val besidePx = with(LocalDensity.current) { beside.toPx() }
    val columns = if (charWidth > 0f) ((listWidth - besidePx) / charWidth).toInt() else 0

    val content = Modifier.onGloballyPositioned { symbols.outer = it }.then(modifier).background(Stretch.colors.surfacePanel)
        .codeFindKeys(state, scope) { opened++ }.codeSymbolKeys(symbols).onGloballyPositioned { symbols.inner = it }
    Column(content) {
        if (state.findOpen) FindBar(state, strings.find, findFocus, opened)
        Row(Modifier.weight(1f).fillMaxWidth()) {
            Box(Modifier.weight(1f).fillMaxHeight().onSizeChanged { listWidth = it.width }) {
                SelectionContainer {
                    LazyColumn(Modifier.fillMaxSize(), state.list) {
                        items(text.lineCount, key = { it }) { index ->
                            val number = index + 1
                            val tone = tones(number)
                            val block = blockAt[number]
                            when {
                                block == null -> CodeViewerLine(
                                    number, text.lines[index], lineTokens.getOrNull(index).orEmpty(), matchesByLine[number].orEmpty(), current,
                                    tone, metrics, look, state, slots, if (changeBars) changeBarOf(tone, bars) else Color.Unspecified,
                                    decorated[number].orEmpty(), decorationLook,
                                )
                                block.first == number -> CodeViewerBlock(block, tone, metrics, look, slots)
                                else -> slots.lineAnnotation?.let { annotation -> DisableSelection { annotation(number) } }
                            }
                        }
                    }
                }
                Scrollbar(state.list, Modifier.align(Alignment.CenterEnd))
            }
            if (ruler) CodeRuler(text, tones, shownMatches, current, allMarks, state, columns)
        }
        CodeDecorationTooltip(symbols)
        slots.symbolCard?.let { card -> CodeSymbolCardPopup(symbols) { rest, open -> card(CodePosition(rest.spot.line, rest.spot.column), rest.range, open) } }
    }
}

/** The bar down a line in [tone]: the strong added or removed color, or none for other lines. */
private fun changeBarOf(tone: LineTone?, colors: RulerColors): Color = when (tone) {
    LineTone.Added -> colors.added
    LineTone.Removed -> colors.removed
    else -> Color.Unspecified
}
