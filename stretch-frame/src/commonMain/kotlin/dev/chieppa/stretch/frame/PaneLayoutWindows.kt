package dev.chieppa.stretch.frame

/** A window of its own holding panes torn out of a [PaneGrid]: its tree of panes, and where it is on screen. */
data class PaneWindow(val id: String, val root: PaneNode, val bounds: WindowBounds)

/** The window [id], if it is open. */
fun PaneLayout.window(id: String): PaneWindow? = windows.firstOrNull { it.id == id }

/** The window holding [tab], or null when the tab is in the grid or not open. */
fun PaneLayout.windowOf(tab: String): PaneWindow? = windows.firstOrNull { w -> w.root.leaves().any { tab in it.tabs } }

/**
 * Tears [tab] out into a new window at [bounds], which takes the focus. A tab alone in its window already moves
 * that window to [bounds] instead.
 */
fun PaneLayout.detach(tab: String, bounds: WindowBounds): PaneLayout {
    if (leafOf(tab) == null) return this
    val own = windowOf(tab)
    if (own != null && own.root.leaves().sumOf { it.tabs.size } == 1) return moveWindow(own.id, bounds)
    val (windowId, afterWindow) = fresh(PaneLayout.WINDOW_PREFIX)
    val (leafId, afterLeaf) = afterWindow.fresh(PaneLayout.LEAF_PREFIX)
    val emptied = afterLeaf.mapTrees { tree -> tree.mapLeaves { it.withoutTab(tab) } }
    return emptied.copy(windows = emptied.windows + PaneWindow(windowId, PaneLeaf(leafId, listOf(tab)), bounds), focused = leafId).normalize()
}

/** Remembers where window [id] is. */
fun PaneLayout.moveWindow(id: String, bounds: WindowBounds): PaneLayout =
    copy(windows = windows.map { if (it.id == id) it.copy(bounds = bounds) else it })

/** Closes window [id] with its tabs. */
fun PaneLayout.closeWindow(id: String): PaneLayout {
    if (window(id) == null) return this
    return copy(windows = windows.filter { it.id != id }).normalize()
}

/**
 * Closes window [id] and puts its tabs back in the grid, at the end of the grid's focused pane, or of its first
 * pane when the focus was in a window.
 */
fun PaneLayout.returnWindow(id: String): PaneLayout {
    val gridLeaf = leaves.firstOrNull { it.id == focused } ?: leaves.first()
    return dockWindow(id, PaneDrop.Center(gridLeaf.id))
}

/**
 * Moves every tab of window [id] where [drop] says, in order, and closes the window. The tab that showed in the
 * window shows there. A drop on the window's own panes changes nothing.
 */
fun PaneLayout.dockWindow(id: String, drop: PaneDrop): PaneLayout {
    val w = window(id) ?: return this
    val windowLeaves = w.root.leaves()
    if (windowLeaves.any { it.id == drop.leaf } || leaf(drop.leaf) == null) return this
    val moving = windowLeaves.flatMap { it.tabs }
    val shown = (windowLeaves.firstOrNull { it.id == focused } ?: windowLeaves.first()).active
    if (moving.isEmpty()) return closeWindow(id)
    val placed = when (drop) {
        is PaneDrop.Strip -> moving.foldIndexed(this) { i, l, tab -> l.move(tab, drop.leaf, drop.index + i) }
        is PaneDrop.Center -> moving.fold(this) { l, tab -> l.move(tab, drop.leaf, Int.MAX_VALUE) }
        is PaneDrop.Edge -> {
            val split = splitWith(moving.first(), drop.leaf, drop.edge)
            val added = split.focused
            moving.drop(1).fold(split) { l, tab -> l.move(tab, added, Int.MAX_VALUE) }
        }
    }
    return (if (shown != null) placed.activate(shown) else placed).normalize()
}
