package dev.chieppa.stretch.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key as keyed
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalDensity
import dev.chieppa.stretch.theme.StretchSizes

/**
 * A tree for layers, parts, folders and anything nested, kept in [state]. Given a bounded height, only the rows on
 * screen are built, so it stays smooth with many thousands; inside a column that scrolls, every row is. Click selects one row, Ctrl-click (Cmd-click on macOS) adds or removes,
 * Shift-click selects a range. Up and Down move, Home and End go to the ends, Page Up and Page Down move a screen,
 * Right opens or steps in, Left closes or steps out, Ctrl A selects every visible row, and typing a name's first
 * letters jumps to it. Enter or a double-click calls [onActivate], to open what the row stands for.
 *
 * With [checkboxes] each row has a box; checking a parent checks everything under it, and a parent whose children
 * differ shows the mixed state. Space checks the row with focus. With [onRename], F2 renames in place (Enter keeps,
 * Esc cancels), as does a double-click when there is no [onActivate]. With [onMove], rows drag to a new place or
 * parent; Esc cancels a drag, and so does letting go off the tree's sides.
 *
 * [onContextMenu] gets a row's item and where to open its menu, in the tree's pixels, on a right-click, a long press
 * with a pen or finger, or Shift F10 or the Menu key for the focused row; a row outside the selection becomes the
 * selection first, so the app reads the selection to see what the menu acts on. Show the menu with stretch-frame's
 * `ContextMenuPopup` composed beside the tree in a box of the same bounds. [rowModifier] adds the app's own modifier
 * to each row, around the tree's own pointer handling, such as stretch-frame's `paneDragSource` to drag rows out:
 * with [onMove] too, a row dragged off the tree's sides leaves the tree's own drag for it. [leading] goes before the
 * label, for a swatch or anything [icon] cannot draw, and [trailing] after it, for counts and badges.
 */
@Composable
fun <T> Tree(
    roots: List<TreeNode<T>>,
    state: TreeState,
    key: (T) -> Any,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    icon: (T) -> ImageVector? = { null },
    checkboxes: Boolean = false,
    onActivate: ((T) -> Unit)? = null,
    onRename: ((T, String) -> Unit)? = null,
    onMove: ((key: Any, drop: TreeDrop) -> Unit)? = null,
    strings: TreeStrings = TreeStrings(),
    onContextMenu: ((item: T, at: Offset) -> Unit)? = null,
    rowModifier: (T) -> Modifier = { Modifier },
    leading: @Composable RowScope.(T) -> Unit = {},
    trailing: @Composable RowScope.(T) -> Unit = {},
) {
    TreeView(
        roots, key, label,
        TreeModel(
            expanded = state.expanded, onExpandedChange = state::setExpanded,
            selection = state.selection, onSelectionChange = { state.selection = it },
            checked = if (checkboxes) state.checked else null, onCheckedChange = { state.checked = it },
            focused = state.focused, onFocusedChange = { state.focused = it },
            list = state.list, onRows = { state.rowKeys = it },
        ),
        modifier, icon, onActivate, onRename, onMove, strings::state, onContextMenu, rowModifier, leading, trailing,
    )
}

/**
 * A tree whose expansion and selection are the caller's own state, given as sets and changed through the callbacks.
 * It behaves as the [TreeState] form does, without checkboxes; [stateText] gives the words screen readers hear for an
 * open or closed parent, and [onContextMenu] and [rowModifier] work as they do there.
 */
@Composable
fun <T> Tree(
    roots: List<TreeNode<T>>,
    key: (T) -> Any,
    label: (T) -> String,
    expanded: Set<Any>,
    onExpandedChange: (key: Any, expanded: Boolean) -> Unit,
    selection: Set<Any>,
    onSelectionChange: (Set<Any>) -> Unit,
    modifier: Modifier = Modifier,
    icon: (T) -> ImageVector? = { null },
    onRename: ((T, String) -> Unit)? = null,
    onMove: ((key: Any, drop: TreeDrop) -> Unit)? = null,
    stateText: (open: Boolean) -> String = TreeStrings()::state,
    onContextMenu: ((item: T, at: Offset) -> Unit)? = null,
    rowModifier: (T) -> Modifier = { Modifier },
    leading: @Composable RowScope.(T) -> Unit = {},
    trailing: @Composable RowScope.(T) -> Unit = {},
) {
    var focused by remember { mutableStateOf<Any?>(null) }
    TreeView(
        roots, key, label,
        TreeModel(
            expanded, onExpandedChange, selection, onSelectionChange, checked = null, onCheckedChange = {},
            focused = focused, onFocusedChange = { focused = it }, list = rememberLazyListState(), onRows = {},
        ),
        modifier, icon, null, onRename, onMove, stateText, onContextMenu, rowModifier, leading, trailing,
    )
}

@Composable
private fun <T> TreeView(
    roots: List<TreeNode<T>>,
    key: (T) -> Any,
    label: (T) -> String,
    model: TreeModel,
    modifier: Modifier,
    icon: (T) -> ImageVector?,
    onActivate: ((T) -> Unit)?,
    onRename: ((T, String) -> Unit)?,
    onMove: ((key: Any, drop: TreeDrop) -> Unit)?,
    stateText: (open: Boolean) -> String,
    onContextMenu: ((item: T, at: Offset) -> Unit)?,
    rowModifier: (T) -> Modifier,
    leading: @Composable RowScope.(T) -> Unit,
    trailing: @Composable RowScope.(T) -> Unit,
) {
    val expanded = model.expanded
    val rows = remember(roots, expanded) { flattenTree(roots) { key(it) in expanded } }
    val checks = remember(roots, model.checked) { model.checked?.let { treeChecks(roots, it, key) } }
    SideEffect { model.onRows(rows.map { key(it.node.item) }) }
    val scope = rememberCoroutineScope()
    val control = remember { TreeController(key, label, scope) }
    control.rows = rows
    control.roots = roots
    control.model = model
    control.slot = with(LocalDensity.current) { StretchSizes.ctl.toPx() }
    control.onActivate = onActivate
    control.onContextMenu = onContextMenu
    control.canRename = onRename != null
    val drag = control.drag
    var treeFocused by remember { mutableStateOf(false) }
    val treeFocus = remember { FocusRequester() }

    Box(
        modifier
            .focusRequester(treeFocus)
            .onFocusChanged {
                treeFocused = it.hasFocus
                if (it.hasFocus) control.focusEntered()
            }
            .focusable()
            .onKeyEvent(control::onKey),
    ) {
        val draggedRow = if (drag.away) -1 else rows.indexOfFirst { key(it.node.item) == drag.key }
        val draggedDepth = rows.getOrNull(draggedRow)?.depth ?: 0
        val draggedEnd = if (draggedRow < 0) -1 else (draggedRow + 1 until rows.size).firstOrNull { rows[it].depth <= draggedDepth } ?: rows.size
        @Composable
        fun rowView(index: Int, row: TreeRow<T>) {
            val item = row.node.item
            val k = key(item)
            val open = k in expanded
            TreeRowView(
                row = row,
                rowKey = k,
                index = index,
                label = label(item),
                icon = icon(item),
                open = open,
                selected = k in model.selection,
                focused = model.focused == k && treeFocused,
                checkable = checks != null,
                check = checks?.get(k),
                faded = draggedRow >= 0 && index in draggedRow until draggedEnd,
                renaming = control.renaming == k && onRename != null,
                stateText = stateText,
                slot = control.slot,
                drag = if (onMove != null) drag else null,
                onPress = { treeFocus.requestFocus() },
                onClick = { mods -> control.click(k, mods) },
                onDoubleClick = when {
                    onActivate != null -> { -> onActivate(item) }
                    onRename != null -> { -> control.renaming = k }
                    else -> null
                },
                onToggleOpen = { model.onExpandedChange(k, !open) },
                onToggleCheck = { control.toggleCheck(row.node) },
                onDrop = { y -> control.dropAt(k, y)?.let { onMove?.invoke(k, it) } },
                onRenamed = { name ->
                    control.renaming = null
                    if (name != null && name.isNotBlank() && name != label(item)) onRename?.invoke(item, name.trim())
                    treeFocus.requestFocus()
                },
                onContextPress = if (onContextMenu == null) null else { at ->
                    treeFocus.requestFocus()
                    control.contextMenu(k, Offset(at.x, index * control.slot - control.scrolled() + at.y))
                },
                modifier = rowModifier(item),
                leading = { leading(item) },
                trailing = { trailing(item) },
            )
        }
        // Inside a column that scrolls, the height has no bound and every row is laid out; otherwise only those on screen.
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            if (constraints.hasBoundedHeight) {
                LazyColumn(Modifier.fillMaxWidth(), model.list) {
                    itemsIndexed(rows, key = { _, row -> key(row.node.item) }) { index, row -> rowView(index, row) }
                }
            } else {
                Column(Modifier.fillMaxWidth()) {
                    rows.forEachIndexed { index, row -> keyed(key(row.node.item)) { rowView(index, row) } }
                }
            }
        }
        val dragged = drag.key
        val target = dragged?.takeUnless { drag.away }?.let { control.dropAt(it, drag.y) }
        if (target != null) {
            TreeDropMark(target, rows, key, dragged, control.slot, control.scrolled(), Modifier.align(Alignment.TopStart))
        }
    }
}
