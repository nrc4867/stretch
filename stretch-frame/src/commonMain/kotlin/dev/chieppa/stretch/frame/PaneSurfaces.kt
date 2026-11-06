package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import dev.chieppa.stretch.frame.generated.Res
import dev.chieppa.stretch.frame.generated.move_back
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.theme.StretchSizes
import dev.chieppa.stretch.ui.IconButton
import dev.chieppa.stretch.ui.Tooltip
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/**
 * One tree of panes, in the grid or in a pane window, as a surface a drag can reach from any window: it reports
 * where it is on screen, and draws a tab dragged from it while the pointer stays over it.
 */
@Composable
internal fun PaneSurface(surface: String, root: PaneNode, grid: PaneGridContext, modifier: Modifier) {
    val host = LocalPaneWindows.current
    val toScreen = remember(host) { host?.let { h -> { point: Offset -> h.screenPosition(point) } } ?: SameWindow }
    var origin by remember { mutableStateOf(Offset.Zero) }
    DisposableEffect(surface) { onDispose { grid.surfaces.remove(surface) } }
    CompositionLocalProvider(LocalPaneScreen provides toScreen) {
        Box(
            modifier.onGloballyPositioned {
                origin = it.positionInWindow()
                grid.surfaces[surface] = Placed(it.boundsInWindow(), toScreen)
            },
        ) {
            PaneNodeView(root, grid, Modifier.fillMaxSize())
            val drag = grid.drag
            val dragged = drag.tab
            val from = drag.from
            if (dragged != null && from != null && grid.surfaceOf(from) == surface && overSource(grid)) {
                Box(
                    Modifier
                        .zIndex(1f)
                        .offset {
                            val at = drag.pointer - drag.grab - origin
                            IntOffset(at.x.roundToInt(), at.y.roundToInt())
                        }
                        .floating(Stretch.shapes.segShape, Stretch.shadows.pop),
                ) { DraggedTab(grid, dragged) }
            }
        }
    }
}

/** Whether the dragged tab is over the surface it came from, or there is no other window it could show in. */
internal fun overSource(grid: PaneGridContext): Boolean {
    val from = grid.drag.from ?: return true
    if (grid.host == null) return true
    return grid.surfaces[grid.surfaceOf(from)]?.onScreen()?.contains(grid.drag.screenPointer) ?: true
}

@Composable
internal fun DraggedTab(grid: PaneGridContext, tab: String) {
    TabFace(grid.item(tab), active = true, focused = true, hovered = false, closeDescription = "", onClose = null)
}

/**
 * The pane windows of [layout], through [host]: each with a header naming its showing tab, which drags the window
 * and docks it on a pane of another surface when let go there, and its own tree of panes. A window comes to the
 * front when the focus moves into it. Closing one puts its tabs back in the grid.
 */
@Composable
internal fun PaneWindows(layout: PaneLayout, host: PaneWindowHost, grid: PaneGridContext) {
    val raises = remember { mutableStateMapOf<String, Int>() }
    LaunchedEffect(layout.focused) {
        val surface = grid.surfaceOf(layout.focused)
        if (surface != GRID_SURFACE) raises[surface] = (raises[surface] ?: 0) + 1
    }
    for (window in layout.windows) {
        key(window.id) {
            val id = window.id
            val leaves = window.root.leaves()
            val shown = (leaves.firstOrNull { it.id == layout.focused } ?: leaves.first()).active
            val item = shown?.let(grid.item)
            host.PaneWindowFrame(
                title = item?.title.orEmpty(),
                bounds = window.bounds,
                raise = raises[id] ?: 0,
                onBoundsChange = { grid.change(grid.layout().moveWindow(id, it)) },
                onClose = { grid.change(grid.layout().returnWindow(id)) },
                onDragOver = { at ->
                    grid.windowDrag = at?.let { WindowDrag(id, it) }
                    at != null && grid.windowTarget(id, at) != null
                },
                onDrop = { at ->
                    grid.windowDrag = null
                    grid.windowTarget(id, at)?.let { grid.change(grid.layout().dockWindow(id, it)) }
                },
            ) {
                Column(Modifier.fillMaxSize()) {
                    DrawerHeader(item?.title.orEmpty(), item?.icon) {
                        val back = stringResource(Res.string.move_back)
                        Tooltip(back) { IconButton(StretchIcons.Restore, back, { grid.change(grid.layout().returnWindow(id)) }, size = StretchSizes.seg) }
                    }
                    PaneSurface(id, window.root, grid, Modifier.weight(1f).fillMaxWidth())
                }
            }
        }
    }
}

/** The dragged tab in a window of its own while it is away from the surface it came from. */
@Composable
internal fun PaneDragFloat(host: PaneWindowHost, grid: PaneGridContext) {
    val drag = grid.drag
    val dragged = drag.tab ?: return
    val size = with(LocalDensity.current) { drag.size.width.toDp() }
    val at = if (overSource(grid)) null else drag.toScreen(drag.pointer - drag.grab)
    host.DragFloat(at, size) {
        Box(Modifier.floating(Stretch.shapes.segShape, Stretch.shadows.pop)) { DraggedTab(grid, dragged) }
    }
}
