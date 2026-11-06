package dev.chieppa.stretch.ui

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

/**
 * The visible row whose label starts with [query], ignoring case, searching down from [from] and wrapping around.
 * A query of one letter repeated ("ttt") steps through the rows starting with that letter, one per press, the way
 * file lists do. Null when no row matches.
 */
fun <T> treeFind(rows: List<TreeRow<T>>, query: String, from: Int, label: (T) -> String): Int? {
    if (query.isEmpty() || rows.isEmpty()) return null
    val repeated = query.length > 1 && query.all { it.equals(query[0], ignoreCase = true) }
    val prefix = if (repeated) query.substring(0, 1) else query
    // A growing query keeps the current row while it still matches; a fresh or repeated letter moves on.
    val start = if (query.length > 1 && !repeated) from.coerceAtLeast(0) else from + 1
    for (step in rows.indices) {
        val at = (start + step).mod(rows.size)
        if (label(rows[at].node.item).startsWith(prefix, ignoreCase = true)) return at
    }
    return null
}

/** The keys of the nodes above [target], outermost first, or null when no node has that key. */
fun <T> treePath(roots: List<TreeNode<T>>, target: Any, key: (T) -> Any): List<Any>? {
    fun search(nodes: List<TreeNode<T>>, above: List<Any>): List<Any>? {
        for (node in nodes) {
            val k = key(node.item)
            if (k == target) return above
            search(node.children, above + k)?.let { return it }
        }
        return null
    }
    return search(roots, emptyList())
}

/** The keys of every node with children, for expanding the whole tree. */
fun <T> treeParents(roots: List<TreeNode<T>>, key: (T) -> Any): Set<Any> = buildSet {
    fun visit(node: TreeNode<T>) {
        if (node.children.isEmpty()) return
        add(key(node.item))
        node.children.forEach(::visit)
    }
    roots.forEach(::visit)
}

/** The keys of the nodes without children at or under [node]. */
fun <T> treeLeaves(node: TreeNode<T>, key: (T) -> Any): List<Any> = buildList {
    fun visit(n: TreeNode<T>) {
        if (n.children.isEmpty()) add(key(n.item)) else n.children.forEach(::visit)
    }
    visit(node)
}

/**
 * Each node's checkbox, by key, when [checked] holds the keys of checked nodes without children: true when every node
 * without children under it is checked, false when none is, and null, the mixed state, otherwise.
 */
fun <T> treeChecks(roots: List<TreeNode<T>>, checked: Set<Any>, key: (T) -> Any): Map<Any, Boolean?> {
    val states = HashMap<Any, Boolean?>()
    fun visit(node: TreeNode<T>): Boolean? {
        val state = if (node.children.isEmpty()) {
            key(node.item) in checked
        } else {
            val below = node.children.map(::visit)
            when {
                below.all { it == true } -> true
                below.all { it == false } -> false
                else -> null
            }
        }
        states[key(node.item)] = state
        return state
    }
    roots.forEach(::visit)
    return states
}

/**
 * [checked] after clicking [node]'s checkbox: everything under it becomes checked, or unchecked when it all was
 * already. Mixed checks all, as a checkbox does.
 */
fun <T> treeToggleCheck(node: TreeNode<T>, checked: Set<Any>, key: (T) -> Any): Set<Any> {
    val leaves = treeLeaves(node, key)
    return if (leaves.all { it in checked }) checked - leaves.toSet() else checked + leaves
}
