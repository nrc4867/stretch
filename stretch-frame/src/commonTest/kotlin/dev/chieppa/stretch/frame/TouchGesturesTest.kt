package dev.chieppa.stretch.frame

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TouchGesturesTest {
    private fun assertNear(expected: Float, actual: Float, slack: Float = 1e-3f) =
        assertTrue(abs(expected - actual) <= slack, "expected $expected, was $actual")

    @Test
    fun theCentreAndSpreadAreMeans() {
        val points = listOf(Offset(0f, 0f), Offset(10f, 0f))
        assertEquals(Offset(5f, 0f), centroid(points))
        assertNear(5f, spread(points, Offset(5f, 0f)))
    }

    @Test
    fun twistIsTheMeanTurnClockwiseOnScreen() {
        val before = listOf(Offset(-10f, 0f), Offset(10f, 0f))
        assertNear(90f, twistDegrees(before, listOf(Offset(0f, -10f), Offset(0f, 10f))), 0.01f)
        assertNear(-45f, twistDegrees(before, listOf(Offset(-7f, 7f), Offset(7f, -7f))), 0.01f)
        assertNear(0f, twistDegrees(before, listOf(Offset(-20f, 5f), Offset(0f, 5f))))
    }

    @Test
    fun twistTakesTheShortWayAcrossTheHalfTurn() {
        val before = listOf(Offset(-10f, 1f), Offset(10f, -1f))
        val after = listOf(Offset(-10f, -1f), Offset(10f, 1f))
        assertTrue(twistDegrees(before, after) in 0f..20f, "a small clockwise turn, not nearly a full one: ${twistDegrees(before, after)}")
    }

    @Test
    fun fingersAtTheCentreHaveNoTwist() {
        assertEquals(0f, twistDegrees(listOf(Offset(5f, 5f), Offset(5f, 5f)), listOf(Offset(5f, 5f), Offset(5f, 5f))))
    }
}
