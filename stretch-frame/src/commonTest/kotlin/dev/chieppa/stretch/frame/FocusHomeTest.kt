package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.test.setStretchContent
import dev.chieppa.stretch.ui.Button
import dev.chieppa.stretch.ui.NumberField
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.TextRange
import dev.chieppa.stretch.ui.TextField
import dev.chieppa.stretch.ui.focusHome
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class FocusHomeTest {
    @Test
    fun clickingEmptySpaceTakesFocusOffFieldsAndButtons() = runComposeUiTest {
        var text by mutableStateOf("")
        var presses = 0
        setStretchContent {
            Column(Modifier.focusHome().testTag("home")) {
                TextField(text, { text = it }, placeholder = "Name")
                Button("Export", { presses++ })
                Box(Modifier.size(200.dp).testTag("empty"))
            }
        }
        onNodeWithContentDescription("Name").performClick()
        onNodeWithContentDescription("Name").assertIsFocused()
        onNodeWithTag("empty").performClick()
        onNodeWithContentDescription("Name").assertIsNotFocused()
        onNodeWithText("Export").performClick()
        assertEquals(1, presses)
        onNodeWithTag("empty").performClick()
        onNodeWithTag("home").performKeyInput { pressKey(Key.Spacebar) }
        assertEquals(1, presses)
    }

    @Test
    fun numberFieldEditsWithItsTextSelectedAndSurvivesClicksInside() = runComposeUiTest {
        var spread by mutableStateOf(68f)
        setStretchContent {
            Column(Modifier.focusHome()) {
                NumberField(spread, { spread = it }, "Spread", unit = "°")
                Box(Modifier.size(200.dp).testTag("empty"))
            }
        }
        onNodeWithContentDescription("Spread").performClick()
        onNodeWithContentDescription("Spread").assertIsFocused()
        onNodeWithContentDescription("Spread").assert(SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, TextRange(0, 2)))
        onNodeWithContentDescription("Spread").performTextInput("42")
        onNodeWithContentDescription("Spread").performClick()
        onNodeWithContentDescription("Spread").assertIsFocused()
        onNodeWithTag("empty").performClick()
        assertEquals(42f, spread)
    }
}
