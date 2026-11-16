package dev.chieppa.stretch.frame

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class OverlayHeightTest {
    @Test
    fun anOverlayTakesTheRoomLessTheMarginAndNeverMoreThanAsked() {
        assertEquals(492.dp, overlayHeightCap(500.dp))
        assertEquals(200.dp, overlayHeightCap(500.dp, 200.dp))
        assertEquals(OverlayMinHeight, overlayHeightCap(20.dp))
        assertEquals(40.dp, overlayHeightCap(20.dp, 40.dp))
    }
}
