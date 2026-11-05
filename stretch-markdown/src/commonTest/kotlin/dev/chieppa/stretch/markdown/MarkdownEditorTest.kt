package dev.chieppa.stretch.markdown

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.TextRange
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class MarkdownEditorTest {
    private val sample = "# Title\n\nSome **bold** words\n\n- first\n- [x] second\n\n```kotlin\nval x = 1\n```\n\n| a | b |\n|---|---|\n| 1 | 2 |\n\n---\n"

    private fun ComposeUiTest.show(state: MarkdownEditorState, modes: List<MarkdownMode> = listOf(state.mode), readOnly: Boolean = false) {
        setContent { StretchTheme(StretchThemes.Ink) { MarkdownEditor(state, Modifier.fillMaxSize(), modes, readOnly) } }
        waitForIdle()
    }

    /** A field drawing exactly [text], whatever its source holds. */
    private fun shows(text: String) = SemanticsMatcher("shows '$text'") { node ->
        SemanticsProperties.EditableText in node.config && node.config[SemanticsProperties.EditableText].text == text
    }

    @Test
    fun openingADocumentTakesNoFocus() = runComposeUiTest {
        val state = MarkdownEditorState("- a")
        show(state)
        onNode(shows("a")).assertIsNotFocused()
    }

    @Test
    fun aBlockOutOfFocusHidesItsMarkers() = runComposeUiTest {
        show(MarkdownEditorState(sample))
        onNode(shows("Title")).assertExists()
        onNode(shows("Some bold words")).assertExists()
        onNode(shows("val x = 1")).assertExists()
    }

    @Test
    fun theFocusedBlockRevealsItsMarkersInLivePreview() = runComposeUiTest {
        val state = MarkdownEditorState(sample)
        show(state)
        onNode(shows("Some bold words")).requestFocus()
        waitForIdle()
        onNode(shows("Some **bold** words")).assertExists()
    }

    @Test
    fun richTextNeverRevealsMarkers() = runComposeUiTest {
        val state = MarkdownEditorState(sample, MarkdownMode.Wysiwyg)
        show(state)
        onNode(shows("Some bold words")).requestFocus()
        waitForIdle()
        onNode(shows("Some bold words")).assertExists()
    }

    @Test
    fun theModeSwitchShowsOnlyWhenOffered() = runComposeUiTest {
        val state = MarkdownEditorState(sample)
        show(state, listOf(MarkdownMode.LivePreview, MarkdownMode.Wysiwyg))
        onNodeWithText("Rich text").performClick()
        waitForIdle()
        assertEquals(MarkdownMode.Wysiwyg, state.mode)
    }

    @Test
    fun oneModeHasNoSwitch() = runComposeUiTest {
        show(MarkdownEditorState(sample))
        onAllNodesWithText("Rich text").assertCountEquals(0)
    }

    @Test
    fun readOnlyHasNoToolbar() = runComposeUiTest {
        show(MarkdownEditorState(sample), readOnly = true)
        onNode(shows("Title")).assertExists()
        runCatching { onNodeWithContentDescription("Bold").assertExists() }.let { assertTrue(it.isFailure) }
    }

    @Test
    fun theFileIsUntouchedUntilEdited() = runComposeUiTest {
        val messy = "#  Spaced   title\n\n\n* star list\n+ plus list\n\nsome_text   \n"
        val state = MarkdownEditorState(messy)
        show(state, listOf(MarkdownMode.Wysiwyg))
        assertEquals(messy, state.text)
    }

    @Test
    fun theToolbarWrapsTheSelection() = runComposeUiTest {
        val state = MarkdownEditorState("hello world")
        show(state)
        onNodeWithText("hello world").requestFocus()
        waitForIdle()
        onNodeWithText("hello world").performTextInputSelection(TextRange(0, 5))
        waitForIdle()
        onNodeWithContentDescription("Bold").performClick()
        waitForIdle()
        assertEquals("**hello** world", state.text)
    }

    @Test
    fun undoReturnsToTheStart() = runComposeUiTest {
        val state = MarkdownEditorState("hello")
        show(state)
        runOnIdle {
            state.focusOn(state.document.blocks[0].id, 5)
            state.setBlockType(BlockType.Heading1)
        }
        waitForIdle()
        assertEquals("# hello", state.text)
        runOnIdle { state.undo() }
        waitForIdle()
        assertEquals("hello", state.text)
        assertTrue(state.canRedo)
        runOnIdle { state.redo() }
        assertEquals("# hello", state.text)
    }

    @Test
    fun blocksSelectTogether() = runComposeUiTest {
        val state = MarkdownEditorState("a\n\nb\n\nc")
        show(state)
        runOnIdle {
            state.selectAllBlocks()
            assertEquals("a\n\nb\n\nc", state.selectedSource)
            state.deleteSelectedBlocks()
        }
        assertEquals("", state.text)
        assertFalse(state.selectedBlocks != null)
    }

    @Test
    fun aTaskCheckBoxEditsTheSource() = runComposeUiTest {
        val state = MarkdownEditorState("- [ ] todo")
        show(state)
        runOnIdle { state.setChecked(state.document.blocks[0].id, true) }
        assertEquals("- [x] todo", state.text)
    }

    @Test
    fun typingThroughTheStateRunsAsOneUndoStep() = runComposeUiTest {
        val state = MarkdownEditorState("")
        show(state)
        runOnIdle {
            val id = state.document.blocks[0].id
            for (text in listOf("h", "he", "hel")) {
                state.onFieldChange(id, androidx.compose.ui.text.input.TextFieldValue(text, TextRange(text.length)))
            }
            state.undo()
            assertEquals("", state.text)
        }
    }

    @Test
    fun deletingInRichTextTakesAVisibleCharacterNotAMarker() {
        val old = androidx.compose.ui.text.input.TextFieldValue("a **bold** c", TextRange(10))
        val deletedStar = androidx.compose.ui.text.input.TextFieldValue("a **bold* c", TextRange(9))
        val source = old.text
        val fixed = deleteThroughMarkers(old, deletedStar, blockMarkup(source, classifyBlock(source)))
        assertEquals("a **bol** c", fixed.text)
    }

    @Test
    fun deletingTheLastStyledCharacterTakesItsMarkersToo() {
        val old = androidx.compose.ui.text.input.TextFieldValue("a **b** c", TextRange(7))
        val deletedStar = androidx.compose.ui.text.input.TextFieldValue("a **b* c", TextRange(6))
        val source = old.text
        assertEquals("a  c", deleteThroughMarkers(old, deletedStar, blockMarkup(source, classifyBlock(source))).text)
    }

    @Test
    fun theCaretSkipsHiddenMarkers() {
        val hidden = listOf(2..3, 8..9)
        // Moving right from before the opening marker lands on its far side.
        assertEquals(4, snapCaret(1, 2, hidden, 12))
        // Moving left from after the closing marker carries on past the visible letter before it.
        assertEquals(7, snapCaret(10, 9, hidden, 12))
        // A position with no marker stays.
        assertEquals(6, snapCaret(5, 6, hidden, 12))
    }

    @Test
    fun hiddenOffsetsMapBothWays() {
        val offsets = HiddenOffsets(8, listOf(0..1, 5..6))
        assertEquals(0, offsets.originalToTransformed(0))
        assertEquals(0, offsets.originalToTransformed(2))
        assertEquals(3, offsets.originalToTransformed(5))
        assertEquals(4, offsets.originalToTransformed(8))
        assertEquals(2, offsets.transformedToOriginal(0))
        assertEquals(7, offsets.transformedToOriginal(3))
        assertEquals(8, offsets.transformedToOriginal(4))
        for (at in 0..8) assertTrue(offsets.originalToTransformed(at) in 0..offsets.shownLength)
        for (at in 0..offsets.shownLength) assertTrue(offsets.transformedToOriginal(at) in 0..8)
    }

    @Test
    fun slashCommandsMatchPlainParagraphsOnly() {
        assertEquals("ta", slashQuery(MarkdownBlock(1, "/ta")))
        assertEquals(null, slashQuery(MarkdownBlock(1, "/ta ble")))
        assertEquals(null, slashQuery(MarkdownBlock(1, "- /ta")))
        assertEquals(listOf(BlockType.Task, BlockType.Table), slashMatches("ta", MarkdownStrings()))
    }
}
