package dev.chieppa.stretch.frame

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.frame.generated.Res
import dev.chieppa.stretch.frame.generated.close
import dev.chieppa.stretch.frame.generated.dock
import dev.chieppa.stretch.frame.generated.dock_left
import dev.chieppa.stretch.frame.generated.dock_right
import dev.chieppa.stretch.frame.generated.drawer_menu
import dev.chieppa.stretch.frame.generated.pop_out
import dev.chieppa.stretch.frame.generated.rail_bottom
import dev.chieppa.stretch.frame.generated.rail_top
import dev.chieppa.stretch.frame.generated.reset_layout
import dev.chieppa.stretch.frame.generated.resize
import dev.chieppa.stretch.frame.generated.resize_panel
import dev.chieppa.stretch.frame.generated.resize_split
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.theme.StretchSizes
import dev.chieppa.stretch.ui.IconButton
import dev.chieppa.stretch.ui.Tooltip
import dev.chieppa.stretch.ui.focusHome
import org.jetbrains.compose.resources.stringResource

/**
 * One drawer an app offers: its rail icon, title and contents. [actions] go in its header before the menu; [badge]
 * goes on its rail button (see [dev.chieppa.stretch.ui.badgeCount]). [content]
 * is told where the drawer is showing: a tall side column or the wide bottom panel, so it can lay out for either.
 */
class DrawerSpec(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val count: String? = null,
    val badge: String? = null,
    val actions: @Composable RowScope.() -> Unit = {},
    val content: @Composable ColumnScope.(DrawerPosition) -> Unit,
)

/**
 * The window every app starts from: top bar, a rail on each edge, docked drawers that push the viewport over, the
 * viewport, and a bottom panel for drawers in the rails' bottom groups. The bottom panel spans the window between
 * the rails, split in two when both sides have a drawer open there. Drawers always stay docked; in a window too narrow
 * for their widths they are shown narrower, down to [FrameLayout.MIN_WIDTH], so the viewport keeps
 * [FrameLayout.MIN_VIEWPORT] as long as it can. Drawers resize from their
 * inner edge, the split between stacked drawers and the bottom panel's
 * top edge; rail buttons reorder by dragging. Where the platform has a [LocalDrawerWindows] host, a drawer's menu
 * pops it out into a window of its own; its rail button stays lit and brings the window to the front, and docking
 * or closing the window puts it back. The layout is the app's state: every change comes back through
 * [onLayoutChange]. Pass a [keyCast] to show key presses over the viewport and a [statusBar] for the window's foot.
 */
@Composable
fun StretchApp(
    layout: FrameLayout,
    onLayoutChange: (FrameLayout) -> Unit,
    drawers: List<DrawerSpec>,
    onResetLayout: () -> Unit,
    modifier: Modifier = Modifier,
    keyCast: KeyCastState? = null,
    statusBar: (@Composable () -> Unit)? = null,
    topBar: @Composable () -> Unit,
    viewport: @Composable BoxScope.() -> Unit,
) {
    val c = Stretch.colors
    val specs = drawers.associateBy { it.id }
    val current by rememberUpdatedState(layout)
    val change by rememberUpdatedState(onLayoutChange)
    val windows = LocalDrawerWindows.current
    val raises = remember { mutableStateMapOf<String, Int>() }

    @Composable
    fun rail(side: Side) = Rail(
        side = side,
        items = layout.on(side).mapNotNull { placement -> specs[placement.id]?.let { RailItem(it.id, it.icon, it.title, placement.bottom, it.badge) } },
        open = layout.on(side).filter { it.open || it.detached }.map { it.id }.toSet(),
        onToggle = { id ->
            if (current[id]?.detached == true) raises[id] = (raises[id] ?: 0) + 1 else change(current.toggle(id))
        },
        onMove = { id, drop -> change(current.move(id, side, drop.bottom, drop.index)) },
    )

    @Composable
    fun drawer(placement: DrawerPlacement, style: DrawerStyle, modifier: Modifier) {
        val spec = specs.getValue(placement.id)
        Drawer(
            title = spec.title,
            icon = spec.icon,
            style = style,
            count = spec.count,
            modifier = modifier,
            actions = {
                spec.actions(this)
                DrawerMenuButton(placement, canPopOut = windows != null, { change(it(current)) }, onResetLayout)
                if (placement.detached) {
                    val dock = stringResource(Res.string.dock)
                    Tooltip(dock) {
                        IconButton(
                            if (placement.side == Side.Left) StretchIcons.DockLeft else StretchIcons.DockRight,
                            dock,
                            { change(current.dockBack(placement.id)) },
                            size = StretchSizes.seg,
                        )
                    }
                } else {
                    IconButton(StretchIcons.Minus, stringResource(Res.string.close), { change(current.close(placement.id)) }, size = StretchSizes.seg)
                }
            },
            content = { spec.content(this, placement.position) },
        )
    }

    @Composable
    fun bottomPanel() {
        val left = layout.openBottom(Side.Left)?.takeIf { it.id in specs }
        val right = layout.openBottom(Side.Right)?.takeIf { it.id in specs }
        if (left == null && right == null) return
        val density = LocalDensity.current.density
        var rowWidth by remember { mutableFloatStateOf(1f) }
        var startHeight by remember { mutableFloatStateOf(0f) }
        var startShare by remember { mutableFloatStateOf(0f) }
        Column(Modifier.fillMaxWidth().height(layout.bottomHeight.dp)) {
            EdgeHandle(vertical = true, stringResource(Res.string.resize_panel), { startHeight = current.bottomHeight }) { px ->
                change(current.resizeBottom(startHeight - px / density))
            }
            Row(Modifier.fillMaxSize().onSizeChanged { rowWidth = it.width.toFloat().coerceAtLeast(1f) }) {
                if (left != null) drawer(left, DrawerStyle.Docked, Modifier.weight(if (right != null) layout.bottomSplit else 1f).fillMaxHeight())
                if (left != null && right != null) {
                    EdgeHandle(vertical = false, stringResource(Res.string.resize_split), { startShare = current.bottomSplit }) { px ->
                        change(current.splitBottom(startShare + px / rowWidth))
                    }
                }
                if (right != null) drawer(right, DrawerStyle.Docked, Modifier.weight(if (left != null) 1 - layout.bottomSplit else 1f).fillMaxHeight())
            }
        }
    }

    @Composable
    fun stack(side: Side, fit: Float) {
        val open = layout.openOn(side).filter { it.id in specs }
        if (open.isEmpty()) return
        val width = open.first().width
        var columnHeight by remember { mutableFloatStateOf(1f) }
        val resize: (Float) -> Unit = { w -> change(open.fold(current) { l, d -> l.resize(d.id, w) }) }
        val shown = maxOf(width * fit, minOf(width, FrameLayout.MIN_WIDTH))
        Row(Modifier.width(shown.dp).fillMaxHeight()) {
            if (side == Side.Right) ResizeHandle(width, -1f, resize)
            Column(Modifier.weight(1f).fillMaxHeight().onSizeChanged { columnHeight = it.height.toFloat().coerceAtLeast(1f) }) {
                open.forEachIndexed { i, placement ->
                    if (i > 0) {
                        val upper = open[i - 1].id
                        SplitHandle({ current }) { start, dy -> change(start.resizeSplit(upper, placement.id, dy / columnHeight)) }
                    }
                    drawer(placement, DrawerStyle.Docked, Modifier.weight(placement.height))
                }
            }
            if (side == Side.Left) ResizeHandle(width, 1f, resize)
        }
    }

    Column(modifier.focusHome().fillMaxSize().background(c.surfaceCanvas).then(if (keyCast != null) Modifier.keyCast(keyCast) else Modifier)) {
        topBar()
        BoxWithConstraints(Modifier.weight(1f)) {
            val fit = layout.sideFit(maxWidth.value - 2 * StretchSizes.rail.value, specs.keys)
            Row(Modifier.fillMaxSize()) {
                rail(Side.Left)
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        stack(Side.Left, fit)
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            viewport()
                            if (keyCast != null) KeyCast(keyCast, Modifier.align(Alignment.BottomEnd).padding(12.dp))
                        }
                        stack(Side.Right, fit)
                    }
                    bottomPanel()
                }
                rail(Side.Right)
            }
        }
        statusBar?.invoke()
    }

    if (windows != null) {
        for (placement in layout.detached) {
            val spec = specs[placement.id] ?: continue
            key(placement.id) {
                windows.DrawerWindow(
                    title = spec.title,
                    bounds = placement.window,
                    side = placement.side,
                    defaultWidth = placement.width,
                    raise = raises[placement.id] ?: 0,
                    onBoundsChange = { change(current.moveWindow(placement.id, it)) },
                    onClose = { change(current.dockBack(placement.id)) },
                ) { drawer(placement, DrawerStyle.Docked, Modifier.fillMaxSize()) }
            }
        }
    }
}

@Composable
private fun DrawerMenuButton(placement: DrawerPlacement, canPopOut: Boolean, change: ((FrameLayout) -> FrameLayout) -> Unit, onReset: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(StretchIcons.More, stringResource(Res.string.drawer_menu), { open = true }, size = StretchSizes.seg)
        Menu(open, { open = false }) {
            if (canPopOut && !placement.detached) {
                MenuItem(stringResource(Res.string.pop_out), { change { it.popOut(placement.id) }; open = false })
                MenuDivider()
            }
            MenuItem(stringResource(Res.string.dock_left), { change { it.dock(placement.id, Side.Left) }; open = false }, icon = StretchIcons.DockLeft)
            MenuItem(stringResource(Res.string.dock_right), { change { it.dock(placement.id, Side.Right) }; open = false }, icon = StretchIcons.DockRight)
            val toBottom = !placement.bottom
            MenuItem(
                stringResource(if (toBottom) Res.string.rail_bottom else Res.string.rail_top),
                { change { it.move(placement.id, placement.side, toBottom, Int.MAX_VALUE) }; open = false },
                icon = if (toBottom) StretchIcons.DockBottom else if (placement.side == Side.Left) StretchIcons.DockLeft else StretchIcons.DockRight,
            )
            MenuDivider()
            MenuItem(stringResource(Res.string.reset_layout), { onReset(); open = false })
        }
    }
}

/**
 * Follows a drag on a handle that moves as it resizes things. Positions are taken in the window, so the handle
 * moving under the pointer does not eat the drag. Reports the distance from where the drag started.
 */
private fun Modifier.windowDrag(vertical: Boolean, onStart: () -> Unit, onEnd: () -> Unit = {}, onDrag: (Float) -> Unit): Modifier = composed {
    var origin by remember { mutableStateOf(Offset.Zero) }
    var startAt by remember { mutableFloatStateOf(0f) }
    val start by rememberUpdatedState(onStart)
    val drag by rememberUpdatedState(onDrag)
    val end by rememberUpdatedState(onEnd)
    onGloballyPositioned { origin = it.positionInWindow() }.pointerInput(vertical) {
        fun along(local: Offset) = if (vertical) origin.y + local.y else origin.x + local.x
        detectDragGestures(
            onDragStart = { at ->
                startAt = along(at)
                start()
            },
            onDragEnd = { end() },
            onDragCancel = { end() },
        ) { change, _ ->
            change.consume()
            drag(along(change.position) - startAt)
        }
    }
}

/**
 * The drag strip on a panel's inner edge: across the panel when [vertical] (dragged up and down), along it
 * otherwise. Reports the distance dragged in pixels since [onStart].
 */
@Composable
private fun EdgeHandle(vertical: Boolean, description: String, onStart: () -> Unit, onDrag: (Float) -> Unit) {
    val c = Stretch.colors
    val drag by rememberUpdatedState(onDrag)
    var dragging by remember { mutableStateOf(false) }
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val active = hovered || dragging
    val line = if (active) 2.dp else Stretch.stroke
    Box(
        (if (vertical) Modifier.fillMaxWidth().height(6.dp) else Modifier.width(6.dp).fillMaxHeight())
            .background(c.surfacePanel)
            .hoverable(source)
            .pointerHoverIcon(if (vertical) resizeUpDown else resizeSideways)
            .windowDrag(vertical, onStart = { onStart(); dragging = true }, onEnd = { dragging = false }) { drag(it) }
            .semantics { contentDescription = description },
    ) {
        Box(
            (if (vertical) Modifier.fillMaxWidth().height(line) else Modifier.width(line).fillMaxHeight())
                .align(Alignment.Center)
                .background(if (active) c.ringSelected else c.linePanel),
        )
    }
}

/** The inner edge of a side column. Reports the new width in dp; [sign] is -1 when dragging left grows it. */
@Composable
private fun ResizeHandle(width: Float, sign: Float, onResize: (Float) -> Unit) {
    val density = LocalDensity.current.density
    val currentWidth by rememberUpdatedState(width)
    var start by remember { mutableFloatStateOf(0f) }
    EdgeHandle(vertical = false, stringResource(Res.string.resize), { start = currentWidth }) { px -> onResize(start + sign * px / density) }
}

/** The split between two stacked drawers. Reports the layout at the start of the drag and the distance in pixels. */
@Composable
private fun SplitHandle(layout: () -> FrameLayout, onResize: (start: FrameLayout, dy: Float) -> Unit) {
    val description = stringResource(Res.string.resize_split)
    val resize by rememberUpdatedState(onResize)
    var start by remember { mutableStateOf<FrameLayout?>(null) }
    Box(
        Modifier
            .fillMaxWidth()
            .pointerHoverIcon(resizeUpDown)
            .windowDrag(vertical = true, onStart = { start = layout() }) { dy -> start?.let { resize(it, dy) } }
            .semantics { contentDescription = description },
    ) { DrawerSplit() }
}

/**
 * The slot the app renders into: the well color with a grid. Floating UI goes in its corners, 12dp in:
 * [topStart] for the toolbar, [topEnd] for view tools like the [ViewNavigator], [bottomStart] and [bottomEnd] for
 * readouts. When a viewport is too narrow for a pair side by side, the end one moves under (or above) the start one
 * instead of overlapping it. [content] draws the scene and anything else, such as toasts; a native GPU surface can
 * sit there instead of drawn content.
 */
@Composable
fun Viewport(
    modifier: Modifier = Modifier,
    grid: Boolean = true,
    topStart: (@Composable () -> Unit)? = null,
    topEnd: (@Composable () -> Unit)? = null,
    bottomStart: (@Composable () -> Unit)? = null,
    bottomEnd: (@Composable () -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
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
        ViewportCorners(topStart, topEnd, bottomStart, bottomEnd)
    }
}

/** Places the viewport's corner UI, moving an end corner off its start corner when the two would overlap. */
@Composable
private fun ViewportCorners(
    topStart: (@Composable () -> Unit)?,
    topEnd: (@Composable () -> Unit)?,
    bottomStart: (@Composable () -> Unit)?,
    bottomEnd: (@Composable () -> Unit)?,
) {
    val none: @Composable () -> Unit = {}
    Layout(
        contents = listOf(topStart ?: none, topEnd ?: none, bottomStart ?: none, bottomEnd ?: none),
        modifier = Modifier.fillMaxSize(),
    ) { slots, constraints ->
        val inset = 12.dp.roundToPx()
        val gap = 8.dp.roundToPx()
        val room = Constraints(maxWidth = (constraints.maxWidth - 2 * inset).coerceAtLeast(0), maxHeight = (constraints.maxHeight - 2 * inset).coerceAtLeast(0))
        val (ts, te, bs, be) = slots.map { it.firstOrNull()?.measure(room) }
        fun fit(start: Placeable?, end: Placeable) = start == null || start.width + gap + end.width <= room.maxWidth
        layout(constraints.maxWidth, constraints.maxHeight) {
            val right = constraints.maxWidth - inset
            val bottom = constraints.maxHeight - inset
            ts?.place(inset, inset)
            te?.place(right - te.width, if (fit(ts, te)) inset else inset + ts!!.height + gap)
            bs?.place(inset, bottom - bs.height)
            be?.place(right - be.width, if (fit(bs, be)) bottom - be.height else bottom - bs!!.height - gap - be.height)
        }
    }
}
