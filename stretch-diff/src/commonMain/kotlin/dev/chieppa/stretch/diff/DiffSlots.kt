package dev.chieppa.stretch.diff

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import dev.chieppa.stretch.code.LineTone
import dev.chieppa.stretch.theme.StretchSpacing

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
