package dev.chieppa.stretch.diff

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import dev.chieppa.stretch.code.CodeRange
import dev.chieppa.stretch.code.LineTone
import dev.chieppa.stretch.code.SymbolCard
import dev.chieppa.stretch.theme.StretchSpacing

/** What the app can add to a [DiffViewer]. Everything is optional. */
class DiffSlots(
    /** Called when someone clicks a line's number, with the side of the number clicked. */
    val onLineClick: ((DiffLineRef) -> Unit)? = null,
    /**
     * Shown under a line, across the viewer, such as a thread of notes. Emit nothing for a line with none. Unified, it
     * is called once per line, with the line's own side: old for a removed line, new otherwise. Side by side, it is
     * called for each side of a row that has a line there, the old side first, each with its own side.
     */
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
    /**
     * Names the symbol at a position, such as a name to go to the definition of, as a range on the position's own line
     * of the same side of the same file, or null for none. Asked while Ctrl (Cmd on macOS) is held over the code, and may
     * take its time: the viewer underlines the range under a pointing hand once it comes, and drops an answer for a place
     * the pointer has left. Clicking through is on when both this and [onSymbolClick] are given.
     */
    val symbolAt: (suspend (DiffPosition) -> CodeRange?)? = null,
    /**
     * Called when someone Ctrl or Cmd clicks a symbol [symbolAt] named, or when [DiffViewerState.clickSymbolAtCaret]
     * finds one. A Ctrl or Cmd press in the code never starts a selection while this is on; a plain press, click and
     * drag select text as always.
     */
    val onSymbolClick: ((DiffSymbolClick) -> Unit)? = null,
    /**
     * A card about a symbol, such as its signature and documentation, opened once the pointer rests on a symbol
     * [symbolAt] names (see `CodeSymbolCards`), with where the pointer rested, the symbol's range, and the card to pin or
     * close. It draws the whole card; emitting nothing shows nothing.
     */
    val symbolCard: (@Composable (position: DiffPosition, range: CodeRange, card: SymbolCard) -> Unit)? = null,
    /**
     * Draws a [DiffBlock] the viewer's `blocks` names in place of its code, such as a doc comment rendered, on [side]:
     * once in one column, and on each side of a split view. The block's first row shows it beside that row's gutter and
     * numbers; its other rows take no room, though their [lineAnnotation]s still show under it.
     */
    val lineBlock: (@Composable (block: DiffBlock, side: DiffSide) -> Unit)? = null,
)
