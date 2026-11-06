package dev.chieppa.stretch.ui

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ComposeUiTest
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class TreeTest {
    private val folders = listOf(
        TreeNode("src", listOf(TreeNode("main", listOf(TreeNode("App.kt"), TreeNode("Tools.kt"))), TreeNode("test", listOf(TreeNode("AppTest.kt"))))),
        TreeNode("README.md"),
    )
    private val k: (String) -> Any = { it }
    private val name: (String) -> String = { it }

    @Test
    fun findsRowsByTheirFirstLetters() {
        val rows = flattenTree(folders) { true }
        assertEquals(listOf("src", "main", "App.kt", "Tools.kt", "test", "AppTest.kt", "README.md"), rows.map { it.node.item })
        assertEquals(3, treeFind(rows, "t", 0, name))
        // A growing query stays on the row while it still matches, and moves on when it no longer does.
        assertEquals(4, treeFind(rows, "te", 3, name))
        assertEquals(4, treeFind(rows, "te", 4, name))
        // One letter repeated steps through the rows starting with it, wrapping around.
        assertEquals(3, treeFind(rows, "tt", 2, name))
        assertEquals(2, treeFind(rows, "a", 6, name))
        assertNull(treeFind(rows, "z", 0, name))
    }

    @Test
    fun checksCascadeAndParentsShowMixed() {
        val main = folders[0].children[0]
        var checked = treeToggleCheck(main, emptySet(), k)
        assertEquals(setOf<Any>("App.kt", "Tools.kt"), checked)
        var states = treeChecks(folders, checked, k)
        assertEquals(true, states["main"])
        assertNull(states["src"])
        assertEquals(false, states["README.md"])
        checked = treeToggleCheck(folders[0], checked, k)
        assertEquals(setOf<Any>("App.kt", "Tools.kt", "AppTest.kt"), checked)
        states = treeChecks(folders, checked, k)
        assertEquals(true, states["src"])
        assertEquals(emptySet(), treeToggleCheck(folders[0], checked, k))
    }

    @Test
    fun findsThePathToANode() {
        assertEquals(listOf<Any>("src", "test"), treePath(folders, "AppTest.kt", k))
        assertEquals(emptyList(), treePath(folders, "README.md", k))
        assertNull(treePath(folders, "nowhere", k))
        assertEquals(setOf<Any>("src", "main", "test"), treeParents(folders, k))
    }

    @Test
    fun buildsOnlyTheRowsOnScreen() = runComposeUiTest {
        val many = (0 until 2000).map { TreeNode("Row $it") }
        val state = TreeState()
        setStretchContent { Tree(many, state, k, name, Modifier.width(260.dp).height(320.dp)) }
        onNodeWithText("Row 0").assertExists()
        onNodeWithText("Row 1999").assertDoesNotExist()
        onNodeWithText("Row 0").performClick()
        onNodeWithText("Row 0").performKeyInput { pressKey(Key.MoveEnd) }
        waitForIdle()
        onNodeWithText("Row 1999").assertExists()
        assertEquals(setOf<Any>("Row 1999"), state.selection)
        onNodeWithText("Row 1999").performKeyInput { pressKey(Key.MoveHome) }
        waitForIdle()
        onNodeWithText("Row 0").assertExists()
        assertEquals("Row 0", state.focused)
    }

    @Test
    fun typingJumpsToARow() = runComposeUiTest {
        val state = TreeState(expanded = setOf("src", "main", "test"))
        setStretchContent { Tree(folders, state, k, name, Modifier.width(260.dp).height(400.dp)) }
        onNodeWithText("src").performClick()
        onNodeWithText("src").performKeyInput {
            pressKey(Key.T)
            pressKey(Key.E)
        }
        assertEquals(setOf<Any>("test"), state.selection)
        onNodeWithText("src").performKeyInput { pressKey(Key.R) }
        assertEquals(setOf<Any>("README.md"), state.selection)
    }

    @Test
    fun checkboxesCascadeFromTheRow() = runComposeUiTest {
        val state = TreeState(expanded = setOf("src", "main", "test"))
        setStretchContent { Tree(folders, state, k, name, Modifier.width(260.dp).height(400.dp), checkboxes = true) }
        onNodeWithContentDescription("main").performClick()
        assertEquals(setOf<Any>("App.kt", "Tools.kt"), state.checked)
        waitForIdle()
        onNodeWithContentDescription("src").assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Indeterminate))
        // Checking never changes the selection.
        assertEquals(emptySet(), state.selection)
        onNodeWithText("README.md").performClick()
        onNodeWithText("README.md").performKeyInput { pressKey(Key.Spacebar) }
        assertEquals(setOf<Any>("App.kt", "Tools.kt", "README.md"), state.checked)
    }

    @Test
    fun enterAndDoubleClickActivate() = runComposeUiTest {
        val state = TreeState(expanded = setOf("src", "main"))
        var opened = emptyList<String>()
        setStretchContent {
            Tree(
                folders, state, k, name, Modifier.width(260.dp).height(400.dp),
                onActivate = { opened = opened + it },
                trailing = { if (it == "main") BasicText("2") },
            )
        }
        onNodeWithText("2").assertExists()
        onNodeWithText("App.kt").performClick()
        onNodeWithText("App.kt").performKeyInput { pressKey(Key.Enter) }
        onNodeWithText("Tools.kt").performClick()
        onNodeWithText("Tools.kt").performClick()
        assertEquals(listOf("App.kt", "Tools.kt"), opened)
    }

    @Test
    fun shiftAndCtrlExtendTheSelection() = runComposeUiTest {
        val state = TreeState(expanded = setOf("src", "main", "test"))
        setStretchContent { Tree(folders, state, k, name, Modifier.width(260.dp).height(400.dp)) }
        onNodeWithText("main").performClick()
        onNodeWithText("main").performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.DirectionDown); pressKey(Key.DirectionDown) } }
        assertEquals(setOf<Any>("main", "App.kt", "Tools.kt"), state.selection)
        onNodeWithText("main").performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(Key.A) } }
        assertEquals(7, state.selection.size)
    }

    private fun ComposeUiTest.setStretchContent(content: @Composable () -> Unit) = setContent { StretchTheme(StretchThemes.Ink, content = content) }
}
