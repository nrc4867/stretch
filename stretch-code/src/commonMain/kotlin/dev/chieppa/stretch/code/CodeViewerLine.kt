package dev.chieppa.stretch.code

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import dev.chieppa.stretch.ui.handPointer

/**
 * Line [number] of a [CodeViewer] with its find [matches] drawn in place, [current] the current one, the app's slots, a
 * [changeBar], its [decorations] and the symbol under a held Ctrl or Cmd.
 */
@Composable
internal fun CodeViewerLine(
    number: Int,
    line: String,
    tokens: List<CodeToken>,
    matches: List<CodeRange>,
    current: CodeRange?,
    tone: LineTone?,
    metrics: CodeMetrics,
    look: CodeLook,
    state: CodeViewerState,
    slots: CodeViewerSlots,
    changeBar: Color,
    decorations: List<CodeDecoration>,
    decorationLook: CodeDecorationLook,
) {
    val symbols = state.symbols
    val paint = remember(line, decorations, decorationLook) { decorationPaint(line, decorations, decorationLook) }
    val highlights = paint.washes + matches.map { look.matchHighlight(it.start, it.end, it == current) }
    val link by remember(symbols, number) { derivedStateOf { symbols.link?.range?.takeIf { it.line == number } } }
    val annotated = remember(line, tokens, highlights, look, link, decorationLook) {
        val text = codeLineText(line, tokens, look.code, highlights)
        link?.let { withCodeLink(text, line, it.start, it.end, decorationLook.link) } ?: text
    }
    val geometry = remember(line) { CodeLineGeometry(line) }
    val keys = remember(number) { listOf(CodeLineKey(Unit, number)) }
    val input = rememberCodeSymbolInput(symbols, keys, geometry)
    val click = slots.onLineNumberClick
    Column {
        CodeLineRow(
            number, annotated, metrics, look, state.wrap, state.horizontal,
            tone = tone,
            gutterWidth = slots.gutterWidth,
            gutter = slots.gutterMark?.let { mark -> { mark(number) } },
            numberModifier = if (click != null) Modifier.handPointer().clickable(role = Role.Button) { click(number) } else Modifier,
            changeBar = changeBar,
            underlines = paint.underlines,
            textModifier = input,
            onTextLayout = { geometry.layout = it },
        )
        slots.lineAnnotation?.let { annotation -> DisableSelection { annotation(number) } }
    }
}
