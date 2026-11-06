package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.test.setStretchContent
import dev.chieppa.stretch.ui.Tree
import dev.chieppa.stretch.ui.TreeNode
import dev.chieppa.stretch.ui.TreeState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class PaneDragSourceUiTest {
    /** Pane windows drawn in place, at the same screen points as the grid's window. */
    private class InlineHost : PaneWindowHost {
        override fun screenPosition(point: Offset) = point

        @Composable
        override fun DragFloat(at: Offset?, size: Dp, content: @Composable () -> Unit) {}

        @Composable
        override fun PaneWindowFrame(
            title: String,
            bounds: WindowBounds,
            raise: Int,
            onBoundsChange: (WindowBounds) -> Unit,
            onClose: () -> Unit,
            onDragOver: (screen: Offset?) -> Boolean,
            onDrop: (screen: Offset) -> Unit,
            content: @Composable () -> Unit,
        ) {}
    }

    private val titles = mapOf("notes" to "Notes", "sketch" to "Sketch", "palette" to "Palette", "brushes" to "Brushes")

    private fun ComposeUiTest.show(
        host: PaneWindowHost? = null,
        onDropItem: ((String, PaneItemDrop) -> Unit)? = null,
        sources: @Composable (PaneDropTarget) -> Unit = { target ->
            Box(Modifier.size(160.dp, 32.dp).testTag("source").paneDragSource(target) { TabItem("palette", "Palette") })
        },
    ): () -> PaneLayout {
        var layout by mutableStateOf(PaneLayout().open("notes").open("sketch").activate("notes"))
        setStretchContent {
            CompositionLocalProvider(LocalPaneWindows provides host) {
                val target = rememberPaneDropTarget()
                Column {
                    Row {
                        Column(Modifier.width(200.dp)) { sources(target) }
                        PaneGrid(
                            layout, { layout = it }, { TabItem(it, titles.getValue(it)) }, Modifier.size(600.dp, 300.dp),
                            dropTarget = target, onDropItem = onDropItem,
                        ) { id -> Box(Modifier.fillMaxSize().testTag("content $id")) }
                    }
                    Box(Modifier.size(800.dp, 200.dp).testTag("below"))
                }
            }
        }
        return { layout }
    }

    private fun DpRect.center() = Offset(((left + right) / 2).value, ((top + bottom) / 2).value)

    /** Drags [from] with the mouse to [to], in dp from the root, in steps. */
    private fun ComposeUiTest.mouseDrag(from: SemanticsNodeInteraction, to: Offset, check: () -> Unit = {}) {
        val bounds = from.getUnclippedBoundsInRoot()
        val start = bounds.center()
        from.performMouseInput {
            moveTo(center)
            press()
            val step = (to - start) * density / 20f
            repeat(20) { moveBy(step) }
        }
        waitForIdle()
        check()
        from.performMouseInput { release() }
        waitForIdle()
    }

    @Test
    fun anItemDroppedInAPanesMiddleOpensThere() = runComposeUiTest {
        val layout = show()
        val body = onNodeWithTag("content notes").getUnclippedBoundsInRoot().center()
        mouseDrag(onNodeWithTag("source"), body) {
            onNodeWithText("Palette").assertExists()
        }
        assertEquals(listOf("notes", "sketch", "palette"), layout().leaves.single().tabs)
        assertEquals("palette", layout().activeTab)
    }

    @Test
    fun anItemDroppedOnAPanesEdgeSplitsIt() = runComposeUiTest {
        val layout = show()
        val body = onNodeWithTag("content notes").getUnclippedBoundsInRoot()
        mouseDrag(onNodeWithTag("source"), Offset((body.right - 20.dp).value, body.center().y))
        val root = assertIs<PaneSplit>(layout().root)
        assertEquals(PaneAxis.Row, root.axis)
        assertEquals(listOf(listOf("notes", "sketch"), listOf("palette")), layout().leaves.map { it.tabs })
    }

    @Test
    fun anItemLetGoAwayFromTheGridOpensAWindowWhereThereIsAHost() = runComposeUiTest {
        val layout = show(InlineHost())
        mouseDrag(onNodeWithTag("source"), onNodeWithTag("below").getUnclippedBoundsInRoot().center())
        assertEquals(listOf("palette"), layout().windows.single().root.leaves().single().tabs)
        assertEquals(listOf("notes", "sketch"), layout().leaves.single().tabs)
    }

    @Test
    fun anItemLetGoAwayFromTheGridWithoutAHostOpensNothing() = runComposeUiTest {
        val layout = show()
        mouseDrag(onNodeWithTag("source"), onNodeWithTag("below").getUnclippedBoundsInRoot().center())
        assertEquals(listOf("notes", "sketch"), layout().tabs)
    }

    @Test
    fun theAppCanMakeTheTabItself() = runComposeUiTest {
        val drops = mutableListOf<Pair<String, PaneItemDrop>>()
        val layout = show(onDropItem = { id, drop -> drops += id to drop })
        mouseDrag(onNodeWithTag("source"), onNodeWithTag("content notes").getUnclippedBoundsInRoot().center())
        val (id, drop) = drops.single()
        assertEquals("palette", id)
        assertEquals(PaneItemDrop.InPane(PaneDrop.Center(PaneLayout.FIRST_LEAF)), drop)
        assertEquals(listOf("notes", "sketch"), layout().tabs, "the app opens it")
    }

    @Test
    fun treeRowsDragIntoThePanes() = runComposeUiTest {
        val state = TreeState()
        val layout = show(
            sources = { target ->
                Tree(
                    listOf(TreeNode("palette"), TreeNode("brushes")), state, { it }, { titles.getValue(it) },
                    Modifier.height(200.dp),
                    onMove = { _, _ -> },
                    rowModifier = { id -> Modifier.paneDragSource(target) { TabItem(id, titles.getValue(id)) } },
                )
            },
        )
        val body = onNodeWithTag("content notes").getUnclippedBoundsInRoot()
        val row = onNodeWithText("Brushes").getUnclippedBoundsInRoot()
        val to = Offset((body.left + 20.dp).value, body.center().y)
        onNodeWithText("Brushes").performTouchInput {
            down(center)
            val step = (to - row.center()) * density / 20f
            repeat(20) { moveBy(step) }
            up()
        }
        waitForIdle()
        assertEquals(listOf(listOf("brushes"), listOf("notes", "sketch")), layout().leaves.map { it.tabs })
        assertTrue(state.selection.isEmpty(), "a drag is not a click")
    }
}
