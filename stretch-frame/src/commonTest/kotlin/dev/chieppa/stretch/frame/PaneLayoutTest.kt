package dev.chieppa.stretch.frame

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PaneLayoutTest {
    /** One pane holding a, b and c, showing a. */
    private fun three() = PaneLayout().open("a").open("b").open("c").activate("a")

    /** Two panes side by side: a and b on the left, c on the right with the focus. */
    private fun sideBySide() = three().splitWith("c", PaneLayout.FIRST_LEAF, PaneEdge.End)

    @Test
    fun openingAddsToTheFocusedPaneAndShowsTheTab() {
        val layout = PaneLayout().open("a").open("b")
        assertEquals(listOf("a", "b"), layout.focusedLeaf.tabs)
        assertEquals("b", layout.activeTab)
        assertEquals(listOf("x", "a", "b"), layout.open("x", index = 0).focusedLeaf.tabs)
        assertEquals("a", layout.open("a").activeTab, "an open tab shows where it is")
        assertEquals(listOf("a", "b"), layout.open("a").focusedLeaf.tabs)
    }

    @Test
    fun closingShowsTheNeighbourAndCollapsesAnEmptyPane() {
        val layout = three().activate("b")
        assertEquals("c", layout.close("b").activeTab, "the tab after it")
        assertEquals("b", layout.activate("c").close("c").activeTab, "the one before it at the end")
        val split = sideBySide()
        assertIs<PaneSplit>(split.root)
        val collapsed = split.close("c")
        assertIs<PaneLeaf>(collapsed.root)
        assertEquals(PaneLayout.FIRST_LEAF, collapsed.focused, "the focus moves to a pane that is left")
        val empty = PaneLayout().open("a").close("a")
        assertEquals(1, empty.leaves.size, "the grid keeps one pane")
        assertTrue(empty.focusedLeaf.tabs.isEmpty())
    }

    @Test
    fun splittingAPaneMovesTheTabIntoTheNewHalf() {
        val layout = sideBySide()
        val split = assertIs<PaneSplit>(layout.root)
        assertEquals(PaneAxis.Row, split.axis)
        assertEquals(listOf(listOf("a", "b"), listOf("c")), layout.leaves.map { it.tabs })
        assertEquals(layout.leaves[1].id, layout.focused)
        assertEquals(listOf(0.5f, 0.5f), split.weights)

        val before = three().splitWith("b", PaneLayout.FIRST_LEAF, PaneEdge.Top)
        assertEquals(PaneAxis.Column, assertIs<PaneSplit>(before.root).axis)
        assertEquals(listOf(listOf("b"), listOf("a", "c")), before.leaves.map { it.tabs })
    }

    @Test
    fun aSplitAlongTheSameAxisTakesTheNewPaneAsASibling() {
        val layout = sideBySide().splitWith("b", PaneLayout.FIRST_LEAF, PaneEdge.End)
        val split = assertIs<PaneSplit>(layout.root)
        assertEquals(3, split.children.size, "no split inside the split")
        assertEquals(listOf(listOf("a"), listOf("b"), listOf("c")), layout.leaves.map { it.tabs })
        assertEquals(listOf(0.25f, 0.25f, 0.5f), split.weights)
    }

    @Test
    fun aPaneCannotSplitOffItsOnlyTab() {
        val layout = sideBySide()
        assertSame(layout, layout.splitWith("c", layout.leaves[1].id, PaneEdge.Start))
    }

    @Test
    fun splittingAcrossCollapsesTheSourceWhenItEmpties() {
        val layout = sideBySide()
        val moved = layout.splitWith("c", PaneLayout.FIRST_LEAF, PaneEdge.Bottom)
        val split = assertIs<PaneSplit>(moved.root)
        assertEquals(PaneAxis.Column, split.axis, "the right pane collapsed and the left one split")
        assertEquals(listOf(listOf("a", "b"), listOf("c")), moved.leaves.map { it.tabs })
    }

    @Test
    fun movingReordersAndCrossesPanes() {
        val layout = three()
        assertEquals(listOf("b", "c", "a"), layout.move("a", PaneLayout.FIRST_LEAF, 5).focusedLeaf.tabs)
        assertEquals(listOf("b", "a", "c"), layout.move("a", PaneLayout.FIRST_LEAF, 1).focusedLeaf.tabs)
        val two = sideBySide()
        val right = two.leaves[1].id
        val crossed = two.move("a", right, 0)
        assertEquals(listOf(listOf("b"), listOf("a", "c")), crossed.leaves.map { it.tabs })
        assertEquals(right, crossed.focused)
        assertEquals("a", crossed.activeTab)
        val emptied = crossed.move("b", right, 1)
        assertIs<PaneLeaf>(emptied.root)
        assertEquals(listOf("a", "b", "c"), emptied.focusedLeaf.tabs)
    }

    @Test
    fun dropsDoWhatTheirPlaceSays() {
        val two = sideBySide()
        val right = two.leaves[1].id
        assertEquals(listOf("a", "c"), two.drop("a", PaneDrop.Strip(right, 0)).leaf(right)!!.tabs)
        assertEquals(listOf("c", "a"), two.drop("a", PaneDrop.Center(right)).leaf(right)!!.tabs)
        assertEquals(3, two.drop("a", PaneDrop.Edge(right, PaneEdge.End)).leaves.size)
        val own = two.drop("b", PaneDrop.Center(PaneLayout.FIRST_LEAF))
        assertEquals(listOf("a", "b"), own.leaf(PaneLayout.FIRST_LEAF)!!.tabs, "its own pane: it only shows")
        assertEquals("b", own.activeTab)
    }

    @Test
    fun resizingKeepsBothPanesAboveTheirMinimum() {
        val layout = sideBySide()
        val id = layout.root.id
        fun PaneLayout.shares() = split(id)!!.weights.map { (it * 100).roundToInt() }
        assertEquals(listOf(70, 30), layout.resize(id, 0, 0.2f).shares())
        assertEquals(listOf(90, 10), layout.resize(id, 0, 0.9f).shares())
        assertEquals(listOf(20, 80), layout.resize(id, 0, -0.9f, minShare = 0.2f).shares())
        assertEquals(layout.root, layout.resize(id, 1, 0.1f).root, "no border after the last child")
    }

    @Test
    fun normalizingTidiesTheTree() {
        val messy = PaneLayout(
            root = PaneSplit(
                "outer", PaneAxis.Row,
                listOf(
                    PaneLeaf("l1", listOf("a"), active = "gone"),
                    PaneSplit("inner", PaneAxis.Row, listOf(PaneLeaf("l2", listOf("b")), PaneLeaf("l3", listOf("c"))), listOf(1f, 3f)),
                    PaneSplit("lonely", PaneAxis.Column, listOf(PaneLeaf("l4")), listOf(1f)),
                ),
                listOf(2f, 2f, 1f),
            ),
            focused = "l4",
        )
        val tidy = messy.normalize()
        val root = assertIs<PaneSplit>(tidy.root)
        assertEquals(listOf("l1", "l2", "l3"), root.children.map { it.id })
        assertEquals(listOf(0.5f, 0.125f, 0.375f), root.weights)
        assertEquals("a", tidy.leaf("l1")!!.active)
        assertEquals("l1", tidy.focused)
    }

    @Test
    fun newIdsSkipThoseTaken() {
        val layout = PaneLayout(PaneLeaf("pane-1", listOf("a", "b")), next = 1)
        val split = layout.splitWith("b", "pane-1", PaneEdge.End)
        val ids = split.leaves.map { it.id } + split.root.id
        assertEquals(ids.toSet().size, ids.size)
    }

    @Test
    fun aTabIsOpenInOnePaneAtATime() {
        assertFailsWith<IllegalArgumentException> {
            PaneLayout(PaneSplit("s", PaneAxis.Row, listOf(PaneLeaf("x", listOf("a")), PaneLeaf("y", listOf("a")))))
        }
    }

    @Test
    fun aDropNearAnEdgeSplitsAndInTheMiddleJoins() {
        val body = Rect(0f, 0f, 400f, 200f)
        assertEquals(PaneEdge.Start, edgeAt(body, Offset(20f, 100f), rtl = false))
        assertEquals(PaneEdge.End, edgeAt(body, Offset(20f, 100f), rtl = true))
        assertEquals(PaneEdge.Bottom, edgeAt(body, Offset(200f, 190f), rtl = false))
        assertNull(edgeAt(body, Offset(200f, 100f), rtl = false))
        assertEquals(Rect(200f, 0f, 400f, 200f), dropPreview(body, PaneEdge.End, rtl = false))
        assertEquals(Rect(0f, 0f, 200f, 200f), dropPreview(body, PaneEdge.End, rtl = true))
        assertEquals(body, dropPreview(body, null, rtl = false))
    }

    @Test
    fun aDroppedTabGoesBeforeTheFirstTabPastIt() {
        val spans = listOf(0f..100f, 100f..200f, 200f..300f)
        assertEquals(0, stripIndex(spans, 10f, rtl = false))
        assertEquals(2, stripIndex(spans, 160f, rtl = false))
        assertEquals(3, stripIndex(spans, 290f, rtl = false))
        assertEquals(3, stripIndex(spans, 10f, rtl = true))
    }

    @Test
    fun placingAnItemOpensItWhereItWasLetGo() {
        val start = PaneLayout().open("a").open("b")
        assertEquals(listOf("a", "c", "b"), start.place("c", PaneItemDrop.InPane(PaneDrop.Strip(PaneLayout.FIRST_LEAF, 1))).tabs)
        val split = start.place("c", PaneItemDrop.InPane(PaneDrop.Edge(PaneLayout.FIRST_LEAF, PaneEdge.Bottom)))
        assertEquals(listOf(listOf("a", "b"), listOf("c")), split.leaves.map { it.tabs })
        val shown = start.place("a", PaneItemDrop.InPane(PaneDrop.Center(PaneLayout.FIRST_LEAF)))
        assertEquals(listOf("a", "b"), shown.tabs, "an open tab is not opened twice")
        assertEquals("a", shown.activeTab)
        val window = start.place("c", PaneItemDrop.InWindow(WindowBounds(0f, 0f, 400f, 300f)))
        assertEquals(listOf("c"), window.windows.single().root.leaves().single().tabs)
        assertEquals(listOf("a", "b"), window.leaves.single().tabs)
        assertEquals(listOf("c"), PaneLayout().place("c", PaneItemDrop.InPane(PaneDrop.Edge(PaneLayout.FIRST_LEAF, PaneEdge.End))).leaves.single().tabs)
    }
}
