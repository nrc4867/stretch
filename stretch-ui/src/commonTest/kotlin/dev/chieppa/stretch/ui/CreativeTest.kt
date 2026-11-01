package dev.chieppa.stretch.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class CreativeTest {
    @Test
    fun colorsRoundTripThroughHsvAndHex() {
        val ochre = parseHex("#E9A24B")!!
        assertEquals("#E9A24B", ochre.toHsv().toColor().toHex())
        assertEquals("#EEAA44", parseHex("ea4")!!.toHex())
        assertNull(parseHex("#12345"))
        assertNull(parseHex("zzzzzz"))
        val hsv = Color.Red.toHsv()
        assertEquals(0f, hsv.h)
        assertEquals(1f, hsv.s)
    }

    @Test
    fun rampBandsAndStopsStayInOrder() {
        val stops = listOf(RampStop(0f, Color.Black), RampStop(0.4f, Color.Gray), RampStop(0.8f, Color.White))
        assertEquals(0, bandAt(stops, 0.1f))
        assertEquals(1, bandAt(stops, 0.5f))
        assertEquals(2, bandAt(stops, 0.95f))
        assertEquals(0.79f, stops.movedStop(1, 0.95f)[1].position, 0.0001f)
        assertEquals(0f, stops.movedStop(0, 0.3f)[0].position)
    }

    @Test
    fun curvesPassThroughTheirPointsWithoutOvershoot() {
        val points = listOf(Offset(0f, 0f), Offset(0.3f, 0.6f), Offset(0.7f, 0.65f), Offset(1f, 1f))
        for (p in points) assertEquals(p.y, evaluateCurve(points, p.x), 0.001f)
        val samples = (0..100).map { evaluateCurve(points, it / 100f) }
        assertTrue(samples.zipWithNext().all { (a, b) -> b >= a - 1e-4f }, "Monotone points give a monotone curve")
        assertEquals(0.5f, evaluateCurve(listOf(Offset(0f, 0f), Offset(1f, 1f)), 0.5f), 0.001f)
    }

    @Test
    fun anglesMeasureCounterclockwiseFromRight() {
        val center = Offset(50f, 50f)
        assertEquals(0f, angleOf(Offset(90f, 50f), center), 0.01f)
        assertEquals(90f, angleOf(Offset(50f, 10f), center), 0.01f)
        assertEquals(270f, angleOf(Offset(50f, 90f), center), 0.01f)
        assertEquals(45f, snapAngle(52f, 15f))
    }

    @Test
    fun pickerTakesHexAndKeys() = runComposeUiTest {
        var color by mutableStateOf(parseHex("#E9A24B")!!)
        setContent { StretchTheme(StretchThemes.Ink) { ColorPicker(color, { color = it }) } }
        onNodeWithContentDescription("Hex").performTextReplacement("#3366cc")
        assertEquals("#3366CC", color.toHex())
        val before = color.toHsv().h
        onNodeWithContentDescription("Hue").requestFocus()
        onNodeWithContentDescription("Hue").performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.DirectionRight) } }
        assertTrue(abs(color.toHsv().h - (before + 10f)) < 1f, "Hue ${color.toHsv().h}")
    }

    @Test
    fun dialTurnsWithKeysAndDrag() = runComposeUiTest {
        var angle by mutableFloatStateOf(0f)
        setContent { StretchTheme(StretchThemes.Ink) { AngleDial(angle, { angle = it }, "Light direction") } }
        onNodeWithContentDescription("Light direction").requestFocus()
        onNodeWithContentDescription("Light direction").performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.DirectionUp) } }
        assertEquals(15f, angle)
        onNodeWithContentDescription("Light direction").performTouchInput {
            down(center)
            moveTo(topCenter)
            up()
        }
        assertEquals(90f, angle, 1f)
    }

    @Test
    fun rampAndCurveEditWithKeys() = runComposeUiTest {
        var stops by mutableStateOf(listOf(RampStop(0f, Color.Black), RampStop(0.5f, Color.Gray), RampStop(0.8f, Color.White)))
        var picked by mutableIntStateOf(1)
        var points by mutableStateOf(listOf(Offset(0f, 0f), Offset(1f, 1f)))
        setContent {
            StretchTheme(StretchThemes.Ink) {
                androidx.compose.foundation.layout.Column {
                    RampEditor(stops, { stops = it }, picked, { picked = it })
                    CurveEditor(points, { points = it }, description = "Pressure")
                }
            }
        }
        onNodeWithContentDescription("Shading ramp").requestFocus()
        onNodeWithContentDescription("Shading ramp").performKeyInput {
            withKeyDown(Key.ShiftLeft) { pressKey(Key.DirectionRight) }
            pressKey(Key.Delete)
        }
        assertEquals(2, stops.size)
        assertEquals(0, picked)
        onNodeWithContentDescription("Pressure").performTouchInput { click(center) }
        assertEquals(3, points.size)
        onNodeWithContentDescription("Pressure").performKeyInput { pressKey(Key.DirectionUp) }
        assertTrue(points[1].y > 0.5f)
    }
}
