package dev.chieppa.stretch.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.StretchShadow
import java.awt.Point
import java.awt.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PopupWindowGeometryTest {
    private val hard = StretchShadow(4.dp, 4.dp, 0.dp, Color.Black)
    private val soft = StretchShadow(0.dp, 10.dp, 28.dp, Color.Black)

    @Test
    fun marginsLeaveRoomOnTheSidesAShadowReaches() {
        assertEquals(PopupMargins.None, PopupMargins.around(null))
        assertEquals(PopupMargins(0.dp, 0.dp, 4.dp, 4.dp), PopupMargins.around(hard))
        assertEquals(PopupMargins(28.dp, 18.dp, 28.dp, 38.dp), PopupMargins.around(soft))
    }

    @Test
    fun clipKeepsThePopupInsideTheWindow() {
        val window = IntSize(100, 100)
        assertEquals(IntOffset(70, 0), clipToWindow(IntOffset(90, -5), IntSize(30, 30), window))
        assertEquals(IntOffset(0, 0), clipToWindow(IntOffset(50, 50), IntSize(120, 120), window))
    }

    @Test
    fun boundsConvertPixelsToWindowUnitsAndIncludeTheMargins() {
        val bounds = popupWindowBounds(Point(10, 20), IntOffset(100, 200), IntSize(300, 400), PopupMargins.around(hard), Density(2f))
        assertEquals(Rectangle(60, 120, 154, 204), bounds)
    }

    @Test
    fun boundsRoundOutwardAtAFractionalScale() {
        val bounds = popupWindowBounds(Point(0, 0), IntOffset(101, 101), IntSize(101, 101), PopupMargins.None, Density(1.5f))
        assertEquals(Rectangle(67, 67, 68, 68), bounds)
    }

    @Test
    fun theShadowsRoomIsNotContent() {
        val margins = PopupMargins.around(soft)
        val size = IntSize(100, 100)
        val density = Density(1f)
        assertTrue(isOnContent(Point(28, 18), size, margins, density))
        assertFalse(isOnContent(Point(27, 18), size, margins, density))
        assertFalse(isOnContent(Point(128, 50), size, margins, density))
    }
}
