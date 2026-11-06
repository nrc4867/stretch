package dev.chieppa.stretch.frame

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PaneWindowsTest {
    private val there = WindowBounds(1200f, 300f, 640f, 480f)
    private val elsewhere = WindowBounds(80f, 90f, 500f, 400f)

    private fun three() = PaneLayout().open("a").open("b").open("c").activate("a")

    @Test
    fun detachingOpensATabInAWindowOfItsOwn() {
        val layout = three().detach("b", there)
        val window = layout.windows.single()
        assertEquals(there, window.bounds)
        assertEquals(listOf("b"), window.root.leaves().single().tabs)
        assertEquals(listOf("a", "c"), layout.leaves.single().tabs)
        assertEquals(window.root.id, layout.focused, "the new window has the focus")
        assertEquals("b", layout.activeTab)
        assertEquals(window, layout.windowOf("b"))
        assertNull(layout.windowOf("a"))
        val ids = layout.trees().flatMap { it.leaves().map(PaneLeaf::id) } + layout.windows.map { it.id }
        assertEquals(ids.toSet().size, ids.size)
    }

    @Test
    fun detachingATabAloneInItsWindowMovesTheWindow() {
        val layout = three().detach("b", there)
        val moved = layout.detach("b", elsewhere)
        assertEquals(1, moved.windows.size)
        assertEquals(elsewhere, moved.windows.single().bounds)
        assertEquals(elsewhere, layout.moveWindow(layout.windows.single().id, elsewhere).windows.single().bounds)
    }

    @Test
    fun aWindowGoesWhenItsLastTabLeaves() {
        val layout = three().detach("b", there)
        assertTrue(layout.close("b").windows.isEmpty())
        val back = layout.move("b", PaneLayout.FIRST_LEAF, 0)
        assertTrue(back.windows.isEmpty())
        assertEquals(listOf("b", "a", "c"), back.leaves.single().tabs)
        assertEquals(PaneLayout.FIRST_LEAF, back.focused)
    }

    @Test
    fun tabsMoveIntoAWindowAndItsPanesSplit() {
        val layout = three().detach("b", there)
        val windowLeaf = layout.windows.single().root.id
        val joined = layout.move("c", windowLeaf, 5)
        assertEquals(listOf("b", "c"), joined.windows.single().root.leaves().single().tabs)
        val split = joined.splitWith("c", windowLeaf, PaneEdge.Bottom)
        val root = assertIs<PaneSplit>(split.windows.single().root)
        assertEquals(PaneAxis.Column, root.axis)
        assertIs<PaneLeaf>(split.root, "the grid stays as it was")
    }

    @Test
    fun dockingAWindowMovesAllItsTabsInOrder() {
        val layout = three().detach("b", there).let { it.move("c", it.windows.single().root.id, 5) }.activate("b")
        val id = layout.windows.single().id
        val center = layout.dockWindow(id, PaneDrop.Center(PaneLayout.FIRST_LEAF))
        assertTrue(center.windows.isEmpty())
        assertEquals(listOf("a", "b", "c"), center.leaves.single().tabs)
        assertEquals("b", center.activeTab, "the tab that showed in the window shows")

        val strip = layout.dockWindow(id, PaneDrop.Strip(PaneLayout.FIRST_LEAF, 0))
        assertEquals(listOf("b", "c", "a"), strip.leaves.single().tabs)

        val edge = layout.dockWindow(id, PaneDrop.Edge(PaneLayout.FIRST_LEAF, PaneEdge.Start))
        assertTrue(edge.windows.isEmpty())
        assertEquals(listOf(listOf("b", "c"), listOf("a")), edge.leaves.map { it.tabs })

        val own = layout.windows.single().root.id
        assertSame(layout, layout.dockWindow(id, PaneDrop.Center(own)), "a drop on its own pane changes nothing")
    }

    @Test
    fun closingOrReturningAWindow() {
        val layout = three().detach("b", there)
        val id = layout.windows.single().id
        val closed = layout.closeWindow(id)
        assertEquals(listOf("a", "c"), closed.tabs)
        assertEquals(PaneLayout.FIRST_LEAF, closed.focused)
        val returned = layout.returnWindow(id)
        assertEquals(listOf("a", "c", "b"), returned.leaves.single().tabs)
        assertTrue(returned.windows.isEmpty())
    }

    @Test
    fun theGridKeepsAPaneOfItsOwnWhenEveryTabLeavesIt() {
        val layout = PaneLayout().open("a").detach("a", there)
        assertEquals(PaneLayout.FIRST_LEAF, layout.leaves.single().id)
        assertTrue(layout.leaves.single().tabs.isEmpty())
        assertEquals(layout.windows.single().root.id, layout.focused)
        assertEquals("a", layout.activeTab)
    }
}
