package dev.chieppa.stretch.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** How long after the last letter typed a new letter starts a new search rather than adding to it. */
private const val FIND_RESET_MILLIS = 1000L

/** Everything a tree reads and changes, whichever way the caller keeps it. */
internal class TreeModel(
    val expanded: Set<Any>,
    val onExpandedChange: (Any, Boolean) -> Unit,
    val selection: Set<Any>,
    val onSelectionChange: (Set<Any>) -> Unit,
    val checked: Set<Any>?,
    val onCheckedChange: (Set<Any>) -> Unit,
    val focused: Any?,
    val onFocusedChange: (Any?) -> Unit,
    val list: LazyListState,
    val onRows: (List<Any>) -> Unit,
)

/** A row drag in progress: which row and where the pointer is, measured down the whole list from its top. */
internal class TreeDragState {
    var key by mutableStateOf<Any?>(null)
    var y by mutableFloatStateOf(0f)

    /** Whether the pointer is off the tree's sides, where letting go drops nothing and a drag source outside may take it. */
    var away by mutableStateOf(false)
}

/**
 * How a tree answers clicks and keys. The tree hands it the current rows and model on every composition; the rest is
 * what a tree remembers between them: the selection's anchor, the row being renamed and the letters typed to find.
 */
internal class TreeController<T>(
    private val key: (T) -> Any,
    private val label: (T) -> String,
    private val scope: CoroutineScope,
) {
    var rows: List<TreeRow<T>> = emptyList()
    var roots: List<TreeNode<T>> = emptyList()
    lateinit var model: TreeModel
    var slot = 1f
    var onActivate: ((T) -> Unit)? = null
    var onContextMenu: ((T, Offset) -> Unit)? = null
    var canRename = false

    var renaming by mutableStateOf<Any?>(null)
    val drag = TreeDragState()
    private var anchor: Any? = null
    private var typed = ""
    private var typedAt: TimeMark? = null

    private fun isOpen(item: T) = key(item) in model.expanded

    private fun keyAt(index: Int): Any? = rows.getOrNull(index)?.let { key(it.node.item) }

    fun focusOn(index: Int) {
        val k = keyAt(index) ?: return
        model.onFocusedChange(k)
        scope.launch { model.list.reveal(index) }
    }

    /** Focus moves into the tree: onto the row it was on, or the first. */
    fun focusEntered() {
        if (model.focused == null) model.onFocusedChange(keyAt(0))
    }

    fun click(k: Any, mods: PointerKeyboardModifiers?) {
        val ctrl = mods?.isCtrlPressed == true || mods?.isMetaPressed == true
        val from = anchor
        when {
            mods?.isShiftPressed == true && from != null -> model.onSelectionChange(treeRange(rows, from, k, key))
            ctrl -> model.onSelectionChange(if (k in model.selection) model.selection - k else model.selection + k).also { anchor = k }
            else -> model.onSelectionChange(setOf(k)).also { anchor = k }
        }
        model.onFocusedChange(k)
    }

    /** How far the list has scrolled, in pixels from the top of its first row. */
    fun scrolled(): Float = model.list.firstVisibleItemIndex * slot + model.list.firstVisibleItemScrollOffset

    /**
     * Asks for the context menu of row [k] at [at], in the tree's pixels. A row outside the selection becomes the
     * selection first, so the menu acts on what it was opened on; a row inside it keeps the selection whole.
     */
    fun contextMenu(k: Any, at: Offset) {
        val menu = onContextMenu ?: return
        val row = rows.firstOrNull { key(it.node.item) == k } ?: return
        if (k !in model.selection) {
            model.onSelectionChange(setOf(k))
            anchor = k
        }
        model.onFocusedChange(k)
        menu(row.node.item, at)
    }

    fun toggleCheck(node: TreeNode<T>) {
        val checked = model.checked ?: return
        model.onCheckedChange(treeToggleCheck(node, checked, key))
    }

    /** The drop for [dragged] with the pointer [y] down the whole list. */
    fun dropAt(dragged: Any, y: Float): TreeDrop? {
        if (rows.isEmpty()) return null
        val index = floor(y / slot).toInt().coerceIn(0, rows.lastIndex)
        val fraction = ((y - index * slot) / slot).coerceIn(0f, 1f)
        return treeDrop(rows, roots, dragged, index, fraction, key, ::isOpen)
    }

    private fun finding() = typed.isNotEmpty() && typedAt?.let { it.elapsedNow().inWholeMilliseconds <= FIND_RESET_MILLIS } == true

    /** Adds a typed letter to the search and jumps to the row it finds. False when the key types nothing. */
    private fun find(event: KeyEvent, at: Int): Boolean {
        val code = event.utf16CodePoint
        if (code <= 0 || event.isCtrlPressed || event.isMetaPressed || event.isAltPressed) return false
        val char = code.toChar()
        if (char.isISOControl() || (char.isWhitespace() && !finding())) return false
        typed = if (finding()) typed + char else char.toString()
        typedAt = TimeSource.Monotonic.markNow()
        // Letters that match nothing together start over from the last one.
        val found = treeFind(rows, typed, at, label) ?: if (typed.length > 1) {
            typed = char.toString()
            treeFind(rows, typed, at, label)
        } else {
            null
        }
        found?.let { index ->
            focusOn(index)
            keyAt(index)?.let {
                model.onSelectionChange(setOf(it))
                anchor = it
            }
        }
        return true
    }

    /** Answers a key pressed while the tree has focus; true when it used it. */
    fun onKey(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyDown || renaming != null) return false
        val at = rows.indexOfFirst { key(it.node.item) == model.focused }
        val row = rows.getOrNull(at)
        if (event.opensContextMenu() && onContextMenu != null) {
            if (row == null) return false
            contextMenu(key(row.node.item), Offset(0f, (at + 1) * slot - scrolled()))
            return true
        }
        val ctrl = event.isCtrlPressed || event.isMetaPressed
        fun go(i: Int) {
            val index = i.coerceIn(0, rows.lastIndex)
            val k = keyAt(index) ?: return
            focusOn(index)
            val from = anchor
            if (event.isShiftPressed && from != null) {
                model.onSelectionChange(treeRange(rows, from, k, key))
            } else if (!ctrl) {
                model.onSelectionChange(setOf(k))
                anchor = k
            }
        }
        val page = (model.list.layoutInfo.viewportSize.height / slot).toInt().coerceAtLeast(1)
        when (event.key) {
            Key.Escape -> if (drag.key != null) drag.key = null else return false
            Key.DirectionDown -> go(if (at < 0) 0 else at + 1)
            Key.DirectionUp -> go(if (at < 0) 0 else at - 1)
            Key.MoveHome -> go(0)
            Key.MoveEnd -> go(rows.lastIndex)
            Key.PageDown -> go(if (at < 0) 0 else at + page)
            Key.PageUp -> go(if (at < 0) 0 else at - page)
            Key.DirectionRight -> when {
                row == null -> return false
                row.node.children.isNotEmpty() && !isOpen(row.node.item) -> model.onExpandedChange(key(row.node.item), true)
                row.node.children.isNotEmpty() -> go(at + 1)
            }
            Key.DirectionLeft -> when {
                row == null -> return false
                row.node.children.isNotEmpty() && isOpen(row.node.item) -> model.onExpandedChange(key(row.node.item), false)
                row.parent != null -> go(rows.indexOfFirst { it.node === row.parent })
            }
            Key.Spacebar -> when {
                finding() -> return find(event, at)
                row == null -> return false
                model.checked != null -> toggleCheck(row.node)
                else -> click(key(row.node.item), PointerKeyboardModifiers(0))
            }
            Key.Enter, Key.NumPadEnter -> {
                val activate = onActivate
                when {
                    row == null -> return false
                    activate != null -> activate(row.node.item)
                    else -> click(key(row.node.item), PointerKeyboardModifiers(0))
                }
            }
            Key.F2 -> if (canRename && row != null) renaming = key(row.node.item) else return false
            Key.A -> if (ctrl) model.onSelectionChange(rows.map { key(it.node.item) }.toSet()) else return find(event, at)
            else -> return find(event, at)
        }
        return true
    }
}
