package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.MouseButton
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.test.setStretchContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class PaneGridUiTest {
    private val titles = mapOf("notes" to "Notes", "sketch" to "Sketch", "palette" to "Palette")

    private fun ComposeUiTest.showGrid(start: PaneLayout, onChange: (PaneLayout) -> Unit = {}): () -> PaneLayout {
        var layout by mutableStateOf(start)
        setStretchContent {
            Box(Modifier.size(800.dp, 500.dp)) {
                PaneGrid(layout, { layout = it; onChange(it) }, { TabItem(it, titles.getValue(it)) }) { id ->
                    Body(id)
                }
            }
        }
        return { layout }
    }

    @androidx.compose.runtime.Composable
    private fun Body(id: String) {
        Box(Modifier.fillMaxSize().testTag("content $id")) { RowText("Showing $id") }
    }

    private fun onePane() = PaneLayout().open("notes").open("sketch").activate("notes")

    @Test
    fun tabsShowAndActivate() = runComposeUiTest {
        val layout = showGrid(onePane())
        onNodeWithText("Showing notes").assertExists()
        onNodeWithText("Sketch").performClick()
        waitForIdle()
        assertEquals("sketch", layout().activeTab)
        onNodeWithText("Showing sketch").assertExists()
        onNodeWithText("Showing notes").assertDoesNotExist()
    }

    @Test
    fun keysMoveTheFocusAndActivate() = runComposeUiTest {
        val layout = showGrid(onePane())
        onNodeWithText("Notes").requestFocus()
        onNodeWithText("Notes").performKeyInput { pressKey(Key.DirectionRight) }
        onNodeWithText("Sketch").performKeyInput { pressKey(Key.Enter) }
        waitForIdle()
        assertEquals("sketch", layout().activeTab)
    }

    @Test
    fun closingTheLastTabOfAPaneCollapsesIt() = runComposeUiTest {
        val layout = showGrid(onePane().splitWith("sketch", PaneLayout.FIRST_LEAF, PaneEdge.End))
        assertIs<PaneSplit>(layout().root)
        onNodeWithContentDescription("Close Sketch").performClick()
        waitForIdle()
        assertIs<PaneLeaf>(layout().root)
        assertEquals(listOf("notes"), layout().tabs)
        onNodeWithTag("content notes").assertExists()
    }

    @Test
    fun aMiddleClickCloses() = runComposeUiTest {
        val layout = showGrid(onePane())
        onNodeWithText("Sketch").performMouseInput {
            moveTo(center)
            press(MouseButton.Tertiary)
            release(MouseButton.Tertiary)
        }
        waitForIdle()
        assertEquals(listOf("notes"), layout().tabs)
    }

    @Test
    fun aTabDroppedOnAPanesEdgeSplitsIt() = runComposeUiTest {
        val layout = showGrid(onePane())
        val tab = onNodeWithText("Sketch").getUnclippedBoundsInRoot()
        val body = onNodeWithTag("content notes").getUnclippedBoundsInRoot()
        val from = Offset(((tab.left + tab.right) / 2).value, ((tab.top + tab.bottom) / 2).value)
        val to = Offset((body.right - 20.dp).value, ((body.top + body.bottom) / 2).value)
        onNodeWithText("Sketch").performTouchInput {
            down(center)
            val step = (to - from) * density / 20f
            repeat(20) { moveBy(step) }
            up()
        }
        waitForIdle()
        val root = assertIs<PaneSplit>(layout().root)
        assertEquals(PaneAxis.Row, root.axis)
        assertEquals(listOf(listOf("notes"), listOf("sketch")), layout().leaves.map { it.tabs })
        onNodeWithTag("content sketch").assertExists()
    }

    @Test
    fun aTabDroppedInAnotherStripMovesThere() = runComposeUiTest {
        val layout = showGrid(PaneLayout().open("notes").open("sketch").open("palette").splitWith("palette", PaneLayout.FIRST_LEAF, PaneEdge.End))
        val tab = onNodeWithText("Notes").getUnclippedBoundsInRoot()
        val target = onNodeWithText("Palette").getUnclippedBoundsInRoot()
        val from = Offset(((tab.left + tab.right) / 2).value, ((tab.top + tab.bottom) / 2).value)
        val to = Offset((target.left + 4.dp).value, ((target.top + target.bottom) / 2).value)
        onNodeWithText("Notes").performTouchInput {
            down(center)
            val step = (to - from) * density / 20f
            repeat(20) { moveBy(step) }
            up()
        }
        waitForIdle()
        assertEquals(listOf(listOf("sketch"), listOf("notes", "palette")), layout().leaves.map { it.tabs })
    }

    @Test
    fun draggingABorderResizesThePanes() = runComposeUiTest {
        val layout = showGrid(onePane().splitWith("sketch", PaneLayout.FIRST_LEAF, PaneEdge.End))
        onNodeWithContentDescription("Resize panes").performTouchInput {
            down(center)
            repeat(10) { moveBy(Offset(8f, 0f)) }
            up()
        }
        waitForIdle()
        val weights = assertIs<PaneSplit>(layout().root).weights
        assertTrue(weights[0] > 0.55f, "the left pane grew: $weights")
    }
}
