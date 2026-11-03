package dev.chieppa.stretch.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class MoreFieldsTest {
    @Test
    fun widthsThatFitAreLeftAlone() {
        assertEquals(listOf(10, 20), fitWidths(listOf(10, 20), 30))
    }

    @Test
    fun theLongestNamesShrinkFirst() {
        assertEquals(listOf(10, 45, 45), fitWidths(listOf(10, 200, 100), 100))
        assertEquals(listOf(10, 40), fitWidths(listOf(10, 200), 50))
        assertEquals(listOf(0, 0), fitWidths(listOf(10, 20), 0))
    }

    @Test
    fun breadcrumbsLinkBackButNotToTheCurrentPlace() = runComposeUiTest {
        var clicked: String? = null
        setContent {
            StretchTheme(StretchThemes.Ink) {
                Breadcrumbs(listOf("Dune walk", "Fennec", "Rig"), { it }, { clicked = it })
            }
        }
        onNodeWithText("Fennec").performClick()
        assertEquals("Fennec", clicked)
        assert(!hasClickAction().matches(onNodeWithText("Rig").fetchSemanticsNode()))
    }

    @Test
    fun textAreaTakesLinesAndTabMovesOn() = runComposeUiTest {
        var notes by mutableStateOf("")
        setContent {
            StretchTheme(StretchThemes.Ink) {
                Column {
                    TextArea(notes, { notes = it }, Modifier.width(300.dp), placeholder = "Notes")
                    TextArea("", {}, Modifier.width(300.dp), placeholder = "Other")
                }
            }
        }
        val area = onNodeWithContentDescription("Notes")
        area.performTextInput("one\ntwo")
        assertEquals("one\ntwo", notes)
        area.requestFocus().performKeyInput { pressKey(Key.Tab) }
        assertEquals("one\ntwo", notes)
        onNodeWithContentDescription("Other").assertIsFocused()
    }

    @Test
    fun numberFieldHasNoStepButtonsUnlessAskedFor() = runComposeUiTest {
        setContent {
            StretchTheme(StretchThemes.Ink) { NumberField(1f, {}, "Frames") }
        }
        onNodeWithContentDescription("More").assertDoesNotExist()
    }

    @Test
    fun stepButtonsMoveByStepAndStopAtTheRange() = runComposeUiTest {
        var value by mutableFloatStateOf(9f)
        setContent {
            StretchTheme(StretchThemes.Ink) {
                NumberField(value, { value = it }, "Frames", step = 1f, range = 0f..10f, steppers = NumberSteppers("Less", "More"))
            }
        }
        onNodeWithContentDescription("Less").performClick()
        assertEquals(8f, value)
        onNodeWithContentDescription("More").performClick()
        onNodeWithContentDescription("More").performClick()
        assertEquals(10f, value)
        onNodeWithContentDescription("More").assertIsNotEnabled()
    }
}
