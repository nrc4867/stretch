package dev.chieppa.stretch.frame

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import dev.chieppa.stretch.core.Action
import dev.chieppa.stretch.core.ActionId
import dev.chieppa.stretch.core.ActionRegistry
import dev.chieppa.stretch.core.Applied
import dev.chieppa.stretch.core.Command
import dev.chieppa.stretch.core.History
import dev.chieppa.stretch.core.KeyChord
import dev.chieppa.stretch.core.Keymap
import dev.chieppa.stretch.test.setStretchContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private data class SetTo(val value: Int) : Command<Int> {
    override fun apply(document: Int) = Applied(value, SetTo(document))
}

@OptIn(ExperimentalTestApi::class)
class PanelsTest {
    @Test
    fun historyRowsUndoAndRedoToTheStepClicked() = runComposeUiTest {
        val history = History(0)
        history.execute(SetTo(1))
        history.execute(SetTo(2))
        history.execute(SetTo(3))
        setStretchContent { HistoryPanel(history, { "Set ${(it as SetTo).value}" }) }
        onNodeWithText("Original").performClick()
        assertEquals(0, history.document.value)
        onNodeWithText("Set 2").performClick()
        assertEquals(2, history.document.value, "a dimmed step redoes up to it")
        onNodeWithText("Set 3").performClick()
        assertEquals(3, history.document.value)
    }

    @Test
    fun keymapEditorRebindsWarnsOnClashesAndResets() = runComposeUiTest {
        val registry = ActionRegistry().apply {
            register(Action(ActionId("draw"), "Draw", keys = listOf(KeyChord.parse("B"))) {})
            register(Action(ActionId("erase"), "Erase", keys = listOf(KeyChord.parse("E"))) {})
        }
        val keymap = Keymap(registry)
        var changes = 0
        setStretchContent { KeymapEditor(keymap, registry.all.toList(), onChange = { changes++ }) }

        onNodeWithContentDescription("Erase shortcut").performClick()
        onNodeWithContentDescription("Erase shortcut").performKeyInput { pressKey(Key.B) }
        assertEquals(listOf(KeyChord.parse("B")), keymap.keysFor(ActionId("erase")))
        assertEquals(1, changes)
        onNodeWithContentDescription("Also the shortcut for Draw").assertExists()
        onNodeWithContentDescription("Also the shortcut for Erase").assertExists()

        onNodeWithContentDescription("Reset to default").performClick()
        assertEquals(listOf(KeyChord.parse("E")), keymap.keysFor(ActionId("erase")))
        onNodeWithContentDescription("Also the shortcut for Draw").assertDoesNotExist()

        onNodeWithContentDescription("Search shortcuts").performTextInput("dra")
        onNodeWithText("Erase").assertDoesNotExist()
        onNodeWithText("Draw").assertExists()
        assertTrue(keymap.changes.isEmpty())
    }
}
