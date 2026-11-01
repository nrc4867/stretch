package dev.chieppa.stretch.frame

import kotlin.test.Test
import kotlin.test.assertEquals

class NavigationTest {
    @Test
    fun zoomStepsThroughPresets() {
        assertEquals(1.5f, nextZoom(1f, up = true))
        assertEquals(0.75f, nextZoom(1f, up = false))
        assertEquals(1f, nextZoom(0.8f, up = true))
        assertEquals(8f, nextZoom(8f, up = true))
        assertEquals(0.1f, nextZoom(0.1f, up = false))
    }
}
