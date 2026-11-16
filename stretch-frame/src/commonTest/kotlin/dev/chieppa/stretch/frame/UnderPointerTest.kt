package dev.chieppa.stretch.frame

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.test.Test
import kotlin.test.assertEquals

class UnderPointerTest {
    @Test
    fun aPreviewPopupHoldsItsGrabUnderThePointer() {
        val host = IntRect(IntOffset(40, 30), IntSize(800, 600))
        val at = UnderPointer(Offset(200f, 100f), Offset(0.5f, 0.25f))
            .calculatePosition(host, IntSize(1000, 800), LayoutDirection.Ltr, IntSize(60, 40))
        assertEquals(IntOffset(40 + 200 - 30, 30 + 100 - 10), at)
    }

    @Test
    fun aPreviewPopupWithRoomHoldsThePreviewNotTheRoomUnderThePointer() {
        val host = IntRect(IntOffset(40, 30), IntSize(800, 600))
        val at = UnderPointer(Offset(200f, 100f), Offset(0.5f, 0.25f), room = 8)
            .calculatePosition(host, IntSize(1000, 800), LayoutDirection.Ltr, IntSize(60 + 16, 40 + 16))
        assertEquals(IntOffset(40 + 200 - 30 - 8, 30 + 100 - 10 - 8), at)
    }
}
