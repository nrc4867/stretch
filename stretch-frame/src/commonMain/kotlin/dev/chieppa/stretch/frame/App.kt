package dev.chieppa.stretch.frame

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.frame.generated.Res
import dev.chieppa.stretch.frame.generated.close
import dev.chieppa.stretch.frame.generated.dock_left
import dev.chieppa.stretch.frame.generated.dock_right
import dev.chieppa.stretch.frame.generated.drawer_menu
import dev.chieppa.stretch.frame.generated.reset_layout
import dev.chieppa.stretch.frame.generated.resize
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.theme.StretchSizes
import dev.chieppa.stretch.ui.IconButton
import org.jetbrains.compose.resources.stringResource

/** One drawer an app offers: its rail icon, title and contents. [actions] go in its header before the menu. */
class DrawerSpec(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val count: String? = null,
    val actions: @Composable RowScope.() -> Unit = {},
    val content: @Composable ColumnScope.() -> Unit,
)

/**
 * The window every app starts from: top bar, a rail on each edge, docked drawers that push the viewport over, and
 * the viewport. Under 1100dp wide, open drawers float over the viewport instead. The layout is the app's state:
 * every change comes back through [onLayoutChange].
 */
@Composable
fun StretchApp(
    layout: FrameLayout,
    onLayoutChange: (FrameLayout) -> Unit,
    drawers: List<DrawerSpec>,
    onResetLayout: () -> Unit,
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit,
    viewport: @Composable BoxScope.() -> Unit,
) {
    val c = Stretch.colors
    val specs = drawers.associateBy { it.id }
    val current by rememberUpdatedState(layout)
    val change by rememberUpdatedState(onLayoutChange)

    @Composable
    fun rail(side: Side) = Rail(
        side = side,
        items = layout.on(side).mapNotNull { placement -> specs[placement.id]?.let { RailItem(it.id, it.icon, it.title) } },
        open = layout.openOn(side).map { it.id }.toSet(),
        onToggle = { change(current.toggle(it)) },
    )

    @Composable
    fun stack(side: Side, style: DrawerStyle, modifier: Modifier) {
        val open = layout.openOn(side).filter { it.id in specs }
        if (open.isEmpty()) return
        val width = open.first().width
        Row(modifier.width(width.dp).fillMaxHeight()) {
            if (side == Side.Right) ResizeHandle { dx -> change(open.fold(current) { l, d -> l.resize(d.id, width - dx) }) }
            Column(Modifier.weight(1f).fillMaxHeight()) {
                open.forEachIndexed { i, placement ->
                    if (i > 0) DrawerSplit()
                    val spec = specs.getValue(placement.id)
                    Drawer(
                        title = spec.title,
                        icon = spec.icon,
                        style = style,
                        count = spec.count,
                        modifier = Modifier.weight(1f),
                        actions = {
                            spec.actions(this)
                            DrawerMenuButton(placement, { change(it(current)) }, onResetLayout)
                            IconButton(StretchIcons.Minus, stringResource(Res.string.close), { change(current.close(placement.id)) }, size = StretchSizes.seg)
                        },
                        content = spec.content,
                    )
                }
            }
            if (side == Side.Left) ResizeHandle { dx -> change(open.fold(current) { l, d -> l.resize(d.id, width + dx) }) }
        }
    }

    Column(modifier.fillMaxSize().background(c.surfaceCanvas)) {
        topBar()
        BoxWithConstraints(Modifier.weight(1f)) {
            val narrow = maxWidth.value < FrameLayout.NARROW_WINDOW
            Row(Modifier.fillMaxSize()) {
                rail(Side.Left)
                if (!narrow) stack(Side.Left, DrawerStyle.Docked, Modifier)
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    viewport()
                    if (narrow) {
                        stack(Side.Left, DrawerStyle.Floating, Modifier.align(Alignment.TopStart).padding(8.dp))
                        stack(Side.Right, DrawerStyle.Floating, Modifier.align(Alignment.TopEnd).padding(8.dp))
                    }
                }
                if (!narrow) stack(Side.Right, DrawerStyle.Docked, Modifier)
                rail(Side.Right)
            }
        }
    }
}

@Composable
private fun DrawerMenuButton(placement: DrawerPlacement, change: ((FrameLayout) -> FrameLayout) -> Unit, onReset: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(StretchIcons.More, stringResource(Res.string.drawer_menu), { open = true }, size = StretchSizes.seg)
        Menu(open, { open = false }) {
            MenuItem(stringResource(Res.string.dock_left), { change { it.dock(placement.id, Side.Left) }; open = false }, icon = StretchIcons.DockLeft)
            MenuItem(stringResource(Res.string.dock_right), { change { it.dock(placement.id, Side.Right) }; open = false }, icon = StretchIcons.DockRight)
            MenuDivider()
            MenuItem(stringResource(Res.string.reset_layout), { onReset(); open = false })
        }
    }
}

/** The drag strip on a drawer column's inner edge; reports horizontal movement in dp. */
@Composable
private fun ResizeHandle(onDrag: (Float) -> Unit) {
    val c = Stretch.colors
    val density = LocalDensity.current.density
    val description = stringResource(Res.string.resize)
    Box(
        Modifier
            .width(5.dp)
            .fillMaxHeight()
            .background(c.surfacePanel)
            .pointerHoverIcon(PointerIcon.Crosshair)
            .draggable(rememberDraggableState { onDrag(it / density) }, Orientation.Horizontal)
            .semantics { contentDescription = description },
    ) {
        Box(Modifier.align(Alignment.Center).width(Stretch.stroke).fillMaxHeight().background(c.linePanel))
    }
}

/**
 * The slot the app renders into: the well color with a grid. Children align to its corners for the toolbar, view
 * tools, readouts and a bottom bar. A native GPU surface can sit in [content] instead of drawn content.
 */
@Composable
fun Viewport(modifier: Modifier = Modifier, grid: Boolean = true, content: @Composable BoxScope.() -> Unit) {
    val c = Stretch.colors
    Box(modifier.fillMaxSize().background(c.surfaceWell)) {
        if (grid) {
            Canvas(Modifier.fillMaxSize()) {
                val step = 32.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawLine(c.gridLine, Offset(x, 0f), Offset(x, size.height), 1f)
                    x += step
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(c.gridLine, Offset(0f, y), Offset(size.width, y), 1f)
                    y += step
                }
            }
        }
        content()
    }
}
