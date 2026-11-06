package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.test.setStretchContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class PaneWindowUiTest {
    /** Pane windows drawn under the grid in the same window, at a screen [SHIFT] from it. */
    private class InlinePaneWindows : PaneWindowHost {
        val overs = mutableMapOf<String, (Offset?) -> Boolean>()
        val drops = mutableMapOf<String, (Offset) -> Unit>()
        var floatAt: Offset? = null

        override fun screenPosition(point: Offset) = point + SHIFT

        @Composable
        override fun DragFloat(at: Offset?, size: Dp, content: @Composable () -> Unit) {
            floatAt = at
        }

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
        ) {
            overs[title] = onDragOver
            drops[title] = onDrop
            Box(Modifier.size(400.dp, 200.dp).testTag("window $title")) { content() }
        }
    }

    private val titles = mapOf("notes" to "Notes", "sketch" to "Sketch")

    private fun ComposeUiTest.showGrid(start: PaneLayout, windows: InlinePaneWindows): () -> PaneLayout {
        var layout by mutableStateOf(start)
        setStretchContent {
            CompositionLocalProvider(LocalPaneWindows provides windows) {
                Column {
                    PaneGrid(layout, { layout = it }, { TabItem(it, titles.getValue(it)) }, Modifier.size(800.dp, 300.dp)) { id ->
                        Box(Modifier.fillMaxSize().testTag("content $id")) { RowText("Showing $id") }
                    }
                }
            }
        }
        return { layout }
    }

    private fun onePane() = PaneLayout().open("notes").open("sketch").activate("notes")

    private fun ComposeUiTest.tab(title: String) = onNode(hasText(title) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))

    private fun DpRect.center() = Offset(((left + right) / 2).value, ((top + bottom) / 2).value)

    @Test
    fun theTabMenuMovesATabToAWindow() = runComposeUiTest {
        val windows = InlinePaneWindows()
        val layout = showGrid(onePane(), windows)
        tab("Sketch").performMouseInput { rightClick(center) }
        onNodeWithText("Move to new window").performClick()
        waitForIdle()
        assertEquals(listOf("sketch"), layout().windows.single().root.leaves().single().tabs)
        onNodeWithTag("window Sketch").assertExists()
        onNodeWithTag("content sketch").assertExists()
        assertTrue(layout().windows.single().bounds.x > SHIFT.x, "opens over its pane, on screen")
    }

    @Test
    fun aTabLetGoAwayFromTheGridOpensAWindowUnderThePointer() = runComposeUiTest {
        val windows = InlinePaneWindows()
        val layout = showGrid(onePane(), windows)
        val node = tab("Sketch")
        node.performTouchInput {
            down(center)
            repeat(20) { moveBy(Offset(0f, 20.dp.toPx())) }
        }
        waitForIdle()
        assertTrue(windows.floatAt != null, "the tab follows the pointer past the grid")
        node.performTouchInput { up() }
        waitForIdle()
        val window = layout().windows.single()
        assertEquals(listOf("sketch"), window.root.leaves().single().tabs)
        assertTrue(window.bounds.y > SHIFT.y + 300f, "under the pointer: ${window.bounds}")
        assertEquals(listOf("notes"), layout().leaves.single().tabs)
    }

    @Test
    fun aWindowDraggedOntoAPaneDocksThere() = runComposeUiTest {
        val windows = InlinePaneWindows()
        val layout = showGrid(onePane().detach("sketch", WindowBounds(0f, 0f, 400f, 200f)), windows)
        waitForIdle()
        val at = onNodeWithTag("content notes").getUnclippedBoundsInRoot().center() * density.density + SHIFT
        assertTrue(windows.overs.getValue("Sketch")(at), "the pane takes the window")
        waitForIdle()
        windows.drops.getValue("Sketch")(at)
        waitForIdle()
        assertTrue(layout().windows.isEmpty())
        assertEquals(listOf("notes", "sketch"), layout().leaves.single().tabs)
        onNodeWithTag("window Sketch").assertDoesNotExist()
    }

    @Test
    fun aTabDraggedFromAWindowIntoTheGridMovesThere() = runComposeUiTest {
        val windows = InlinePaneWindows()
        val layout = showGrid(onePane().detach("sketch", WindowBounds(0f, 0f, 400f, 200f)), windows)
        waitForIdle()
        val from = tab("Sketch").getUnclippedBoundsInRoot().center()
        val notes = tab("Notes").getUnclippedBoundsInRoot()
        val to = Offset((notes.right - 4.dp).value, notes.center().y)
        tab("Sketch").performTouchInput {
            down(center)
            val step = (to - from) * density / 20f
            repeat(20) { moveBy(step) }
            up()
        }
        waitForIdle()
        assertTrue(layout().windows.isEmpty())
        assertEquals(listOf("notes", "sketch"), layout().leaves.single().tabs)
    }

    private companion object {
        val SHIFT = Offset(1000f, 2000f)
    }
}
