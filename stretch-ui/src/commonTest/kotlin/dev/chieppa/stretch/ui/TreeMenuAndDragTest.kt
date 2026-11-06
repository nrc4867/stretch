package dev.chieppa.stretch.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class TreeMenuAndDragTest {
    private val folders = listOf(
        TreeNode("src", listOf(TreeNode("App.kt"), TreeNode("Tools.kt"), TreeNode("Main.kt"))),
        TreeNode("README.md"),
    )
    private val k: (String) -> Any = { it }
    private val name: (String) -> String = { it }

    private fun ComposeUiTest.setStretchContent(content: @Composable () -> Unit) = setContent { StretchTheme(StretchThemes.Ink, content = content) }

    private fun ComposeUiTest.showTree(state: TreeState, menus: MutableList<Pair<String, Offset>>, onMove: ((Any, TreeDrop) -> Unit)? = null, rowModifier: (String) -> Modifier = { Modifier }) {
        setStretchContent {
            Tree(
                folders, state, k, name, Modifier.width(260.dp).height(400.dp),
                onMove = onMove,
                onContextMenu = { item, at -> menus += item to at },
                rowModifier = rowModifier,
            )
        }
    }

    @Test
    fun aRightClickAsksForTheRowsMenuAndSelectsIt() = runComposeUiTest {
        val state = TreeState(expanded = setOf("src"))
        val menus = mutableListOf<Pair<String, Offset>>()
        showTree(state, menus)
        onNodeWithText("Tools.kt").performMouseInput { rightClick(center) }
        waitForIdle()
        val (item, at) = menus.single()
        assertEquals("Tools.kt", item)
        assertEquals(setOf<Any>("Tools.kt"), state.selection)
        val row = onNodeWithText("Tools.kt").getUnclippedBoundsInRoot()
        assertTrue(at.y / density.density in row.top.value..row.bottom.value, "at the pointer, in the tree's pixels: $at")
    }

    @Test
    fun aRightClickInsideTheSelectionKeepsItWhole() = runComposeUiTest {
        val state = TreeState(expanded = setOf("src"), selection = setOf("App.kt", "Tools.kt"))
        val menus = mutableListOf<Pair<String, Offset>>()
        showTree(state, menus)
        onNodeWithText("App.kt").performMouseInput { rightClick(center) }
        waitForIdle()
        assertEquals("App.kt", menus.single().first)
        assertEquals(setOf<Any>("App.kt", "Tools.kt"), state.selection)
    }

    @Test
    fun aLongPressAsksForTheMenu() = runComposeUiTest {
        val state = TreeState(expanded = setOf("src"))
        val menus = mutableListOf<Pair<String, Offset>>()
        showTree(state, menus)
        onNodeWithText("Main.kt").performTouchInput { longClick(center) }
        waitForIdle()
        assertEquals("Main.kt", menus.first().first)
    }

    @Test
    fun shiftF10AsksForTheFocusedRowsMenuUnderIt() = runComposeUiTest {
        val state = TreeState(expanded = setOf("src"))
        val menus = mutableListOf<Pair<String, Offset>>()
        showTree(state, menus)
        onNodeWithText("App.kt").performClick()
        onNodeWithText("App.kt").performKeyInput { pressKey(Key.DirectionDown) }
        onNodeWithText("App.kt").performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.F10) } }
        waitForIdle()
        val (item, at) = menus.single()
        assertEquals("Tools.kt", item)
        val row = onNodeWithText("Tools.kt").getUnclippedBoundsInRoot()
        assertTrue(at.y / density.density in row.bottom.value..(row.bottom.value + 16f), "under the row: $at")
    }

    @Test
    fun rowsStillDragToANewPlace() = runComposeUiTest {
        val state = TreeState(expanded = setOf("src"))
        var moved: Pair<Any, TreeDrop>? = null
        showTree(state, mutableListOf(), onMove = { key, drop -> moved = key to drop })
        onNodeWithText("App.kt").performTouchInput {
            down(center)
            repeat(10) { moveBy(Offset(0f, 8.dp.toPx())) }
            up()
        }
        waitForIdle()
        assertEquals("App.kt", moved?.first)
    }

    @Test
    fun aRowDraggedOffTheSidesIsLeftToADragSourceAroundIt() = runComposeUiTest {
        val state = TreeState(expanded = setOf("src"))
        var moved: Pair<Any, TreeDrop>? = null
        var taken: String? = null
        val takeUnconsumed: (String) -> Modifier = { item ->
            Modifier.pointerInput(item) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.first()
                        if (!change.pressed) break
                        if (taken != null || change.position.x > size.width) {
                            change.consume()
                            taken = item
                        }
                    }
                }
            }
        }
        showTree(state, mutableListOf(), onMove = { key, drop -> moved = key to drop }, rowModifier = takeUnconsumed)
        onNodeWithText("App.kt").performTouchInput {
            down(center)
            repeat(5) { moveBy(Offset(0f, 10.dp.toPx())) }
            repeat(10) { moveBy(Offset(30.dp.toPx(), 0f)) }
            repeat(3) { moveBy(Offset(-30.dp.toPx(), 0f)) }
            up()
        }
        waitForIdle()
        assertEquals("App.kt", taken)
        assertNull(moved, "the tree let the drag go")
    }

    @Test
    fun aRowLetGoOffTheSidesMovesNothing() = runComposeUiTest {
        val state = TreeState(expanded = setOf("src"))
        var moved: Pair<Any, TreeDrop>? = null
        showTree(state, mutableListOf(), onMove = { key, drop -> moved = key to drop })
        onNodeWithText("App.kt").performTouchInput {
            down(center)
            repeat(5) { moveBy(Offset(0f, 10.dp.toPx())) }
            repeat(10) { moveBy(Offset(30.dp.toPx(), 0f)) }
            up()
        }
        waitForIdle()
        assertNull(moved)
        onNodeWithText("App.kt").performTouchInput {
            down(center)
            repeat(5) { moveBy(Offset(0f, 10.dp.toPx())) }
            repeat(10) { moveBy(Offset(30.dp.toPx(), 0f)) }
            repeat(10) { moveBy(Offset(-30.dp.toPx(), 0f)) }
            up()
        }
        waitForIdle()
        assertNotNull(moved, "brought back over the tree, it drops again")
    }
}
