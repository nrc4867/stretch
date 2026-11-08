package dev.chieppa.stretch.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ComponentsTest {
    @Test
    fun toggleFlips() = runComposeUiTest {
        var checked by mutableStateOf(false)
        setContent { StretchTheme(StretchThemes.Ink) { Toggle(checked, { checked = it }, "Snap") } }
        onNodeWithContentDescription("Snap").assertIsOff().performClick()
        onNodeWithContentDescription("Snap").assertIsOn()
        assertEquals(true, checked)
    }

    @Test
    fun segmentedSelectsOne() = runComposeUiTest {
        var view by mutableStateOf("Side")
        setContent {
            StretchTheme(StretchThemes.Ink) { Segmented(listOf("Side", "Top", "Under"), view, { view = it }, { it }) }
        }
        onNodeWithText("Top").performClick()
        onNodeWithText("Top").assertIsSelected()
        assertEquals("Top", view)
    }

    @Test
    fun aDisabledButtonTakesNoClicks() = runComposeUiTest {
        var clicks = 0
        setContent {
            StretchTheme(StretchThemes.Ink) {
                Column {
                    Button("Delete", { clicks++ }, enabled = false)
                    Button("Keep", { clicks += 10 })
                }
            }
        }
        onNodeWithText("Delete").assertIsNotEnabled().performClick()
        assertEquals(0, clicks)
        onNodeWithText("Keep").assertIsEnabled().performClick()
        assertEquals(10, clicks)
    }

    @Test
    fun aDisabledSegmentCannotBePicked() = runComposeUiTest {
        var view by mutableStateOf("Side")
        setContent {
            StretchTheme(StretchThemes.Ink) { Segmented(listOf("Side", "Top"), view, { view = it }, { it }, enabled = { it == "Side" }) }
        }
        onNodeWithText("Top").assertIsNotEnabled()
        onNodeWithText("Top").performClick()
        assertEquals("Side", view)
    }

    @Test
    fun sliderReportsLiveValuesThenFinishes() = runComposeUiTest {
        var value by mutableFloatStateOf(1f)
        var finished = 0
        setContent {
            StretchTheme(StretchThemes.Ink) {
                Slider(
                    value, { value = it }, "Size", Modifier.width(200.dp),
                    valueRange = 0f..8f, onValueChangeFinished = { finished++ },
                )
            }
        }
        onNodeWithContentDescription("Size").performSemanticsAction(SemanticsActions.SetProgress) { it(20f) }
        assertEquals(8f, value)
        assertEquals(1, finished)
    }

    @Test
    fun optionChipsToggleIndependently() = runComposeUiTest {
        var pads by mutableStateOf(false)
        setContent { StretchTheme(StretchThemes.Ink) { OptionChip("Pads", pads, { pads = it }) } }
        onNodeWithText("Pads").performClick()
        assertEquals(true, pads)
    }

    @Test
    fun everyComponentComposesInEveryTheme() = runComposeUiTest {
        var theme by mutableStateOf(StretchThemes.Ink)
        setContent {
            StretchTheme(theme) {
                Column {
                    Button("Export", {}, kind = ButtonKind.Primary)
                    IconButton(StretchIcons.Undo, "Undo", {})
                    ToolButton(StretchIcons.Pen, "Draw", true, {})
                    Toggle(true, {}, "Outline")
                    Slider(0.5f, {}, "Opacity", Modifier.width(120.dp))
                    Chip("Gap", kind = ChipKind.Warn)
                    Segmented(listOf("A", "B"), "A", {}, { it }, mode = true)
                }
            }
        }
        for (each in StretchThemes.all) {
            theme = each
            waitForIdle()
            onNodeWithText("Export").assertExists()
        }
    }
}
