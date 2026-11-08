package dev.chieppa.stretch.code

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import dev.chieppa.stretch.ui.handPointer

/** Line [number] of a [CodeViewer] with its find [matches] drawn in place, [current] the current one, the app's slots and a [changeBar]. */
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
) {
    val highlights = matches.map { look.matchHighlight(it.start, it.end, it == current) }
    val annotated = remember(line, tokens, highlights, look) { codeLineText(line, tokens, look.code, highlights) }
    val click = slots.onLineNumberClick
    Column {
        CodeLineRow(
            number, annotated, metrics, look, state.wrap, state.horizontal,
            tone = tone,
            gutterWidth = slots.gutterWidth,
            gutter = slots.gutterMark?.let { mark -> { mark(number) } },
            numberModifier = if (click != null) Modifier.handPointer().clickable(role = Role.Button) { click(number) } else Modifier,
            changeBar = changeBar,
        )
        slots.lineAnnotation?.let { annotation -> DisableSelection { annotation(number) } }
    }
}
