package dev.chieppa.stretch.code

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import dev.chieppa.stretch.theme.StretchSpacing

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
    /**
     * Names the symbol at a position, such as a name to go to the definition of, as a range on the position's own line,
     * or null for none. Asked while Ctrl (Cmd on macOS) is held over the code, and may take its time: the viewer
     * underlines the range under a pointing hand once it comes, and drops an answer for a place the pointer has left.
     * Clicking through is on when both this and [onSymbolClick] are given.
     */
    val symbolAt: (suspend (CodePosition) -> CodeRange?)? = null,
    /**
     * Called when someone Ctrl or Cmd clicks a symbol [symbolAt] named, or when [CodeViewerState.clickSymbolAtCaret]
     * finds one. A Ctrl or Cmd press in the code never starts a selection while this is on; a plain press, click and drag
     * select text as always.
     */
    val onSymbolClick: ((CodeSymbolClick) -> Unit)? = null,
    /**
     * A card about a symbol, such as its signature and documentation, opened once the pointer rests on a symbol
     * [symbolAt] names (see [CodeSymbolCards]), with where the pointer rested, the symbol's range, and the card to pin or
     * close. It draws the whole card; emitting nothing shows nothing.
     */
    val symbolCard: (@Composable (position: CodePosition, range: CodeRange, card: SymbolCard) -> Unit)? = null,
    /**
     * Draws a run of lines the viewer's `blocks` names in place of their code, such as a doc comment rendered. The run's
     * first line shows it beside that line's gutter and number; the other lines take no room, though their
     * [lineAnnotation]s still show under it.
     */
    val lineBlock: (@Composable (lines: IntRange) -> Unit)? = null,
)
