package dev.chieppa.stretch.frame

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import dev.chieppa.stretch.core.KeyChord
import dev.chieppa.stretch.test.setStretchContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class ShortcutFieldTest {
    @Test
    fun recordsTheNextChordAndEscCancels() = runComposeUiTest {
        var chord by mutableStateOf<KeyChord?>(KeyChord.parse("Ctrl E"))
        setStretchContent { ShortcutField(chord, { chord = it }, "Export shortcut") }
        val field = onNodeWithContentDescription("Export shortcut")
        field.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Ctrl E"))

        field.performClick()
        field.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Press keys"))
        field.performKeyInput { withKeyDown(Key.CtrlLeft) { withKeyDown(Key.ShiftLeft) { pressKey(Key.K) } } }
        assertEquals(KeyChord.parse("Ctrl Shift K"), chord)
        field.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Ctrl Shift K"))

        field.performClick()
        field.performKeyInput { pressKey(Key.Escape) }
        assertEquals(KeyChord.parse("Ctrl Shift K"), chord, "Esc keeps the shortcut")

        field.performClick()
        field.performKeyInput { pressKey(Key.Backspace) }
        assertNull(chord, "Backspace clears it")
        field.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "None"))
    }
}
