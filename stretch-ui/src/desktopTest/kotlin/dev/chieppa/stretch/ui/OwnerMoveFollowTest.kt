package dev.chieppa.stretch.ui

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import java.awt.Window
import javax.swing.JFrame
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals

/** Popup windows move with their window in the step Windows moves it, without waiting on the UI thread. */
class OwnerMoveFollowTest {
    private val windows = System.getProperty("os.name").startsWith("Windows")

    private fun hwnd(window: Window) = WinDef.HWND(Pointer(Native.getComponentID(window)))

    private fun at(window: Window) = WinDef.RECT().also { User32.INSTANCE.GetWindowRect(hwnd(window), it) }.let { it.left to it.top }

    @Test
    fun aPopupMovesWithItsOwnerWhileTheUiThreadIsBusy() {
        if (!windows) return
        SwingUtilities.invokeAndWait {
            val owner = JFrame().apply { pack(); setBounds(100, 100, 400, 300) }
            val popup = newPopupWindow(owner, focusable = false).apply { pack(); setBounds(150, 160, 50, 40) }
            try {
                OwnerMoveFollow.start(owner, popup)
                val (x, y) = at(owner)
                val (px, py) = at(popup)
                // Moved natively from the UI thread, which handles none of AWT's move events until this block ends.
                User32.INSTANCE.SetWindowPos(hwnd(owner), null, x + 30, y + 20, 0, 0, WinUser.SWP_NOSIZE or WinUser.SWP_NOZORDER or WinUser.SWP_NOACTIVATE)
                assertEquals(px + 30 to py + 20, at(popup))
                OwnerMoveFollow.stop(popup)
                User32.INSTANCE.SetWindowPos(hwnd(owner), null, x, y, 0, 0, WinUser.SWP_NOSIZE or WinUser.SWP_NOZORDER or WinUser.SWP_NOACTIVATE)
                assertEquals(px + 30 to py + 20, at(popup), "a popup no longer following stays")
            } finally {
                popup.dispose()
                owner.dispose()
            }
        }
    }
}
