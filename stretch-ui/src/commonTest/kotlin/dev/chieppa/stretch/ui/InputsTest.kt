package dev.chieppa.stretch.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class InputsTest {
    @Test
    fun numbersFormatWithoutTrailingZeros() {
        assertEquals("18", formatNumber(18f))
        assertEquals("2.5", formatNumber(2.5f))
        assertEquals("0.33", formatNumber(1f / 3))
        assertEquals("-4", formatNumber(-4.001f))
    }

    @Test
    fun numberFieldStepsWithArrowsAndShift() = runComposeUiTest {
        var value by mutableFloatStateOf(18f)
        var finished = 0
        setContent {
            StretchTheme(StretchThemes.Ink) {
                NumberField(value, { value = it }, "Spread", unit = "°", range = 0f..90f, onValueChangeFinished = { finished++ })
            }
        }
        val field = onNodeWithContentDescription("Spread")
        field.requestFocus()
        field.performKeyInput { pressKey(Key.DirectionUp) }
        assertEquals(19f, value)
        field.performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.DirectionUp) } }
        assertEquals(29f, value)
        field.performKeyInput { withKeyDown(Key.ShiftLeft) { repeat(10) { pressKey(Key.DirectionUp) } } }
        assertEquals(90f, value)
        assertEquals(12, finished)
    }

    @Test
    fun numberFieldCommitsTypedValue() = runComposeUiTest {
        var value by mutableFloatStateOf(2f)
        setContent { StretchTheme(StretchThemes.Ink) { NumberField(value, { value = it }, "Scale", range = 1f..8f) } }
        onNodeWithContentDescription("Scale").performClick()
        onNodeWithContentDescription("Scale").performTextClearance()
        onNodeWithContentDescription("Scale").performTextInput("12")
        onNodeWithContentDescription("Scale").performKeyInput { pressKey(Key.Enter) }
        waitForIdle()
        assertEquals(8f, value)
    }

    @Test
    fun swatchSelects() = runComposeUiTest {
        var picked by mutableStateOf("Ink")
        setContent {
            StretchTheme(StretchThemes.Ink) {
                Swatch(Color(0xFFE9A24B), "Ochre", picked == "Ochre", { picked = "Ochre" })
            }
        }
        onNodeWithContentDescription("Ochre").performClick()
        onNodeWithContentDescription("Ochre").assertIsSelected()
    }

    @Test
    fun calloutOffersItsFix() = runComposeUiTest {
        var fixed = false
        setContent { StretchTheme(StretchThemes.Ink) { Callout("Gap in outline", "Close gap", { fixed = true }, value = "0.4 px") } }
        onNodeWithText("Close gap").performClick()
        assertTrue(fixed)
    }

    @Test
    fun textFieldEdits() = runComposeUiTest {
        var text by mutableStateOf("")
        setContent { StretchTheme(StretchThemes.Ink) { TextField(text, { text = it }, placeholder = "Search") } }
        onNodeWithContentDescription("Search").performTextInput("ear")
        assertEquals("ear", text)
    }

    @Test
    fun aMaskedFieldHidesItsTextButTakesTyping() = runComposeUiTest {
        var text by mutableStateOf("")
        setContent { StretchTheme(StretchThemes.Ink) { TextField(text, { text = it }, placeholder = "Token", masked = true) } }
        val field = onNodeWithContentDescription("Token")
        field.performTextInput("hunter2")
        assertEquals("hunter2", text)
        field.assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password))
        field.assert(SemanticsMatcher("shows no text") { node -> "hunter" !in (node.config.getOrElseNullable(SemanticsProperties.EditableText) { null }?.text ?: "") })
    }
}
