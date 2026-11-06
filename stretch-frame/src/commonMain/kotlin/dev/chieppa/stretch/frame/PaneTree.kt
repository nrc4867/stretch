package dev.chieppa.stretch.frame

/** Every leaf under this node, in reading order. */
internal fun PaneNode.leaves(): List<PaneLeaf> = when (this) {
    is PaneLeaf -> listOf(this)
    is PaneSplit -> children.flatMap { it.leaves() }
}

/** Every split under this node, this one included. */
internal fun PaneNode.splits(): List<PaneSplit> = when (this) {
    is PaneLeaf -> emptyList()
    is PaneSplit -> listOf(this) + children.flatMap { it.splits() }
}

internal fun PaneNode.mapLeaves(change: (PaneLeaf) -> PaneLeaf): PaneNode = when (this) {
    is PaneLeaf -> change(this)
    is PaneSplit -> copy(children = children.map { it.mapLeaves(change) })
}

internal fun PaneNode.mapSplits(change: (PaneSplit) -> PaneSplit): PaneNode = when (this) {
    is PaneLeaf -> this
    is PaneSplit -> change(copy(children = children.map { it.mapSplits(change) }))
}

/** The leaf without [tab]. When [tab] was showing, the tab after it shows, or the one before it at the end. */
internal fun PaneLeaf.withoutTab(tab: String): PaneLeaf {
    val at = tabs.indexOf(tab)
    if (at < 0) return this
    val rest = tabs - tab
    val active = if (active == tab) rest.getOrNull(at) ?: rest.lastOrNull() else active
    return copy(tabs = rest, active = active)
}

/** The leaf with [tab] at [index] (clamped), showing it. */
internal fun PaneLeaf.withTab(tab: String, index: Int): PaneLeaf {
    val rest = tabs - tab
    return copy(tabs = rest.toMutableList().apply { add(index.coerceIn(0, rest.size), tab) }, active = tab)
}

/**
 * Splits the leaf [target] at [edge], putting [added] on that side. A split along the same axis takes [added] in
 * as a sibling, sharing the target's weight; otherwise the target becomes a new split [splitId] of the two.
 */
internal fun PaneNode.splitAt(target: String, edge: PaneEdge, added: PaneLeaf, splitId: String): PaneNode {
    val axis = if (edge == PaneEdge.Start || edge == PaneEdge.End) PaneAxis.Row else PaneAxis.Column
    val before = edge == PaneEdge.Start || edge == PaneEdge.Top
    return when (this) {
        is PaneLeaf -> if (id != target) this else PaneSplit(splitId, axis, if (before) listOf(added, this) else listOf(this, added), listOf(0.5f, 0.5f))
        is PaneSplit -> {
            val at = children.indexOfFirst { it is PaneLeaf && it.id == target }
            if (at >= 0 && this.axis == axis) {
                val half = weights[at] / 2
                val insert = if (before) at else at + 1
                copy(
                    children = children.toMutableList().apply { add(insert, added) },
                    weights = weights.toMutableList().apply { set(at, half); add(insert, half) },
                )
            } else {
                copy(children = children.map { it.splitAt(target, edge, added, splitId) })
            }
        }
    }
}

/**
 * The tree with empty leaves gone, splits of one child replaced by that child, a split inside a split along the
 * same axis merged into it, and weights adding up to 1. Null when no leaf has a tab.
 */
internal fun PaneNode.pruned(): PaneNode? = when (this) {
    is PaneLeaf -> takeIf { tabs.isNotEmpty() }?.let { if (active in tabs) it else it.copy(active = tabs.first()) }
    is PaneSplit -> {
        val kept = children.indices.mapNotNull { i -> children[i].pruned()?.let { it to weights.getOrElse(i) { 0f } } }
        val merged = kept.flatMap { (child, weight) ->
            if (child is PaneSplit && child.axis == axis) {
                val sum = child.weights.sum().takeIf { it > 0f } ?: 1f
                child.children.zip(child.weights) { c, w -> c to weight * w / sum }
            } else {
                listOf(child to weight)
            }
        }
        when (merged.size) {
            0 -> null
            1 -> merged.single().first
            else -> {
                val total = merged.sumOf { it.second.toDouble() }.toFloat()
                val even = total <= 0f
                copy(children = merged.map { it.first }, weights = merged.map { if (even) 1f / merged.size else it.second / total })
            }
        }
    }
}

/**
 * Moves the border between child [index] and the next one in [split] by [fraction] of the split's length. Each of
 * the two keeps at least [minShare] of the split.
 */
internal fun PaneSplit.resized(index: Int, fraction: Float, minShare: Float): PaneSplit {
    if (index < 0 || index + 1 >= weights.size) return this
    val a = weights[index]
    val b = weights[index + 1]
    val pair = a + b
    val floor = minShare.coerceIn(0f, pair / 2)
    val newA = (a + fraction).coerceIn(floor, pair - floor)
    return copy(weights = weights.toMutableList().apply { set(index, newA); set(index + 1, pair - newA) })
}
