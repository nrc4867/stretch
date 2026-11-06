package dev.chieppa.stretch.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * What a [Tree] shows open, selected and checked, and where it has scrolled. Changing a property updates the tree, and
 * the tree changes them as a person uses it, so an app reads them to follow along.
 */
@Stable
class TreeState(expanded: Set<Any> = emptySet(), selection: Set<Any> = emptySet(), checked: Set<Any> = emptySet()) {
    /** Keys of the open parents. */
    var expanded by mutableStateOf(expanded)

    /** Keys of the selected rows. */
    var selection by mutableStateOf(selection)

    /**
     * Keys of the checked nodes without children. A parent's box follows from them: checked, unchecked or mixed (see
     * [treeChecks]).
     */
    var checked by mutableStateOf(checked)

    /** The key of the row keyboard focus is on, if any. */
    var focused by mutableStateOf<Any?>(null)
        internal set

    val list = LazyListState()

    /** Keys of the rows laid out now, top to bottom, for scrolling to one. */
    internal var rowKeys by mutableStateOf(emptyList<Any>())

    fun setExpanded(key: Any, open: Boolean) {
        expanded = if (open) expanded + key else expanded - key
    }

    fun <T> expandAll(roots: List<TreeNode<T>>, key: (T) -> Any) {
        expanded = expanded + treeParents(roots, key)
    }

    fun collapseAll() {
        expanded = emptySet()
    }

    /** Opens every parent above [target], so its row shows. */
    fun <T> expandTo(roots: List<TreeNode<T>>, target: Any, key: (T) -> Any) {
        treePath(roots, target, key)?.let { expanded = expanded + it }
    }

    /** Scrolls the row for [key] into view, if it is showing. Call [expandTo] first for a row inside a closed parent. */
    suspend fun reveal(key: Any) {
        val index = rowKeys.indexOf(key)
        if (index >= 0) list.reveal(index)
    }
}

@Composable
fun rememberTreeState(expanded: Set<Any> = emptySet(), selection: Set<Any> = emptySet(), checked: Set<Any> = emptySet()): TreeState =
    remember { TreeState(expanded, selection, checked) }

/** Text a [Tree] gives screen readers. Apps pass their own for other languages. */
data class TreeStrings(
    val expanded: String = "Expanded",
    val collapsed: String = "Collapsed",
) {
    fun state(open: Boolean): String = if (open) expanded else collapsed
}
