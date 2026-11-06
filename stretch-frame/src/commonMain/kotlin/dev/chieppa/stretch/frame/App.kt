package dev.chieppa.stretch.frame

import androidx.compose.foundation.background
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.frame.generated.Res
import dev.chieppa.stretch.frame.generated.close
import dev.chieppa.stretch.frame.generated.dock
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
 * goes on its rail button (see [dev.chieppa.stretch.ui.badgeCount]) and [keys] in its rail button's tooltip. [content]
 * is told where the drawer is showing: a tall side column or the wide bottom panel, so it can lay out for either.
 * [headerIcon] is the icon in its header and [railLabel] the name on its rail button, when those differ from the
 * [icon] and [title], as when a drawer's title follows the armed tool. A drawer not [enabled] shows on the rail, faint, and does not open: a place kept for what is not built yet.
 * With [scrolls] false the drawer's contents, docked or in their own window, get the height under the header and do
 * their own scrolling, so a `LazyColumn` or `Tree` in them builds only the rows on screen.
 */
class DrawerSpec(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val count: String? = null,
    val badge: String? = null,
    val keys: List<String>? = null,
    val headerIcon: ImageVector = icon,
    val railLabel: String = title,
    val enabled: Boolean = true,
    val actions: @Composable RowScope.() -> Unit = {},
    val scrolls: Boolean = true,
    val content: @Composable ColumnScope.(DrawerPosition) -> Unit,
)

/**
 * The window every app starts from: top bar, a rail on each edge, docked drawers that push the viewport over, the
 * viewport, and a bottom panel for drawers in the rails' bottom groups. The bottom panel spans the window between
 * the rails, split in two when both sides have a drawer open there. Drawers always stay docked; in a window too narrow
 * for their widths they are shown narrower, down to [FrameLayout.MIN_WIDTH], so the viewport keeps
 * [FrameLayout.MIN_VIEWPORT] as long as it can. Drawers resize from their
 * inner edge, the split between stacked drawers and the bottom panel's
 * top edge; rail buttons drag to any slot on either rail. Where the platform has a [LocalDrawerWindows] host, a
 * drawer's menu pops it out into a window of its own, as does letting its rail button go away from the rails, which
 * opens the window there; its rail button stays lit and brings the window to the front, and docking or closing the
 * window puts it back, as does dragging the window onto either rail. The layout is the app's state: every change comes back through
 * [onLayoutChange]. Pass a [keyCast] to show key presses over the viewport and a [statusBar] for the window's foot.
 * [topStrip] is a strip across the whole window under the [topBar], above the rails, drawers and viewport, such as a
 * row of sources the window is showing.
 *
 * With [placeNewDrawer], [drawers] can change while the app runs: the layout follows the list, through
 * [onLayoutChange]. A drawer new to it goes where [placeNewDrawer] says, one that left and comes back returns where it
 * was, and one no longer listed leaves the rail, its window closing. Without it, the layout lists the drawers and specs
 * it has no placement for do not show.
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
    placeNewDrawer: ((DrawerSpec) -> DrawerPlacement)? = null,
    topStrip: (@Composable () -> Unit)? = null,
    topBar: @Composable () -> Unit,
    viewport: @Composable BoxScope.() -> Unit,
) {
    val c = Stretch.colors
    val specs = drawers.associateBy { it.id }
    val current by rememberUpdatedState(layout)
    val change by rememberUpdatedState(onLayoutChange)
    val windows = LocalDrawerWindows.current
    val raises = remember { mutableStateMapOf<String, Int>() }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val railDrag = remember { RailDragState() }
    if (placeNewDrawer != null) {
        val ids = drawers.map { it.id }
        SideEffect {
            val synced = current.syncDrawers(ids) { id -> placeNewDrawer(specs.getValue(id)) }
            if (synced != current) change(synced)
        }
    }

    @Composable
    fun rail(side: Side) = Rail(
        side = side,
        modifier = if (windows == null) Modifier else Modifier.onGloballyPositioned { windows.railBounds(side, it.boundsInWindow()) },
        windowDrop = windows?.dockTarget?.takeIf { it.side == side }?.bottom,
        items = layout.on(side).mapNotNull { placement -> specs[placement.id]?.let { RailItem(it.id, it.icon, it.railLabel, placement.inRailFoot, it.badge, it.keys, it.enabled) } },
        open = layout.on(side).filter { it.open || it.detached }.map { it.id }.toSet(),
        onToggle = { id ->
            if (current[id]?.detached == true) raises[id] = (raises[id] ?: 0) + 1 else change(current.toggle(id))
        },
        onMove = { id, to, drop -> change(current.move(id, to, drop.bottom, drop.index)) },
        drag = railDrag,
        canPopOut = { id -> windows != null && current[id]?.detached == false && specs[id]?.enabled == true },
        onPopOut = { id, pointer ->
            val placement = current[id]
            if (windows != null && placement != null) {
                val bounds = windowUnderPointer(windows.screenPosition(pointer), placement.windowWidth, placement.windowHeight)
                change(current.popOut(id).moveWindow(id, bounds))
            }
        },
    )

    @Composable
    fun drawer(placement: DrawerPlacement, style: DrawerStyle, modifier: Modifier) {
        val spec = specs.getValue(placement.id)
        Drawer(
            title = spec.title,
            icon = spec.headerIcon,
            style = style,
            count = spec.count,
            modifier = modifier,
            actions = {
                spec.actions(this)
                DrawerMenuButton(placement, canPopOut = windows != null, rtl, { change(it(current)) }, onResetLayout)
                if (placement.detached) {
                    val dock = stringResource(Res.string.dock)
                    Tooltip(dock) {
                        IconButton(
                            if ((placement.side == Side.Left) != rtl) StretchIcons.DockLeft else StretchIcons.DockRight,
                            dock,
                            { change(current.dockBack(placement.id)) },
                            size = StretchSizes.seg,
                        )
                    }
                } else {
                    IconButton(StretchIcons.Minus, stringResource(Res.string.close), { change(current.close(placement.id)) }, size = StretchSizes.seg)
                }
            },
            scrolls = spec.scrolls,
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
                        change(current.splitBottom(startShare + (if (rtl) -px else px) / rowWidth))
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
            // The handle is on the inner edge; dragging away from the drawer's own edge grows it.
            val grow = if ((side == Side.Left) != rtl) 1f else -1f
            if (side == Side.Right) ResizeHandle(width, grow, resize)
            Column(Modifier.weight(1f).fillMaxHeight().onSizeChanged { columnHeight = it.height.toFloat().coerceAtLeast(1f) }) {
                open.forEachIndexed { i, placement ->
                    if (i > 0) {
                        val upper = open[i - 1].id
                        SplitHandle({ current }) { start, dy -> change(start.resizeSplit(upper, placement.id, dy / columnHeight)) }
                    }
                    drawer(placement, DrawerStyle.Docked, Modifier.weight(placement.height))
                }
            }
            if (side == Side.Left) ResizeHandle(width, grow, resize)
        }
    }

    Column(modifier.focusHome().fillMaxSize().background(c.surfaceCanvas).then(if (keyCast != null) Modifier.keyCast(keyCast) else Modifier)) {
        topBar()
        topStrip?.invoke()
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
                    onDock = { side, bottom -> change(current.move(placement.id, side, bottom, Int.MAX_VALUE)) },
                ) { drawer(placement, DrawerStyle.Docked, Modifier.fillMaxSize()) }
            }
        }
    }
}
