package dev.chieppa.stretch.frame

import dev.chieppa.stretch.ui.controlPointer
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize
import dev.chieppa.stretch.frame.generated.Res
import dev.chieppa.stretch.frame.generated.close_tab
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchSizes
import org.jetbrains.compose.resources.stringResource

/**
 * A row of tabs for open views. Clicking a tab or pressing Enter or Space on it calls [onActivate]; its close button
 * and a middle-click call [onClose]. Left and Right move the focus between tabs, and with [onMove] Alt Left and Alt
 * Right move the focused tab, as does dragging it, with a line where it will land; Esc cancels a drag. Each tab is as
 * wide as its title asks, within a least and a greatest width. When they do not all fit, the widest shrink first, their
 * titles cut short, down to the least width; past that the strip scrolls, and the active tab scrolls into view. [focused] says whether the strip's view has the focus,
 * which colors the active tab's bar in the accent. Strips sharing a [drag] see each other's tabs; [onDragEnd] then
 * takes every drop, inside the strip or not, in place of [onMove]. [menu] is the tab's context menu and [trailing]
 * goes after the tabs.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabStrip(
    tabs: List<TabItem>,
    active: String?,
    onActivate: (String) -> Unit,
    onClose: (String) -> Unit,
    modifier: Modifier = Modifier,
    focused: Boolean = true,
    onMove: ((id: String, index: Int) -> Unit)? = null,
    drag: TabDragState = remember { TabDragState() },
    stripKey: String = DEFAULT_STRIP,
    onDragEnd: ((id: String) -> Unit)? = null,
    menu: (@Composable ColumnScope.(id: String, close: () -> Unit) -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val c = Stretch.colors
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val toScreen = LocalPaneScreen.current
    val order = tabs.map { it.id }
    val currentOrder by rememberUpdatedState(order)
    val move by rememberUpdatedState(onMove)
    val dragEnd by rememberUpdatedState(onDragEnd)
    val requesters = remember { mutableMapOf<String, FocusRequester>() }
    val target = if (drag.tab != null) drag.indexIn(stripKey, order, rtl) else null
    DisposableEffect(stripKey) { onDispose { drag.strips.remove(stripKey); drag.spans.remove(stripKey) } }

    @Composable
    fun tab(item: TabItem, index: Int) {
        val id = item.id
        val requester = requesters.getOrPut(id) { FocusRequester() }
        DisposableEffect(id) { onDispose { requesters.remove(id); drag.spans[stripKey]?.remove(id) } }
        val source = remember { MutableInteractionSource() }
        val hovered by source.collectIsHoveredAsState()
        val hasFocus by source.collectIsFocusedAsState()
        var origin by remember { mutableStateOf(Offset.Zero) }
        var size by remember { mutableStateOf(Size.Zero) }
        val into = remember { BringIntoViewRequester() }
        val isActive = id == active
        LaunchedEffect(isActive) { if (isActive) into.bringIntoView() }
        val othersBefore = order.take(index).count { it != drag.tab }
        val lineBefore = target != null && drag.tab != id && target == othersBefore
        val lineAfter = target != null && index == order.lastIndex && target == order.count { it != drag.tab } && !lineBefore
        val close = { onClose(id) }
        val closeLabel = stringResource(Res.string.close_tab, item.title)
        val face = @Composable {
            Box(
                Modifier
                    .bringIntoViewRequester(into)
                    .onGloballyPositioned {
                        origin = it.positionInWindow()
                        size = it.size.toSize()
                        drag.spans.getOrPut(stripKey) { mutableMapOf() }[id] = Placed(Rect(origin, size), toScreen)
                    }
                    .alpha(if (drag.tab == id) DRAGGED_ALPHA else 1f)
                    .focusRequester(requester)
                    .onKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                        val forward = if (rtl) Key.DirectionLeft else Key.DirectionRight
                        val back = if (rtl) Key.DirectionRight else Key.DirectionLeft
                        val step = when (event.key) {
                            forward -> 1
                            back -> -1
                            else -> 0
                        }
                        when {
                            event.key == Key.Escape && drag.tab == id -> drag.end()
                            event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.Spacebar -> onActivate(id)
                            step != 0 && event.isAltPressed -> {
                                val to = currentOrder.indexOf(id) + step
                                if (to in currentOrder.indices) move?.invoke(id, to) else return@onKeyEvent false
                            }
                            step != 0 -> currentOrder.getOrNull(currentOrder.indexOf(id) + step)?.let { requesters[it]?.requestFocus() } ?: return@onKeyEvent false
                            else -> return@onKeyEvent false
                        }
                        true
                    }
                    .hoverable(source)
                    .then(if (hasFocus) Modifier.border(Stretch.stroke, c.focus, Stretch.shapes.segShape) else Modifier)
                    .pointerInput(id) {
                        awaitPointerEventScope {
                            while (true) {
                                val press = awaitPointerEvent(PointerEventPass.Initial)
                                if (press.type != PointerEventType.Press || !press.buttons.isTertiaryPressed) continue
                                press.changes.forEach { it.consume() }
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    event.changes.forEach { it.consume() }
                                    if (event.type == PointerEventType.Release) break
                                }
                                if (item.closable) onClose(id)
                            }
                        }
                    }
                    .pointerInput(id, stripKey) {
                        detectDragGestures(
                            onDragStart = { at -> drag.begin(id, stripKey, at, origin + at, size, toScreen) },
                            onDrag = { change, amount ->
                                if (drag.tab == id) {
                                    change.consume()
                                    drag.moveBy(amount)
                                }
                            },
                            onDragEnd = {
                                if (drag.tab == id) {
                                    val end = dragEnd
                                    if (end != null) {
                                        end(id)
                                    } else {
                                        drag.indexIn(stripKey, currentOrder, rtl)?.let { move?.invoke(id, it) }
                                    }
                                }
                                drag.end()
                            },
                            onDragCancel = { drag.end() },
                        )
                    }
                    .controlPointer().clickable(source, null, role = Role.Tab) { onActivate(id) }
                    .semantics { selected = isActive; role = Role.Tab },
            ) {
                TabFace(item, isActive, focused, hovered, closeLabel, if (item.closable) close else null)
                if (lineBefore) InsertLine(Modifier.align(Alignment.CenterStart))
                if (lineAfter) InsertLine(Modifier.align(Alignment.CenterEnd))
            }
        }
        if (menu != null) ContextMenuArea({ dismiss -> menu(id, dismiss) }) { face() } else face()
    }

    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().height(StretchSizes.row), verticalAlignment = Alignment.CenterVertically) {
            BoxWithConstraints(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .onGloballyPositioned { drag.strips[stripKey] = Placed(it.boundsInWindow(), toScreen) },
            ) {
                TabRow(maxWidth, Modifier.horizontalScroll(rememberScrollState())) {
                    tabs.forEachIndexed { i, item -> key(item.id) { tab(item, i) } }
                }
                if (target != null && order.none { it != drag.tab }) InsertLine(Modifier.align(Alignment.CenterStart))
            }
            trailing()
        }
        Box(Modifier.fillMaxWidth().height(Stretch.stroke).background(c.linePanel))
    }
}

/** The line in a strip where a dragged tab will land. */
@Composable
private fun InsertLine(modifier: Modifier) {
    Box(modifier.width(Stretch.stroke * 2).height(StretchSizes.row).background(Stretch.colors.ringSelected))
}

/** The key of a [TabStrip] that shares its drag with no other strip. */
const val DEFAULT_STRIP = "strip"

/** How faint a tab shows in its strip while it is dragged. */
internal const val DRAGGED_ALPHA = 0.4f
