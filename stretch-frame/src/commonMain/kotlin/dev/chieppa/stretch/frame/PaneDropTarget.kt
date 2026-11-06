package dev.chieppa.stretch.frame

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember

/**
 * Lets things outside a [PaneGrid], such as the files of a tree in a drawer, be dragged into it as tabs. Give the
 * same one to the grid's `dropTarget` and to each [paneDragSource]; the grid then shows where a dragged item would
 * land just as it does for its own tabs.
 */
@Stable
class PaneDropTarget {
    /** The drag the grid and the sources share, of the grid's own tabs and of items alike. */
    internal val drag = TabDragState()

    /** The grid taking drops, while one is composed with this target. */
    internal var grid: PaneGridContext? = null
}

/** A [PaneDropTarget] kept across recompositions. */
@Composable
fun rememberPaneDropTarget(): PaneDropTarget = remember { PaneDropTarget() }
