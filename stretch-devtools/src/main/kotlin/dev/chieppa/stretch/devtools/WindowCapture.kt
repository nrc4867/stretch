package dev.chieppa.stretch.devtools

import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.platform.win32.GDI32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinGDI
import java.awt.Rectangle
import java.awt.Window
import java.awt.image.BufferedImage

@Suppress("FunctionName")
private interface Dwm : Library {
    fun DwmGetWindowAttribute(hwnd: WinDef.HWND, attribute: Int, value: WinDef.RECT, size: Int): Int
}

private const val PW_RENDERFULLCONTENT = 2
private const val DWMWA_EXTENDED_FRAME_BOUNDS = 9

private val dwm by lazy { Native.load("dwmapi", Dwm::class.java) }

internal val captureSupported = System.getProperty("os.name").orEmpty().startsWith("Windows")

/**
 * Draws [window] as the OS composes it, frame and all, at full device resolution: only this window's own pixels,
 * whatever covers it on screen. Its showing popups and other owned windows are drawn over it where they sit. Windows
 * only; the window must not be minimized.
 */
internal fun capture(window: Window): BufferedImage {
    val bounds = visibleBounds(window)
    val image = BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_RGB)
    val g = image.createGraphics()
    g.drawImage(printWindow(window), 0, 0, null)
    window.ownedWindows.filter { it.isShowing && it.width > 0 && it.height > 0 }.forEach { owned ->
        val at = visibleBounds(owned)
        g.drawImage(printWindow(owned), at.x - bounds.x, at.y - bounds.y, null)
    }
    g.dispose()
    return image
}

private fun hwnd(window: Window) = WinDef.HWND(Native.getWindowPointer(window))

/** The window's bounds on screen in device pixels, without the invisible resize border Windows adds around it. */
private fun visibleBounds(window: Window): Rectangle {
    val rect = WinDef.RECT()
    if (dwm.DwmGetWindowAttribute(hwnd(window), DWMWA_EXTENDED_FRAME_BOUNDS, rect, rect.size()) != 0) {
        User32.INSTANCE.GetWindowRect(hwnd(window), rect)
    }
    return rect.toRectangle()
}

/** The window's visible pixels, from PrintWindow, which DWM answers even for GPU-drawn and covered windows. */
private fun printWindow(window: Window): BufferedImage {
    val hwnd = hwnd(window)
    val outer = WinDef.RECT().also { User32.INSTANCE.GetWindowRect(hwnd, it) }.toRectangle()
    val visible = visibleBounds(window)
    val screenDc = User32.INSTANCE.GetDC(null)
    val memDc = GDI32.INSTANCE.CreateCompatibleDC(screenDc)
    val bitmap = GDI32.INSTANCE.CreateCompatibleBitmap(screenDc, outer.width, outer.height)
    val previous = GDI32.INSTANCE.SelectObject(memDc, bitmap)
    try {
        User32.INSTANCE.PrintWindow(hwnd, memDc, PW_RENDERFULLCONTENT)
        val full = readBitmap(memDc, bitmap, outer.width, outer.height)
        return full.getSubimage(visible.x - outer.x, visible.y - outer.y, visible.width, visible.height)
    } finally {
        GDI32.INSTANCE.SelectObject(memDc, previous)
        GDI32.INSTANCE.DeleteObject(bitmap)
        GDI32.INSTANCE.DeleteDC(memDc)
        User32.INSTANCE.ReleaseDC(null, screenDc)
    }
}

private fun readBitmap(dc: WinDef.HDC, bitmap: WinDef.HBITMAP, width: Int, height: Int): BufferedImage {
    val info = WinGDI.BITMAPINFO()
    info.bmiHeader.biWidth = width
    info.bmiHeader.biHeight = -height
    info.bmiHeader.biPlanes = 1
    info.bmiHeader.biBitCount = 32
    info.bmiHeader.biCompression = WinGDI.BI_RGB
    val pixels = Memory(width.toLong() * height * 4)
    GDI32.INSTANCE.GetDIBits(dc, bitmap, 0, height, pixels, info, WinGDI.DIB_RGB_COLORS)
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    image.setRGB(0, 0, width, height, pixels.getIntArray(0, width * height), 0, width)
    return image
}

private fun WinDef.RECT.toRectangle() = Rectangle(left, top, right - left, bottom - top)
