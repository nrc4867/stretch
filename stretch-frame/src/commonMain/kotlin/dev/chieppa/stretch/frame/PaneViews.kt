package dev.chieppa.stretch.frame

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import dev.chieppa.stretch.frame.generated.Res
import dev.chieppa.stretch.frame.generated.move_to_window
import dev.chieppa.stretch.frame.generated.resize_panes
import dev.chieppa.stretch.frame.generated.tab_close
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/** The smallest weight a pane is laid out with, so a pane never vanishes from the row or column. */
private const val LEAST_WEIGHT = 0.0001f

/** One node of a pane tree: a split with its children and the borders between them, or a pane. */
@Composable
internal fun PaneNodeView(node: PaneNode, grid: PaneGridContext, modifier: Modifier) {
    when (node) {
        is PaneLeaf -> PaneLeafView(node, grid, modifier)
        is PaneSplit -> PaneSplitView(node, grid, modifier)
    }
}

@Composable
private fun PaneSplitView(split: PaneSplit, grid: PaneGridContext, modifier: Modifier) {
    val row = split.axis == PaneAxis.Row
    val density = LocalDensity.current.density
    var length by remember { mutableFloatStateOf(1f) }
    val description = stringResource(Res.string.resize_panes)

    @Composable
    fun border(index: Int) {
        var start by remember { mutableStateOf<PaneLayout?>(null) }
        var startLength by remember { mutableFloatStateOf(1f) }
        EdgeHandle(vertical = !row, description, { start = grid.layout(); startLength = length }) { px ->
            val from = start ?: return@EdgeHandle
            val along = if (row && grid.rtl) -px else px
            grid.change(from.resize(split.id, index, along / startLength, PaneLayout.MIN_PANE * density / startLength))
        }
    }

    @Composable
    fun children(childModifier: (Float) -> Modifier) {
        split.children.forEachIndexed { i, child ->
            key(child.id) {
                if (i > 0) border(i - 1)
                PaneNodeView(child, grid, childModifier(split.weights.getOrElse(i) { 0f }.coerceAtLeast(LEAST_WEIGHT)))
            }
        }
    }

    if (row) {
        Row(modifier.onSizeChanged { length = it.width.toFloat().coerceAtLeast(1f) }) {
            children { Modifier.weight(it).fillMaxHeight() }
        }
    } else {
        Column(modifier.onSizeChanged { length = it.height.toFloat().coerceAtLeast(1f) }) {
            children { Modifier.weight(it).fillMaxWidth() }
        }
    }
}

@Composable
private fun PaneLeafView(leaf: PaneLeaf, grid: PaneGridContext, modifier: Modifier) {
    val c = Stretch.colors
    val layout = grid.layout()
    val toScreen = LocalPaneScreen.current
    var body by remember { mutableStateOf(IntSize.Zero) }
    Column(
        modifier.pointerInput(leaf.id) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                if (grid.layout().focused != leaf.id) grid.change(grid.layout().focus(leaf.id))
            }
        },
    ) {
        TabStrip(
            tabs = leaf.tabs.map(grid.item),
            active = leaf.active,
            onActivate = { grid.change(grid.layout().activate(it)) },
            onClose = grid.close,
            focused = layout.focused == leaf.id,
            onMove = { id, index -> grid.change(grid.layout().move(id, leaf.id, index)) },
            drag = grid.drag,
            stripKey = leaf.id,
            onDragEnd = grid::finish,
            menu = { id, dismiss -> PaneTabMenu(grid, id, dismiss) },
        )
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
                .background(c.surfacePanel)
                .onSizeChanged { body = it }
                .onGloballyPositioned { grid.bodies[leaf.id] = Placed(it.boundsInWindow(), toScreen) },
        ) {
            val active = leaf.active
            if (active != null) {
                key(active) { grid.holder.SaveableStateProvider(active) { Box(Modifier.fillMaxSize()) { grid.content(this, active) } } }
            } else {
                Box(Modifier.fillMaxSize()) { grid.empty(this) }
            }
            val target = grid.target()
            if (target != null && target.leaf == leaf.id && target !is PaneDrop.Strip) {
                val area = dropPreview(Rect(0f, 0f, body.width.toFloat(), body.height.toFloat()), (target as? PaneDrop.Edge)?.edge, grid.rtl)
                val shape = Stretch.shapes.rowShape
                Box(
                    Modifier
                        .offset { IntOffset(area.left.roundToInt(), area.top.roundToInt()) }
                        .size(with(LocalDensity.current) { area.size.toDpSize() })
                        .background(c.ringSelected.copy(alpha = PREVIEW_ALPHA), shape)
                        .border(Stretch.stroke * 2, c.ringSelected, shape),
                )
            }
        }
    }
}

/** A tab's context menu: close it, move it to a window of its own where there is a host, then the app's items. */
@Composable
private fun ColumnScope.PaneTabMenu(grid: PaneGridContext, id: String, dismiss: () -> Unit) {
    MenuItem(stringResource(Res.string.tab_close), { dismiss(); grid.close(id) }, icon = StretchIcons.Close)
    if (grid.host != null) {
        MenuItem(stringResource(Res.string.move_to_window), {
            dismiss()
            grid.change(grid.layout().detach(id, grid.newWindowBounds(id)))
        }, icon = StretchIcons.ExternalLink)
    }
    val more = grid.menu ?: return
    MenuDivider()
    more(id, dismiss)
}

/** How strongly a drop preview tints the pane under it. */
private const val PREVIEW_ALPHA = 0.14f
