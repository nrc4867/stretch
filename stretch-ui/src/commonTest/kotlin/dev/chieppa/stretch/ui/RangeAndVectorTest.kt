package dev.chieppa.stretch.ui

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class RangeAndVectorTest {
    @Test
    fun rangeKnobsStepAndNeverCross() = runComposeUiTest {
        var range by mutableStateOf(0.2f..0.6f)
        var finished = 0
        setContent {
            StretchTheme(StretchThemes.Ink) {
                RangeSlider(range, { range = it }, "Start", "End", Modifier.width(300.dp).testTag("range"), onValueChangeFinished = { finished++ })
            }
        }
        onNodeWithContentDescription("Start").requestFocus()
        onNodeWithContentDescription("Start").performKeyInput { pressKey(Key.DirectionRight) }
        assertEquals(0.21f, range.start, 0.001f)
        onNodeWithContentDescription("Start").performKeyInput { pressKey(Key.MoveEnd) }
        assertEquals(0.6f, range.start, 0.001f, "the start knob stops at the end knob")
        onNodeWithContentDescription("End").performSemanticsAction(SemanticsActions.SetProgress) { it(0.1f) }
        assertEquals(0.6f..0.6f, range, "nor can the end knob pass the start")
        assertTrue(finished >= 3)
    }

    @Test
    fun clickingTheTrackMovesTheNearerKnob() = runComposeUiTest {
        var range by mutableStateOf(0.2f..0.6f)
        setContent {
            StretchTheme(StretchThemes.Ink) { RangeSlider(range, { range = it }, "Start", "End", Modifier.width(300.dp).testTag("range")) }
        }
        onNodeWithTag("range").performTouchInput { down(Offset(width * 0.9f, centerY)); up() }
        assertEquals(0.2f, range.start, 0.001f)
        assertTrue(range.endInclusive > 0.85f, "end moved to ${range.endInclusive}")
        onNodeWithTag("range").performTouchInput { down(Offset(width * 0.05f, centerY)); up() }
        assertTrue(range.start < 0.1f, "start moved to ${range.start}")
    }

    @Test
    fun vectorFieldsReportTheirAxis() = runComposeUiTest {
        var values by mutableStateOf(listOf(1f, 2f, 3f))
        setContent {
            StretchTheme(StretchThemes.Ink) {
                VectorField(values, { axis, v -> values = values.toMutableList().also { it[axis] = v } }, "Position")
            }
        }
        onNodeWithContentDescription("Position Y").performClick()
        onNodeWithContentDescription("Position Y").performTextInput("7")
        onNodeWithContentDescription("Position Y").performKeyInput { pressKey(Key.Enter) }
        assertEquals(listOf(1f, 7f, 3f), values)
    }
}
