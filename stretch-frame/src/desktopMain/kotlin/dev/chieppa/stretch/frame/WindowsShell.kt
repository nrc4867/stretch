package dev.chieppa.stretch.frame

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.ptr.IntByReference
import java.awt.Frame
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

@Suppress("FunctionName")
private interface DwmApi : Library {
    fun DwmSetWindowAttribute(hwnd: WinDef.HWND, attribute: Int, value: IntByReference, size: Int): Int
}

private const val DWMWA_WINDOW_CORNER_PREFERENCE = 33
private const val DWMWCP_ROUND = 2

private val isWindows = System.getProperty("os.name").orEmpty().startsWith("Windows")

/**
 * Asks Windows to round [frame]'s corners exactly as it rounds every other window. The shell picks the radius, so
 * it follows the system and turns square on its own when maximized or snapped. Windows 10 has no rounding and keeps
 * square corners, matching its other windows.
 */
private fun roundCorners(frame: Frame) {
    if (!isWindows || !frame.isDisplayable) return
    runCatching {
        val dwm = Native.load("dwmapi", DwmApi::class.java)
        dwm.DwmSetWindowAttribute(WinDef.HWND(Native.getWindowPointer(frame)), DWMWA_WINDOW_CORNER_PREFERENCE, IntByReference(DWMWCP_ROUND), 4)
    }
}

/** The screen's area minus the taskbar and docked bars, for the screen [frame] is on. */
private fun workArea(frame: Frame): Rectangle {
    val config = frame.graphicsConfiguration
    val screen = config.bounds
    val insets = Toolkit.getDefaultToolkit().getScreenInsets(config)
    return Rectangle(screen.x + insets.left, screen.y + insets.top, screen.width - insets.left - insets.right, screen.height - insets.top - insets.bottom)
}

/**
 * Makes an undecorated [frame] behave like a system window: shell-rounded corners, and maximizing that fills the
 * work area instead of covering the taskbar. Follows the frame across screens.
 */
internal fun followSystemShell(frame: Frame) {
    fun sync() {
        frame.maximizedBounds = workArea(frame)
        roundCorners(frame)
    }
    frame.addWindowListener(object : WindowAdapter() {
        override fun windowOpened(e: WindowEvent) = sync()
    })
    frame.addComponentListener(object : ComponentAdapter() {
        override fun componentMoved(e: ComponentEvent) {
            if (frame.extendedState and Frame.MAXIMIZED_BOTH == 0) frame.maximizedBounds = workArea(frame)
        }
    })
    if (frame.isDisplayable) sync()
}
