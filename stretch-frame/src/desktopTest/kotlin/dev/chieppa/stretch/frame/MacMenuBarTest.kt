package dev.chieppa.stretch.frame

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import dev.chieppa.stretch.core.KeyChord
import java.awt.event.KeyEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MacMenuBarTest {
    @Test
    fun ctrlInAChordIsCmdInTheMacMenu() {
        assertEquals(KeyShortcut(Key(KeyEvent.VK_S), meta = true, shift = true), menuShortcut(KeyChord.parse("Ctrl Shift S"), mac = true))
        assertEquals(KeyShortcut(Key(KeyEvent.VK_S), ctrl = true), menuShortcut(KeyChord.parse("Ctrl S"), mac = false))
    }

    @Test
    fun keyNamesMapBackToKeyCodes() {
        assertEquals(KeyEvent.VK_DELETE, awtKeyCode("Delete"))
        assertEquals(KeyEvent.VK_F2, awtKeyCode("F2"))
        assertEquals(KeyEvent.VK_K, awtKeyCode("K"))
        assertNull(awtKeyCode("NoSuchKey"))
    }
}
