package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/**
 * Panes of tabs for the viewport, split side by side and stacked as [layout] says. Each pane has a [TabStrip] over
 * the [content] of its showing tab; the app gives each tab id its [item] (title, icon, modified) and its content.
 * Tabs drag within and between strips, and onto a pane's body: near an edge the pane splits and the tab gets the new
 * half, in the middle it joins the pane, with a preview of where it lands. Borders between panes drag to resize them,
 * down to [PaneLayout.MIN_PANE]. Closing a tab calls [onClose], which closes it in the layout unless the app passes
 * its own, say to ask about unsaved changes first; a pane left empty collapses. A press in a pane focuses it. Each
 * tab's content keeps its saveable state while another tab shows. [tabMenu] adds items to a tab's context menu,
 * [empty] fills a pane with no tabs, and [paneActions] goes at the end of each pane's strip, for buttons such as split
 * or close others that act on the pane whose id it gets. Every change comes back through [onChange].
 *
 * With a [dropTarget], items outside the grid carrying a [paneDragSource] drag in too, with the same preview: onto a
 * strip or a pane's middle they open as a tab there, onto an edge in a new pane split off, and let go away from the
 * grid, where there is a host, in a window of their own; without a host, away from the grid nothing happens. The tab
 * opens through [PaneLayout.place] under the id the source gave, unless the app passes [onDropItem] to make the tab
 * itself, say to create its view first, and then call `place`.
 *
 * Where the platform has a [LocalPaneWindows] host, a tab let go away from the grid, or sent by its menu's Move to new
 * window, opens in a window of its own under the pointer, which holds panes like the grid's and is a full window of
 * the app. Tabs drag between windows and the grid alike, and dragging a pane window by its header onto a pane docks
 * its tabs there; closing it puts them back in the grid. Without a host, windows in [layout] go back into the grid.
 */
@Composable
fun PaneGrid(
    layout: PaneLayout,
    onChange: (PaneLayout) -> Unit,
    item: (id: String) -> TabItem,
    modifier: Modifier = Modifier,
    onClose: ((id: String) -> Unit)? = null,
    tabMenu: (@Composable ColumnScope.(id: String, close: () -> Unit) -> Unit)? = null,
    empty: @Composable BoxScope.() -> Unit = {},
    paneActions: @Composable RowScope.(paneId: String) -> Unit = {},
    dropTarget: PaneDropTarget? = null,
    onDropItem: ((id: String, drop: PaneItemDrop) -> Unit)? = null,
    content: @Composable BoxScope.(id: String) -> Unit,
) {
    val current by rememberUpdatedState(layout)
    val change by rememberUpdatedState(onChange)
    val close by rememberUpdatedState(onClose)
    val items by rememberUpdatedState(item)
    val drag = dropTarget?.drag ?: remember { TabDragState() }
    val holder = rememberSaveableStateHolder()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val host = LocalPaneWindows.current
    val grid = remember(drag, holder, rtl, host) {
        PaneGridContext(
            layout = { current },
            change = { change(it) },
            item = { items(it) },
            close = { id -> close?.invoke(id) ?: change(current.close(id)) },
            menu = null,
            empty = {},
            actions = {},
            content = {},
            drag = drag,
            holder = holder,
            rtl = rtl,
            host = host,
        )
    }.also {
        it.menu = tabMenu
        it.empty = empty
        it.actions = paneActions
        it.dropItem = onDropItem
        it.content = content
    }
    ForgetClosedTabs(layout.tabs, holder::removeState)
    if (dropTarget != null) {
        DisposableEffect(dropTarget, grid) {
            dropTarget.grid = grid
            onDispose { if (dropTarget.grid === grid) dropTarget.grid = null }
        }
    }

    if (host != null) {
        PaneSurface(GRID_SURFACE, layout.root, grid, modifier)
        PaneWindows(layout, host, grid)
        PaneDragFloat(host, grid)
    } else {
        PaneSurface(GRID_SURFACE, layout.root, grid, modifier)
        val windows = layout.windows.map { it.id }
        LaunchedEffect(windows) { if (windows.isNotEmpty()) change(windows.fold(current) { l, w -> l.returnWindow(w) }) }
    }
}

/** Forgets the saved state of tabs that are closed. */
@Composable
private fun ForgetClosedTabs(open: List<String>, forget: (String) -> Unit) {
    val seen = remember { mutableSetOf<String>() }
    SideEffect {
        val gone = seen - open.toSet()
        gone.forEach(forget)
        seen.removeAll(gone)
        seen.addAll(open)
    }
}
