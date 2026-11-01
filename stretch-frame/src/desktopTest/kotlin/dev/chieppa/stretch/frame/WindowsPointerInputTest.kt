package dev.chieppa.stretch.frame

import androidx.compose.ui.input.pointer.PointerButton
import dev.chieppa.stretch.frame.PointerUser32.Companion.WM_POINTERDOWN
import dev.chieppa.stretch.frame.PointerUser32.Companion.WM_POINTERUP
import dev.chieppa.stretch.frame.PointerUser32.Companion.WM_POINTERUPDATE
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WindowsPointerInputTest {
    @Test
    fun aPenStrokeIsHandledButHoverIsLeftToWindows() {
        assertTrue(isPenContact(WM_POINTERDOWN, contact = true, down = false), "the press")
        assertTrue(isPenContact(WM_POINTERUPDATE, contact = true, down = true), "moves in contact")
        assertTrue(isPenContact(WM_POINTERUP, contact = false, down = true), "the lift")
        assertFalse(isPenContact(WM_POINTERUPDATE, contact = false, down = false), "hovering")
        assertFalse(isPenContact(WM_POINTERUPDATE, contact = true, down = false), "a stroke that began before this was watching")
        assertFalse(isPenContact(WM_POINTERUP, contact = false, down = false), "a lift for a press this never saw")
    }

    @Test
    fun theBarrelButtonPressesTheSecondaryButton() {
        assertEquals(PointerButton.Primary, penButton(barrel = false))
        assertEquals(PointerButton.Secondary, penButton(barrel = true))
    }
}
