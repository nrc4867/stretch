package dev.chieppa.stretch.gpu

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import dev.chieppa.stretch.frame.buildOverlay
import dev.chieppa.stretch.gpu.OverlayGeometry.FLOATS_PER_VERTEX
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OverlayGeometryTest {
    private fun FloatArray.positions() = (indices step FLOATS_PER_VERTEX).map { Offset(this[it], this[it + 1]) }

    @Test
    fun aRectIsTwoTrianglesCoveringIt() {
        val triangles = OverlayGeometry.triangles(buildOverlay { rect(Rect(10f, 20f, 30f, 60f), Color.Red) })
        val points = triangles.positions()
        assertEquals(6, points.size)
        assertEquals(setOf(Offset(10f, 20f), Offset(30f, 20f), Offset(30f, 60f), Offset(10f, 60f)), points.toSet())
        assertEquals(listOf(1f, 0f, 0f, 1f), triangles.slice(2..5))
    }

    @Test
    fun aLineIsAQuadAsWideAsTheLine() {
        val points = OverlayGeometry.triangles(buildOverlay { line(Offset(0f, 10f), Offset(100f, 10f), Color.Black, width = 4f) }).positions()
        assertEquals(6, points.size)
        assertEquals(12f, points.maxOf { it.y })
        assertEquals(8f, points.minOf { it.y })
        assertEquals(setOf(0f, 100f), points.map { it.x }.toSet())
    }

    @Test
    fun aZeroLengthLineDrawsNothing() {
        assertEquals(0, OverlayGeometry.triangles(buildOverlay { line(Offset(5f, 5f), Offset(5f, 5f), Color.Black) }).size)
    }

    @Test
    fun aDotIsAFanWithinItsRadius() {
        val center = Offset(50f, 50f)
        val points = OverlayGeometry.triangles(buildOverlay { dot(center, 6f, Color.White) }).positions()
        assertEquals(0, points.size % 3)
        assertTrue(points.size >= 36)
        assertTrue(points.all { (it - center).getDistance() <= 6.001f })
    }

    @Test
    fun rectsComeBeforeLinesAndLinesBeforeDots() {
        val overlay = buildOverlay {
            dot(Offset(0f, 0f), 2f, Color.Blue)
            line(Offset(0f, 0f), Offset(1f, 0f), Color.Green)
            rect(Rect(0f, 0f, 1f, 1f), Color.Red)
        }
        val triangles = OverlayGeometry.triangles(overlay)
        assertEquals(Color.Red.red, triangles[2])
        assertEquals(Color.Green.green, triangles[6 * FLOATS_PER_VERTEX + 3])
    }
}
