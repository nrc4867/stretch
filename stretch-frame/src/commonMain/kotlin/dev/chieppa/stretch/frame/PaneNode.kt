package dev.chieppa.stretch.frame

/** How a split lays out its children: side by side in a [Row], or stacked in a [Column]. */
enum class PaneAxis { Row, Column }

/** An edge of a pane where a dropped tab splits it off. [Start] and [End] follow the layout direction. */
enum class PaneEdge { Start, End, Top, Bottom }

/** One node of a [PaneLayout]'s tree: a leaf holding tabs, or a split holding nodes. */
sealed interface PaneNode {
    val id: String
}

/** A pane: its tabs in strip order, by the app's ids, and the one showing. */
data class PaneLeaf(
    override val id: String,
    val tabs: List<String> = emptyList(),
    val active: String? = tabs.firstOrNull(),
) : PaneNode

/**
 * Panes side by side or stacked, along [axis]. [weights] are each child's share of the split, in the same order as
 * [children], and add up to 1.
 */
data class PaneSplit(
    override val id: String,
    val axis: PaneAxis,
    val children: List<PaneNode>,
    val weights: List<Float> = List(children.size) { 1f / children.size },
) : PaneNode

/** Where a dragged tab lands in a pane, named by the pane's leaf id. */
sealed interface PaneDrop {
    val leaf: String

    /** In the pane's tab strip at [index], counted without the dragged tab. */
    data class Strip(override val leaf: String, val index: Int) : PaneDrop

    /** On an edge of the pane's body: the pane splits and the tab gets the new half. */
    data class Edge(override val leaf: String, val edge: PaneEdge) : PaneDrop

    /** In the middle of the pane's body: the tab joins the pane, last in its strip. */
    data class Center(override val leaf: String) : PaneDrop
}
