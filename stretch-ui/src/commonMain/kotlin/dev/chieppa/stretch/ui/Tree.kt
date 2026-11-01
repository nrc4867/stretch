package dev.chieppa.stretch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key as keyed
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Icon
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import kotlin.math.floor
import kotlin.math.roundToInt

/** One node of a tree: an item and the nodes under it. */
class TreeNode<T>(val item: T, val children: List<TreeNode<T>> = emptyList())

/** A visible row of a tree: its node, how deep it sits and its parent node (null at the top). */
class TreeRow<T>(val node: TreeNode<T>, val depth: Int, val parent: TreeNode<T>?)

/** The rows a tree shows, top to bottom: every root, and the children of every expanded node. */
fun <T> flattenTree(roots: List<TreeNode<T>>, expanded: (T) -> Boolean): List<TreeRow<T>> {
    val rows = mutableListOf<TreeRow<T>>()
    fun visit(node: TreeNode<T>, depth: Int, parent: TreeNode<T>?) {
        rows += TreeRow(node, depth, parent)
        if (node.children.isNotEmpty() && expanded(node.item)) node.children.forEach { visit(it, depth + 1, node) }
    }
    roots.forEach { visit(it, 0, null) }
    return rows
}

/** Where a dragged node lands: under [parent] (null for the top level) at [index] among its other children. */
data class TreeDrop(val parent: Any?, val index: Int, val into: Boolean)

/**
 * The drop for the node [dragged] held over visible row [over] at [fraction] of its height. The top quarter puts it
 * before the row, the bottom quarter after it (or first inside an expanded parent), the middle inside it as the last
 * child. Null when it would land inside itself.
 */
fun <T> treeDrop(rows: List<TreeRow<T>>, roots: List<TreeNode<T>>, dragged: Any, over: Int, fraction: Float, key: (T) -> Any, expanded: (T) -> Boolean): TreeDrop? {
    val row = rows.getOrNull(over) ?: return null
    val draggedNode = rows.firstOrNull { key(it.node.item) == dragged }?.node ?: return null
    fun contains(node: TreeNode<T>, k: Any): Boolean = key(node.item) == k || node.children.any { contains(it, k) }
    fun siblings(parent: TreeNode<T>?) = (parent?.children ?: roots).filter { key(it.item) != dragged }
    fun beside(after: Boolean): TreeDrop? {
        val parent = row.parent
        if (parent != null && contains(draggedNode, key(parent.item))) return null
        if (key(row.node.item) == dragged) return null
        val list = siblings(parent)
        val i = list.indexOfFirst { key(it.item) == key(row.node.item) }
        return TreeDrop(parent?.let { key(it.item) }, if (after) i + 1 else i, into = false)
    }
    if (contains(draggedNode, key(row.node.item))) return null
    return when {
        fraction < 0.25f -> beside(after = false)
        fraction > 0.75f && !(row.node.children.isNotEmpty() && expanded(row.node.item)) -> beside(after = true)
        fraction > 0.75f -> TreeDrop(key(row.node.item), 0, into = true)
        else -> TreeDrop(key(row.node.item), siblings(row.node).size, into = true)
    }
}

/** The keys of the visible rows from [anchor] to [to], inclusive, in either direction. */
fun <T> treeRange(rows: List<TreeRow<T>>, anchor: Any, to: Any, key: (T) -> Any): Set<Any> {
    val a = rows.indexOfFirst { key(it.node.item) == anchor }
    val b = rows.indexOfFirst { key(it.node.item) == to }
    if (a < 0 || b < 0) return setOf(to)
    return (minOf(a, b)..maxOf(a, b)).map { key(rows[it].node.item) }.toSet()
}

private val rowHeight: Dp = 32.dp
private val indent: Dp = 16.dp

/**
 * A tree for layers, parts and anything nested. Expansion and selection are the caller's state. Click selects one
 * row, Ctrl-click adds or removes, Shift-click selects a range. Up and Down move, Right opens or steps in, Left closes
 * or steps out, Ctrl A selects every visible row. With [onRename], F2 or a double-click renames in place (Enter keeps,
 * Esc cancels). With [onMove], rows drag to a new place or parent; Esc cancels a drag. [stateText] gives the words
 * screen readers hear for an open or closed parent.
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
    stateText: (open: Boolean) -> String = { if (it) "Expanded" else "Collapsed" },
    trailing: @Composable RowScope.(T) -> Unit = {},
) {
    val c = Stretch.colors
    val isOpen: (T) -> Boolean = { key(it) in expanded }
    val rows = flattenTree(roots, isOpen)
    val current by rememberUpdatedState(rows)
    val currentRoots by rememberUpdatedState(roots)
    val currentSelection by rememberUpdatedState(selection)
    val select by rememberUpdatedState(onSelectionChange)
    val move by rememberUpdatedState(onMove)
    var focusKey by remember { mutableStateOf<Any?>(null) }
    var anchor by remember { mutableStateOf<Any?>(null) }
    var renaming by remember { mutableStateOf<Any?>(null) }
    var dragging by remember { mutableStateOf<Any?>(null) }
    var dragY by remember { mutableFloatStateOf(0f) }
    var lastClick by remember { mutableLongStateOf(0L) }
    var lastClickKey by remember { mutableStateOf<Any?>(null) }
    val treeFocus = remember { FocusRequester() }
    val slot = with(LocalDensity.current) { rowHeight.toPx() }

    fun click(k: Any, mods: PointerKeyboardModifiers?) {
        val ctrl = mods?.isCtrlPressed == true || mods?.isMetaPressed == true
        val shift = mods?.isShiftPressed == true
        when {
            shift && anchor != null -> select(treeRange(current, anchor!!, k, key))
            ctrl -> select(if (k in currentSelection) currentSelection - k else currentSelection + k).also { anchor = k }
            else -> select(setOf(k)).also { anchor = k }
        }
        focusKey = k
    }

    fun dropFor(y: Float): TreeDrop? {
        val k = dragging ?: return null
        val index = floor(y / slot).toInt().coerceIn(0, current.lastIndex)
        val fraction = ((y - index * slot) / slot).coerceIn(0f, 1f)
        return treeDrop(current, currentRoots, k, index, fraction, key) { key(it) in expanded }
    }

    Box(
        modifier
            .focusRequester(treeFocus)
            .onFocusChanged { if (it.hasFocus && focusKey == null) focusKey = current.firstOrNull()?.let { r -> key(r.node.item) } }
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || renaming != null) return@onKeyEvent false
                val at = current.indexOfFirst { key(it.node.item) == focusKey }
                val row = current.getOrNull(at)
                val ctrl = event.isCtrlPressed || event.isMetaPressed
                fun go(i: Int) {
                    val r = current.getOrNull(i) ?: return
                    val k = key(r.node.item)
                    focusKey = k
                    if (event.isShiftPressed && anchor != null) select(treeRange(current, anchor!!, k, key)) else if (!ctrl) select(setOf(k)).also { anchor = k }
                }
                when (event.key) {
                    Key.Escape -> if (dragging != null) dragging = null else return@onKeyEvent false
                    Key.DirectionDown -> go(if (at < 0) 0 else at + 1)
                    Key.DirectionUp -> go(if (at < 0) 0 else at - 1)
                    Key.DirectionRight -> when {
                        row == null -> return@onKeyEvent false
                        row.node.children.isNotEmpty() && !isOpen(row.node.item) -> onExpandedChange(key(row.node.item), true)
                        row.node.children.isNotEmpty() -> go(at + 1)
                    }
                    Key.DirectionLeft -> when {
                        row == null -> return@onKeyEvent false
                        row.node.children.isNotEmpty() && isOpen(row.node.item) -> onExpandedChange(key(row.node.item), false)
                        row.parent != null -> go(current.indexOfFirst { it.node === row.parent })
                    }
                    Key.Spacebar, Key.Enter -> row?.let { click(key(it.node.item), PointerKeyboardModifiers(0)) }
                    Key.F2 -> if (onRename != null && row != null) renaming = key(row.node.item) else return@onKeyEvent false
                    Key.A -> if (ctrl) select(current.map { key(it.node.item) }.toSet()) else return@onKeyEvent false
                    else -> return@onKeyEvent false
                }
                true
            },
    ) {
        Column(Modifier.fillMaxWidth()) {
            val draggedRow = rows.indexOfFirst { key(it.node.item) == dragging }
            val draggedDepth = rows.getOrNull(draggedRow)?.depth ?: 0
            val draggedEnd = if (draggedRow < 0) -1 else (draggedRow + 1 until rows.size).firstOrNull { rows[it].depth <= draggedDepth } ?: rows.size
            rows.forEachIndexed { index, row ->
                val item = row.node.item
                val k = key(item)
                keyed(k) {
                    val source = remember { MutableInteractionSource() }
                    val hovered by source.collectIsHoveredAsState()
                    val selected = k in selection
                    val focused = focusKey == k && keyboardInput.value
                    val ink = if (selected) c.onInk else c.ink
                    val parentRow = row.node.children.isNotEmpty()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(rowHeight)
                            .alpha(if (draggedRow >= 0 && index in draggedRow until draggedEnd) 0.4f else 1f)
                            .clip(Stretch.shapes.rowShape)
                            .background(if (selected) c.onFill else if (hovered) c.surfaceControl else Color.Transparent)
                            .then(if (focused) Modifier.border(Stretch.theme.focusStroke, c.focus, Stretch.shapes.rowShape) else Modifier)
                            .hoverable(source)
                            .semantics {
                                this.selected = selected
                                if (parentRow) stateDescription = stateText(isOpen(item))
                            }
                            .pointerInput(k) {
                                awaitEachGesture {
                                    val down = awaitFirstDown()
                                    val mods = currentEvent.keyboardModifiers
                                    treeFocus.requestFocus()
                                    if (move != null) {
                                        var overSlop = 0f
                                        val slop = awaitTouchSlopOrCancellation(down.id) { change, over -> change.consume(); overSlop = over.y }
                                        if (slop != null) {
                                            dragging = k
                                            dragY = index * slot + down.position.y + overSlop
                                            drag(slop.id) { change ->
                                                if (dragging != k) return@drag
                                                dragY += change.positionChange().y
                                                change.consume()
                                            }
                                            if (dragging == k) dropFor(dragY)?.let { move?.invoke(k, it) }
                                            dragging = null
                                            return@awaitEachGesture
                                        }
                                        if (currentEvent.changes.any { it.pressed }) return@awaitEachGesture
                                    } else if (waitForUpOrCancellation() == null) {
                                        return@awaitEachGesture
                                    }
                                    val now = currentEvent.changes.first().uptimeMillis
                                    if (onRename != null && lastClickKey == k && now - lastClick < viewConfiguration.doubleTapTimeoutMillis) {
                                        renaming = k
                                    } else {
                                        click(k, mods)
                                    }
                                    lastClick = now
                                    lastClickKey = k
                                }
                            }
                            .padding(start = indent * row.depth + 4.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            Modifier
                                .size(20.dp)
                                .then(
                                    if (parentRow) {
                                        Modifier.clip(Stretch.shapes.segShape).clickable(role = Role.Button) { onExpandedChange(k, !isOpen(item)) }
                                            .semantics { contentDescription = "${label(item)} · ${stateText(!isOpen(item))}" }
                                    } else {
                                        Modifier
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (parentRow) Icon(if (isOpen(item)) StretchIcons.ChevronDown else StretchIcons.ChevronRight, if (selected) c.onInk else c.inkMuted, size = 12.dp)
                        }
                        icon(item)?.let { Icon(it, if (selected) c.onInk else c.inkControl) }
                        if (renaming == k && onRename != null) {
                            RenameField(label(item), ink, { name -> renaming = null; if (name.isNotBlank() && name != label(item)) onRename(item, name.trim()); treeFocus.requestFocus() }) {
                                renaming = null
                                treeFocus.requestFocus()
                            }
                        } else {
                            Label(label(item), Stretch.type.body, ink, Modifier.weight(1f))
                        }
                        trailing(item)
                    }
                }
            }
        }
        val target = dragging?.let { d ->
            val i = floor(dragY / slot).toInt().coerceIn(0, rows.lastIndex)
            treeDrop(rows, roots, d, i, ((dragY - i * slot) / slot).coerceIn(0f, 1f), key) { key(it) in expanded }
        }
        if (target != null) {
            val depthOf = { parentKey: Any? -> parentKey?.let { p -> (rows.firstOrNull { key(it.node.item) == p }?.depth ?: -1) + 1 } ?: 0 }
            if (target.into) {
                val r = rows.indexOfFirst { key(it.node.item) == target.parent }
                Box(
                    Modifier
                        .offset { IntOffset(0, (r * slot).roundToInt()) }
                        .fillMaxWidth()
                        .height(rowHeight)
                        .border(2.dp, c.ringSelected, Stretch.shapes.rowShape),
                )
            } else {
                val siblings = rows.filter { (it.parent?.let { p -> key(p.item) }) == target.parent && key(it.node.item) != dragging }
                val y = when {
                    target.index < siblings.size -> rows.indexOf(siblings[target.index]) * slot
                    else -> {
                        val last = siblings.lastOrNull()
                        if (last == null) 0f else {
                            val i = rows.indexOf(last)
                            ((i + 1 until rows.size).firstOrNull { rows[it].depth <= last.depth } ?: rows.size) * slot
                        }
                    }
                }
                val depth = depthOf(target.parent)
                Row(Modifier.offset { IntOffset(0, (y - 1.dp.toPx()).roundToInt()) }.fillMaxWidth().height(2.dp)) {
                    Spacer(Modifier.width(indent * depth + 24.dp))
                    Box(Modifier.weight(1f).fillMaxHeight().clip(Stretch.shapes.pillShape).background(c.ringSelected))
                }
            }
        }
    }
}

@Composable
private fun RenameField(initial: String, ink: Color, onDone: (String) -> Unit, onCancel: () -> Unit) {
    var value by remember { mutableStateOf(TextFieldValue(initial, TextRange(0, initial.length))) }
    val focus = remember { FocusRequester() }
    var hadFocus by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { focus.requestFocus() }
    BasicTextField(
        value = value,
        onValueChange = { value = it },
        modifier = Modifier
            .focusRequester(focus)
            .onFocusChanged { if (it.isFocused) hadFocus = true else if (hadFocus) onDone(value.text) }
            .onPreviewKeyEvent {
                if (it.type == KeyEventType.KeyDown && it.key == Key.Escape) {
                    hadFocus = false
                    onCancel()
                    true
                } else {
                    false
                }
            }
            .background(Stretch.colors.surfaceFloat, Stretch.shapes.segShape)
            .border(Stretch.stroke, Stretch.colors.lineControl, Stretch.shapes.segShape)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        singleLine = true,
        textStyle = Stretch.type.body.merge(TextStyle(color = Stretch.colors.inkStrong)),
        cursorBrush = SolidColor(Stretch.colors.inkStrong),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            hadFocus = false
            onDone(value.text)
        }),
    )
}
