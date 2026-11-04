package dev.chieppa.stretch.ui

import androidx.compose.ui.awt.ComposeDialog
import java.awt.Dialog
import javax.swing.JFrame
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PopupWindowPoolTest {
    private fun withOwner(block: (JFrame) -> Unit) = SwingUtilities.invokeAndWait {
        val owner = JFrame().apply { pack() }
        try {
            block(owner)
        } finally {
            owner.dispose()
        }
    }

    private fun popup(owner: JFrame) = ComposeDialog(owner, Dialog.ModalityType.MODELESS)

    @Test
    fun aReleasedWindowComesBackForItsOwnerOnly() = withOwner { owner ->
        val dialog = popup(owner)
        PopupWindowPool.release(dialog)
        val other = JFrame().apply { pack() }
        assertNull(PopupWindowPool.take(other))
        other.dispose()
        assertSame(dialog, PopupWindowPool.take(owner))
        assertNull(PopupWindowPool.take(owner))
        dialog.dispose()
    }

    @Test
    fun aPopupWindowHasNoDragHandles() = withOwner { owner ->
        val dialog = newPopupWindow(owner, focusable = false)
        assertFalse(dialog.isResizable)
        dialog.dispose()
    }

    @Test
    fun aFullPoolDisposesWhatIsLeft() = withOwner { owner ->
        val released = List(17) { popup(owner).apply { pack() } }
        released.forEach(PopupWindowPool::release)
        assertFalse(released.last().isDisplayable)
        assertTrue(released.dropLast(1).all { it.isDisplayable })
        assertEquals(16, generateSequence { PopupWindowPool.take(owner) }.count())
    }

    @Test
    fun aWindowWhoseOwnerIsGoneIsDisposed() {
        SwingUtilities.invokeAndWait {
            val owner = JFrame().apply { pack() }
            val dialog = popup(owner).apply { pack() }
            owner.dispose()
            PopupWindowPool.release(dialog)
            assertFalse(dialog.isDisplayable)
            assertNull(PopupWindowPool.take(owner))
        }
    }
}
