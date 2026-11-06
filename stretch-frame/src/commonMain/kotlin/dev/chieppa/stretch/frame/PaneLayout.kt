package dev.chieppa.stretch.frame

/**
 * The panes of a [PaneGrid]: a tree of splits whose leaves hold tabs, by the app's ids, the [windows] torn out of
 * it, each with a tree of its own, and the pane that has the focus, in the grid or in a window. A tab is open in one
 * pane at a time. Every change returns a new layout, so the app keeps it as state and can
 * save it; it holds only strings, numbers, enums and lists. Changes that take tabs out of panes collapse the panes left
 * empty, and a split left with one child gives way to it; the grid always keeps one pane, empty or not.
 */
data class PaneLayout(
    val root: PaneNode = PaneLeaf(FIRST_LEAF),
    val focused: String = root.leaves().first().id,
    val next: Int = 1,
    val windows: List<PaneWindow> = emptyList(),
) {
    init {
        val all = allLeaves()
        val leafIds = all.map { it.id }
        require(leafIds.toSet().size == leafIds.size) { "Pane ids must be unique" }
        val tabIds = all.flatMap { it.tabs }
        require(tabIds.toSet().size == tabIds.size) { "A tab is open in one pane at a time" }
        require(windows.map { it.id }.toSet().size == windows.size) { "Window ids must be unique" }
    }

    /** The panes in the grid, in reading order; panes in [windows] are not among them. */
    val leaves: List<PaneLeaf> get() = root.leaves()

    /** Every open tab, in the grid and in windows. */
    val tabs: List<String> get() = allLeaves().flatMap { it.tabs }

    /** The pane with the focus, in the grid or in a window. */
    val focusedLeaf: PaneLeaf get() = leaf(focused) ?: leaves.first()

    /** The tab showing in the pane with the focus. */
    val activeTab: String? get() = focusedLeaf.active

    /** The pane [leaf], if it is in the layout. */
    fun leaf(leaf: String): PaneLeaf? = allLeaves().firstOrNull { it.id == leaf }

    /** The pane holding [tab]. */
    fun leafOf(tab: String): PaneLeaf? = allLeaves().firstOrNull { tab in it.tabs }

    /** The split [split], if it is in the layout. */
    fun split(split: String): PaneSplit? = trees().flatMap { it.splits() }.firstOrNull { it.id == split }

    fun focus(leaf: String): PaneLayout = if (leaf(leaf) == null) this else copy(focused = leaf)

    /**
     * Opens [tab] at [index] in [leaf], or in the focused pane, showing it and focusing its pane. A tab already open
     * shows where it is.
     */
    fun open(tab: String, leaf: String = focused, index: Int = Int.MAX_VALUE): PaneLayout {
        if (leafOf(tab) != null) return activate(tab)
        val target = leaf(leaf)?.id ?: focusedLeaf.id
        return mapTrees { tree -> tree.mapLeaves { if (it.id == target) it.withTab(tab, index) else it } }.copy(focused = target)
    }

    /** Shows [tab] in its pane and focuses that pane. */
    fun activate(tab: String): PaneLayout {
        val at = leafOf(tab) ?: return this
        return mapTrees { tree -> tree.mapLeaves { if (it.id == at.id) it.copy(active = tab) else it } }.copy(focused = at.id)
    }

    /** Closes [tab]. The tab beside it shows in its place; a pane left empty collapses. */
    fun close(tab: String): PaneLayout {
        if (leafOf(tab) == null) return this
        return mapTrees { tree -> tree.mapLeaves { it.withoutTab(tab) } }.normalize()
    }

    /** Moves [tab] to [index] in [leaf]'s strip, counted without the tab, showing it there and focusing that pane. */
    fun move(tab: String, leaf: String, index: Int): PaneLayout {
        if (leafOf(tab) == null || leaf(leaf) == null) return this
        return mapTrees { tree -> tree.mapLeaves { if (it.id == leaf) it.withTab(tab, index) else it.withoutTab(tab) } }
            .copy(focused = leaf)
            .normalize()
    }

    /**
     * Splits [leaf] at [edge] and moves [tab] into the new pane, which takes the focus. A pane cannot split off its
     * only tab, so that leaves the layout as it is.
     */
    fun splitWith(tab: String, leaf: String, edge: PaneEdge): PaneLayout {
        val source = leafOf(tab) ?: return this
        if (leaf(leaf) == null || (source.id == leaf && source.tabs.size == 1)) return this
        val (leafId, afterLeaf) = fresh(LEAF_PREFIX)
        val (splitId, afterSplit) = afterLeaf.fresh(SPLIT_PREFIX)
        val added = PaneLeaf(leafId, listOf(tab))
        return afterSplit
            .mapTrees { tree -> tree.mapLeaves { it.withoutTab(tab) }.splitAt(leaf, edge, added, splitId) }
            .copy(focused = leafId)
            .normalize()
    }

    /** Puts [tab] where [drop] says. Dropped in the middle of its own pane, it just shows. */
    fun drop(tab: String, drop: PaneDrop): PaneLayout = when (drop) {
        is PaneDrop.Strip -> move(tab, drop.leaf, drop.index)
        is PaneDrop.Edge -> splitWith(tab, drop.leaf, drop.edge)
        is PaneDrop.Center -> if (leafOf(tab)?.id == drop.leaf) activate(tab) else move(tab, drop.leaf, Int.MAX_VALUE)
    }

    /**
     * Moves the border after child [index] of [split] by [fraction] of the split's length. Both panes beside it keep
     * at least [minShare] of the split.
     */
    fun resize(split: String, index: Int, fraction: Float, minShare: Float = MIN_SHARE): PaneLayout =
        mapTrees { tree -> tree.mapSplits { if (it.id == split) it.resized(index, fraction, minShare) else it } }

    /**
     * The layout with empty panes collapsed, splits of one child replaced by it, nested splits along the same axis
     * merged and weights adding up to 1, every pane showing one of its tabs, and the focus on a pane that exists.
     */
    fun normalize(): PaneLayout {
        val keep = focused
        val gridLeaves = root.leaves()
        val tree = root.pruned() ?: PaneLeaf(gridLeaves.firstOrNull { it.id == keep }?.id ?: gridLeaves.first().id)
        val tidy = copy(root = tree, windows = windows.mapNotNull { w -> w.root.pruned()?.let { w.copy(root = it) } })
        return tidy.copy(focused = if (tidy.leaf(keep) != null) keep else tree.leaves().first().id)
    }

    /** The grid's tree, then each window's. */
    internal fun trees(): List<PaneNode> = listOf(root) + windows.map { it.root }

    private fun allLeaves(): List<PaneLeaf> = trees().flatMap { it.leaves() }

    internal fun mapTrees(change: (PaneNode) -> PaneNode): PaneLayout =
        copy(root = change(root), windows = windows.map { it.copy(root = change(it.root)) })

    /** An id starting with [prefix] that no pane, split or window has yet, and the layout counting past it. */
    internal fun fresh(prefix: String): Pair<String, PaneLayout> {
        val taken = trees().flatMap { it.leaves().map(PaneLeaf::id) + it.splits().map(PaneSplit::id) }.toSet() + windows.map { it.id }
        var n = next
        while ("$prefix$n" in taken) n++
        return "$prefix$n" to copy(next = n + 1)
    }

    companion object {
        /** The id of the one pane a new layout starts with. */
        const val FIRST_LEAF = "pane-0"

        /** The least share of a split each pane keeps when a border is dragged. */
        const val MIN_SHARE = 0.1f

        /** The least size of a pane, in dp, while a border is dragged. */
        const val MIN_PANE = 120f

        internal const val LEAF_PREFIX = "pane-"
        internal const val SPLIT_PREFIX = "split-"
        internal const val WINDOW_PREFIX = "window-"
    }
}
