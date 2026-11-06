package dev.chieppa.stretch.frame

/** Where an item dragged into a [PaneGrid] from outside it, by a [paneDragSource], is let go. */
sealed interface PaneItemDrop {
    /** On a pane: into its strip, onto its middle, or onto an edge to split it, as [drop] says. */
    data class InPane(val drop: PaneDrop) : PaneItemDrop

    /** Away from the grid, where a pane window host opens a window at [bounds], under the pointer. */
    data class InWindow(val bounds: WindowBounds) : PaneItemDrop
}

/**
 * Opens [tab] where [drop] says: in a pane's strip or middle, in a new pane split off at an edge, or in a window of
 * its own. A tab already open moves there instead. An edge of a pane with no tabs opens the tab in that pane.
 */
fun PaneLayout.place(tab: String, drop: PaneItemDrop): PaneLayout = when (drop) {
    is PaneItemDrop.InWindow -> (if (leafOf(tab) != null) this else open(tab)).detach(tab, drop.bounds)
    is PaneItemDrop.InPane -> when {
        leafOf(tab) != null -> drop(tab, drop.drop)
        else -> when (val at = drop.drop) {
            is PaneDrop.Strip -> open(tab, at.leaf, at.index)
            is PaneDrop.Center -> open(tab, at.leaf)
            is PaneDrop.Edge -> open(tab, at.leaf).splitWith(tab, at.leaf, at.edge)
        }
    }
}
