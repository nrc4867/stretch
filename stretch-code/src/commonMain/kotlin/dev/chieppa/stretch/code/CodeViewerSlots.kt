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
)
