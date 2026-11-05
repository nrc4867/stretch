package dev.chieppa.stretch.markdown

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test
import kotlin.test.assertEquals

/** Real keys through the real fields, in both modes where they differ. */
@OptIn(ExperimentalTestApi::class)
class EditorTypingTest {
    private fun shows(text: String) = SemanticsMatcher("shows '$text'") { node ->
        SemanticsProperties.EditableText in node.config && node.config[SemanticsProperties.EditableText].text == text
    }

    private fun ComposeUiTest.open(text: String, mode: MarkdownMode = MarkdownMode.LivePreview): MarkdownEditorState {
        val state = MarkdownEditorState(text, mode)
        setContent { StretchTheme(StretchThemes.Ink) { MarkdownEditor(state, Modifier.fillMaxSize(), listOf(mode)) } }
        waitForIdle()
        return state
    }

    /** Focuses the field drawing [text], with the caret at its end. */
    private fun ComposeUiTest.field(text: String): SemanticsNodeInteraction {
        val node = onNode(shows(text))
        node.requestFocus()
        waitForIdle()
        return node
    }

    private fun ComposeUiTest.press(node: SemanticsNodeInteraction, key: Key, times: Int = 1) {
        repeat(times) {
            node.performKeyInput { pressKey(key) }
            waitForIdle()
        }
    }

    /** Puts the caret at the end of the focused field drawn as [text]. */
    private fun ComposeUiTest.toEnd(text: String): SemanticsNodeInteraction {
        val node = onNode(shows(text))
        press(node, Key.MoveEnd)
        return node
    }

    private fun ComposeUiTest.focused(text: String) = onNode(shows(text)).assertIsFocused()

    @Test
    fun typingAMarkerMakesTheBlockAndItHides() = runComposeUiTest {
        val state = open("", MarkdownMode.Wysiwyg)
        val node = field("")
        node.performTextInput("## ")
        waitForIdle()
        assertEquals(BlockKind.Heading(2), state.document.blocks[0].kind)
        onNode(shows("")).performTextInput("Title")
        waitForIdle()
        assertEquals("## Title", state.text)
        onNode(shows("Title")).assertIsFocused()
    }

    @Test
    fun enterSplitsAndTheCaretMovesOn() = runComposeUiTest {
        val state = open("first")
        field("first")
        press(toEnd("first"), Key.Enter)
        assertEquals(2, state.document.blocks.size)
        onNode(shows("")).assertIsFocused()
        onNode(shows("")).performTextInput("second")
        waitForIdle()
        assertEquals("first\n\nsecond", state.text)
    }

    @Test
    fun enterContinuesAListInLivePreview() = runComposeUiTest {
        val state = open("- a")
        field("a")
        press(toEnd("- a"), Key.Enter)
        assertEquals("- a\n- ", state.text)
        onNode(shows("- ")).performTextInput("b")
        waitForIdle()
        assertEquals("- a\n- b", state.text)
    }

    @Test
    fun backspaceAtTheStartOfRichTextDropsTheMarkerThenJoins() = runComposeUiTest {
        val state = open("before\n\n## heading", MarkdownMode.Wysiwyg)
        field("heading")
        val node = toEnd("heading")
        press(node, Key.DirectionLeft, times = 7)
        // The hidden marker takes no key presses: seven take the caret to the start of the visible text.
        assertEquals(3, state.fieldValue.selection.start)
        press(node, Key.Backspace)
        assertEquals("before\n\nheading", state.text)
        press(onNode(shows("heading")), Key.Backspace)
        assertEquals("beforeheading", state.text)
        assertEquals(6, state.fieldValue.selection.start)
    }

    @Test
    fun arrowsWalkBetweenBlocks() = runComposeUiTest {
        val state = open("one\n\ntwo\n\nthree")
        press(field("one"), Key.DirectionDown)
        focused("two")
        press(onNode(shows("two")), Key.DirectionDown)
        focused("three")
        press(onNode(shows("three")), Key.DirectionUp)
        focused("two")
        assertEquals("one\n\ntwo\n\nthree", state.text)
    }

    @Test
    fun enterInCodeStaysInTheBlock() = runComposeUiTest {
        val state = open("```\nx\n```", MarkdownMode.Wysiwyg)
        field("x")
        press(toEnd("x"), Key.Enter)
        assertEquals(1, state.document.blocks.size)
    }

    @Test
    fun tabNestsAListItem() = runComposeUiTest {
        val state = open("- a\n- b", MarkdownMode.Wysiwyg)
        field("b")
        press(toEnd("b"), Key.Tab)
        assertEquals("- a\n  - b", state.text)
        focused("b")
        onNode(shows("b")).performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.Tab) } }
        waitForIdle()
        assertEquals("- a\n- b", state.text)
    }

    @Test
    fun theSlashMenuPicksABlockType() = runComposeUiTest {
        val state = open("", MarkdownMode.Wysiwyg)
        field("").performTextInput("/quo")
        waitForIdle()
        onNodeWithText("Quote").assertExists()
        press(onNode(shows("/quo")), Key.Enter)
        assertEquals("> ", state.text)
    }

    @Test
    fun escapeClosesTheSlashMenu() = runComposeUiTest {
        val state = open("", MarkdownMode.Wysiwyg)
        field("").performTextInput("/quo")
        waitForIdle()
        press(onNode(shows("/quo")), Key.Escape)
        onNodeWithText("Quote").assertDoesNotExist()
        assertEquals("/quo", state.text)
    }

    @Test
    fun aClickOnATaskBoxTogglesIt() = runComposeUiTest {
        val state = open("- [ ] thing", MarkdownMode.Wysiwyg)
        onNode(SemanticsMatcher("task box") { node ->
            SemanticsProperties.ContentDescription in node.config && node.config[SemanticsProperties.ContentDescription] == listOf("thing")
        }).performClick()
        waitForIdle()
        assertEquals("- [x] thing", state.text)
    }
}
