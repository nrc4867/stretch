package dev.chieppa.stretch.ui

import com.sun.jna.CallbackReference
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import java.awt.Window
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.util.concurrent.ConcurrentHashMap

private const val WM_WINDOWPOSCHANGED = 0x0047
private const val FOLLOW_FLAGS = WinUser.SWP_NOSIZE or WinUser.SWP_NOZORDER or WinUser.SWP_NOACTIVATE or 0x0200 // SWP_NOOWNERZORDER

/**
 * Moves popup windows with the window they belong to in the same step Windows moves it, from that window's own window
 * procedure. Following its moves through AWT's events instead reaches the popups only once the UI thread gets to them, so
 * they trail behind a dragged window. Placing a popup from its layout still goes through the UI thread; this only carries
 * the window's moves over. Windows only; elsewhere it does nothing.
 */
internal object OwnerMoveFollow {
    private val hooks = HashMap<Window, Hook>()
    private val isWindows = System.getProperty("os.name").startsWith("Windows")

    /** Starts moving [popup] along with [owner]. Called on the UI thread. */
    fun start(owner: Window, popup: Window) {
        if (!isWindows) return
        val hwnd = hwndOf(popup) ?: return
        val hook = hooks[owner] ?: Hook.install(owner)?.also { installed ->
            hooks[owner] = installed
            owner.addWindowListener(object : WindowAdapter() {
                override fun windowClosed(e: WindowEvent) {
                    owner.removeWindowListener(this)
                    hooks.remove(owner)?.close()
                }
            })
        } ?: return
        hook.followers[popup] = hwnd
    }

    /** Stops moving [popup] with whichever window it followed. Called on the UI thread. */
    fun stop(popup: Window) {
        hooks.values.forEach { it.followers.remove(popup) }
    }

    private fun hwndOf(window: Window): WinDef.HWND? =
        window.takeIf { it.isDisplayable }?.let(Native::getComponentID)?.takeIf { it != 0L }?.let { WinDef.HWND(Pointer(it)) }

    private class Hook(private val hwnd: WinDef.HWND) : AutoCloseable {
        val followers = ConcurrentHashMap<Window, WinDef.HWND>()
        private val user32 = User32.INSTANCE
        private var original: Pointer? = null
        private var last = rectOf(hwnd)
        private val procedure = WinUser.WindowProc { target, message, wParam, lParam ->
            val result = user32.CallWindowProc(original, target, message, wParam, lParam)
            if (message == WM_WINDOWPOSCHANGED) carry()
            result
        }

        private fun carry() {
            val now = rectOf(hwnd)
            val dx = now.left - last.left
            val dy = now.top - last.top
            last = now
            if (dx == 0 && dy == 0) return
            for (follower in followers.values) {
                val at = rectOf(follower)
                user32.SetWindowPos(follower, null, at.left + dx, at.top + dy, 0, 0, FOLLOW_FLAGS)
            }
        }

        /** Takes the original procedure before swapping, since messages can reach this one the moment it is set. */
        private fun attach() {
            original = user32.GetWindowLongPtr(hwnd, WinUser.GWL_WNDPROC).toLong().takeIf { it != 0L }?.let(::Pointer)
            user32.SetWindowLongPtr(hwnd, WinUser.GWL_WNDPROC, CallbackReference.getFunctionPointer(procedure))
        }

        /**
         * Puts the original window procedure back if this one is still on top. If something else subclassed the window
         * since, it keeps calling this procedure until the window is gone, so the procedure is kept alive instead.
         */
        override fun close() {
            followers.clear()
            val current = user32.GetWindowLongPtr(hwnd, WinUser.GWL_WNDPROC)
            if (current.toLong() == Pointer.nativeValue(CallbackReference.getFunctionPointer(procedure))) {
                user32.SetWindowLongPtr(hwnd, WinUser.GWL_WNDPROC, original)
            } else {
                retained += this
            }
        }

        companion object {
            private val retained: MutableSet<Hook> = ConcurrentHashMap.newKeySet()

            fun install(owner: Window): Hook? = hwndOf(owner)?.let { Hook(it).also(Hook::attach) }

            private fun rectOf(window: WinDef.HWND) = WinDef.RECT().also { User32.INSTANCE.GetWindowRect(window, it) }
        }
    }
}
