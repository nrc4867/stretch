package dev.chieppa.stretch.ui

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test
import kotlin.test.assertTrue

/** Controls that run along a line read right to left in a right-to-left layout, by pointer and by key. */
@OptIn(ExperimentalTestApi::class)
class RtlTest {
    @Composable
    private fun Rtl(content: @Composable () -> Unit) {
        StretchTheme(StretchThemes.Ink) { CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, content = content) }
    }

    @Test
    fun aSliderFillsFromTheRight() = runComposeUiTest {
        var value by mutableFloatStateOf(0.5f)
        setContent { Rtl { Slider(value, { value = it }, "Size", Modifier.width(200.dp)) } }
        onNodeWithContentDescription("Size").performTouchInput { click(Offset(width - 2f, centerY)) }
        assertTrue(value < 0.05f, "the right end is the start: $value")
        onNodeWithContentDescription("Size").performTouchInput { click(Offset(2f, centerY)) }
        assertTrue(value > 0.95f, "the left end is the end: $value")
        val before = value
        onNodeWithContentDescription("Size").requestFocus()
        onNodeWithContentDescription("Size").performKeyInput { pressKey(Key.DirectionRight) }
        assertTrue(value < before, "Right moves the knob right, toward the start: $value")
    }

    @Test
    fun aRangeSliderStartsOnTheRight() = runComposeUiTest {
        var range by mutableStateOf(0.2f..0.8f)
        setContent { Rtl { RangeSlider(range, { range = it }, "Taper start", "Taper end", Modifier.width(200.dp)) } }
        val start = onNodeWithContentDescription("Taper start").fetchSemanticsNode().boundsInRoot
        val end = onNodeWithContentDescription("Taper end").fetchSemanticsNode().boundsInRoot
        assertTrue(start.left > end.left, "the start knob is on the right: $start $end")
        val before = range
        onNodeWithContentDescription("Taper start").requestFocus()
        onNodeWithContentDescription("Taper start").performKeyInput { pressKey(Key.DirectionLeft) }
        assertTrue(range.start > before.start, "Left moves it toward the end: $range")
    }

    @Test
    fun numbersKeepTheirUnitAfterThem() = runComposeUiTest {
        setContent { Rtl { NumberField(18f, {}, "Spread", unit = "°") } }
        val number = onNodeWithText("18", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val unit = onNodeWithText("°", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue(unit.left >= number.right - 0.5f, "18° reads left to right: $number $unit")
        onNodeWithContentDescription("Spread").performClick()
    }
}
