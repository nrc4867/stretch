package dev.chieppa.stretch.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class BasicsTest {
    @Test
    fun checkboxCyclesFromMixedToChecked() = runComposeUiTest {
        var checked by mutableStateOf<Boolean?>(null)
        setContent { StretchTheme(StretchThemes.Ink) { Checkbox(checked, { checked = it }, "Head") } }
        onNodeWithText("Head").assert(SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Indeterminate))
        onNodeWithText("Head").performClick()
        assertEquals(true, checked)
        onNodeWithText("Head").performClick()
        assertEquals(false, checked)
    }

    @Test
    fun radioGroupPicksOne() = runComposeUiTest {
        var pick by mutableStateOf("This part")
        setContent { StretchTheme(StretchThemes.Ink) { RadioGroup(listOf("This part", "All parts"), pick, { pick = it }, { it }) } }
        onNodeWithText("All parts").performClick()
        assertEquals("All parts", pick)
        onNodeWithText("All parts").assertIsSelected()
    }

    @Test
    fun badgesCountUpToNinetyNine() {
        assertNull(badgeCount(0))
        assertEquals("7", badgeCount(7))
        assertEquals("99+", badgeCount(140))
    }

    @Test
    fun tabsSwitchAndClose() = runComposeUiTest {
        var tabs by mutableStateOf(listOf("Fox", "Owl"))
        var current by mutableStateOf("Fox")
        setContent {
            StretchTheme(StretchThemes.Ink) {
                Column {
                Tabs(tabs, current, { current = it }, { it }, onClose = { t -> tabs = tabs - t }, closeDescription = { "Close $it" })
                BadgedBox("3", "Library · 3") { BasicText("Library") }
                EmptyState(StretchIcons.Layers, "No layers", actionLabel = "Add layer")
                }
            }
        }
        onNodeWithText("Owl").performClick()
        assertEquals("Owl", current)
        onNodeWithContentDescription("Close Fox").performClick()
        assertEquals(listOf("Owl"), tabs)
        onNodeWithContentDescription("Library · 3").assertExists()
        onNodeWithText("Add layer").assertExists()
    }

    @Test
    fun fieldsReportErrors() = runComposeUiTest {
        setContent { StretchTheme(StretchThemes.Ink) { TextField("fox?", {}, placeholder = "File name", error = "Names can't contain ?") } }
        onNodeWithContentDescription("File name").assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "Names can't contain ?"))
        onNodeWithContentDescription("Names can't contain ?").assertExists()
    }

    private val parts = listOf(
        TreeNode("Body", listOf(TreeNode("Head", listOf(TreeNode("Ear"))), TreeNode("Tail"))),
        TreeNode("Props"),
    )

    @Test
    fun treeFlattensOpenNodesAndPlansDrops() {
        val open = setOf("Body")
        val rows = flattenTree(parts) { it in open }
        assertEquals(listOf("Body", "Head", "Tail", "Props"), rows.map { it.node.item })
        assertEquals(listOf(0, 1, 1, 0), rows.map { it.depth })
        val k: (String) -> Any = { it }
        assertEquals(TreeDrop("Body", 0, into = false), treeDrop(rows, parts, "Tail", 1, 0.1f, k) { it in open })
        assertEquals(TreeDrop("Head", 1, into = true), treeDrop(rows, parts, "Tail", 1, 0.5f, k) { it in open })
        assertEquals(TreeDrop(null, 2, into = false), treeDrop(rows, parts, "Tail", 3, 0.9f, k) { it in open })
        assertNull(treeDrop(rows, parts, "Body", 1, 0.5f, k) { it in open })
        assertEquals(setOf("Head", "Tail", "Props"), treeRange(rows, "Props", "Head", k))
    }

    @Test
    fun treeOpensSelectsRenamesAndMoves() = runComposeUiTest {
        var open by mutableStateOf(setOf<Any>())
        var selection by mutableStateOf(setOf<Any>())
        var renamed = ""
        var moved: Pair<Any, TreeDrop>? = null
        setContent {
            StretchTheme(StretchThemes.Ink) {
                Column {
                    Tree(
                        parts, { it }, { it }, open, { key, o -> open = if (o) open + key else open - key },
                        selection, { selection = it }, Modifier.width(260.dp),
                        onRename = { item, name -> renamed = "$item→$name" },
                        onMove = { key, drop -> moved = key to drop },
                    )
                }
            }
        }
        onNodeWithContentDescription("Body · Expanded").performClick()
        onNodeWithText("Head").assertExists()
        onNodeWithText("Head").performClick()
        assertEquals(setOf<Any>("Head"), selection)
        onNodeWithText("Head").performKeyInput {
            pressKey(Key.DirectionDown)
            withKeyDown(Key.ShiftLeft) { pressKey(Key.DirectionDown) }
        }
        assertEquals(setOf<Any>("Tail", "Props"), selection)
        onNodeWithText("Props").performKeyInput { pressKey(Key.F2) }
        onNodeWithText("Props").performTextReplacement("Accessories")
        onNodeWithText("Accessories").performKeyInput { pressKey(Key.Enter) }
        assertEquals("Props→Accessories", renamed)
        onNodeWithText("Props").performTouchInput {
            down(center)
            repeat(12) { moveBy(Offset(0f, -5f)) }
            up()
        }
        assertEquals("Props", moved?.first)
    }
}
