package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

/** The surface of the grid in the app's window, beside the surfaces of pane windows, which go by window id. */
internal const val GRID_SURFACE = "grid"

/** A pane window being dragged by its title bar, and the pointer on screen. */
internal data class WindowDrag(val window: String, val at: Offset)

/**
 * What every pane of one [PaneGrid] shares, in its own window and in pane windows: the layout and its changes, the
 * app's tabs and content, the tab drag, and where each pane's body and each surface is on screen, so a drag can land
 * on any of them.
 */
internal class PaneGridContext(
    val layout: () -> PaneLayout,
    val change: (PaneLayout) -> Unit,
    val item: (String) -> TabItem,
    val close: (String) -> Unit,
    var menu: (@Composable ColumnScope.(id: String, close: () -> Unit) -> Unit)?,
    var empty: @Composable BoxScope.() -> Unit,
    var actions: @Composable RowScope.(String) -> Unit,
    var content: @Composable BoxScope.(String) -> Unit,
    val drag: TabDragState,
    val holder: SaveableStateHolder,
    val rtl: Boolean,
    val host: PaneWindowHost?,
) {
    /** Each pane's body by leaf id. */
    val bodies = mutableMapOf<String, Placed>()

    /** The grid and each pane window's panes, by [GRID_SURFACE] or window id. */
    val surfaces = mutableMapOf<String, Placed>()

    /** The pane window being dragged by its title bar, if any. */
    var windowDrag by mutableStateOf<WindowDrag?>(null)

    /** The surface holding pane [leaf]: [GRID_SURFACE] or a window id. */
    fun surfaceOf(leaf: String): String =
        layout().windows.firstOrNull { w -> w.root.leaves().any { it.id == leaf } }?.id ?: GRID_SURFACE

    /** Whether [at], on screen, is over no surface of the grid. */
    fun outside(at: Offset): Boolean = surfaces.values.none { it.onScreen().contains(at) }

    /** Where the dragged tab or window lands if let go now, or null where it would change nothing. */
    fun target(): PaneDrop? {
        val tab = drag.tab
        if (tab != null) return tabTarget(tab, drag.screenPointer)
        val moving = windowDrag ?: return null
        return windowTarget(moving.window, moving.at)
    }

    /** Where [tab] lands if let go at [at] on screen. */
    fun tabTarget(tab: String, at: Offset): PaneDrop? {
        val current = layout()
        val source = current.leafOf(tab)
        val all = current.trees().flatMap { it.leaves() }
        for (leaf in all) drag.indexAt(leaf.id, leaf.tabs, rtl, at, tab)?.let { return PaneDrop.Strip(leaf.id, it) }
        val (leaf, edge) = bodyAt(all, at) ?: return null
        val own = source?.id == leaf.id
        return when {
            edge == null -> if (own) null else PaneDrop.Center(leaf.id)
            own && leaf.tabs.size == 1 -> null
            else -> PaneDrop.Edge(leaf.id, edge)
        }
    }

    /** Where window [window] docks if let go at [at] on screen: on the body of a pane outside it. */
    fun windowTarget(window: String, at: Offset): PaneDrop? {
        val current = layout()
        val outside = current.trees().flatMap { it.leaves() } - current.window(window)?.root?.leaves().orEmpty().toSet()
        val (leaf, edge) = bodyAt(outside, at) ?: return null
        return if (edge == null) PaneDrop.Center(leaf.id) else PaneDrop.Edge(leaf.id, edge)
    }

    /** Lets the dragged [tab] go: where [target] says, or into a window of its own when let go away from the grid. */
    fun finish(tab: String) {
        val at = drag.screenPointer
        val drop = tabTarget(tab, at)
        val current = layout()
        when {
            drop != null -> change(current.drop(tab, drop))
            host != null && outside(at) -> {
                val size = current.leafOf(tab)?.let { bodies[it.id]?.onScreen() }
                val bounds = windowUnderPointer(at, size?.width ?: DEFAULT_WINDOW_WIDTH, size?.height ?: DEFAULT_WINDOW_HEIGHT)
                change(current.detach(tab, bounds))
            }
        }
    }

    /** The window [tab] opens in from its menu: over its pane, a little in from the pane's corner. */
    fun newWindowBounds(tab: String): WindowBounds {
        val body = layout().leafOf(tab)?.let { bodies[it.id]?.onScreen() }
        return if (body == null) {
            WindowBounds(WINDOW_INSET, WINDOW_INSET, DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT)
        } else {
            WindowBounds(body.left + WINDOW_INSET, body.top + WINDOW_INSET, body.width, body.height)
        }
    }

    private fun bodyAt(leaves: List<PaneLeaf>, at: Offset): Pair<PaneLeaf, PaneEdge?>? {
        for (leaf in leaves) {
            val body = bodies[leaf.id]?.onScreen() ?: continue
            if (body.contains(at)) return leaf to edgeAt(body, at, rtl)
        }
        return null
    }

    companion object {
        const val DEFAULT_WINDOW_WIDTH = 800f
        const val DEFAULT_WINDOW_HEIGHT = 600f

        /** How far in from its pane's corner a window opened from the tab menu starts. */
        const val WINDOW_INSET = 32f
    }
}
