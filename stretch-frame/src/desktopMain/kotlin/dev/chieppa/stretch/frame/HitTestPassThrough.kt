package dev.chieppa.stretch.frame

import com.sun.jna.CallbackReference
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import dev.chieppa.stretch.frame.PointerUser32.Companion.GWLP_WNDPROC
import dev.chieppa.stretch.frame.PointerUser32.Companion.HTTRANSPARENT
import dev.chieppa.stretch.frame.PointerUser32.Companion.WM_NCHITTEST
import java.util.Collections

/**
 * Makes a native child window transparent to the mouse, pens and touch: Windows sends what lands on it to the window
 * under it instead, which for a native surface is the Compose window's own canvas. Its pointer handling (a viewport's
 * camera, the app's tools, pen input) then works over the surface as anywhere else.
 */
internal class HitTestPassThrough private constructor(private val hwnd: WinDef.HWND) : AutoCloseable {
    private val api = PointerUser32.instance
    private var original: Pointer? = null
    private val procedure = WinUser.WindowProc { target, message, wParam, lParam ->
        if (message == WM_NCHITTEST) {
            WinDef.LRESULT(HTTRANSPARENT.toLong())
        } else {
            api.CallWindowProcW(original, target, message, wParam, lParam)
        }
    }

    private fun attach() {
        original = api.SetWindowLongPtrW(hwnd, GWLP_WNDPROC, procedure)
    }

    /**
     * Puts the original window procedure back if this one is still on top. If something else subclassed the window
     * since, it keeps calling this procedure until the window is gone, so the procedure is kept alive instead.
     */
    override fun close() {
        val current = api.GetWindowLongPtrW(hwnd, GWLP_WNDPROC)
        if (current == CallbackReference.getFunctionPointer(procedure)) {
            api.SetWindowLongPtrW(hwnd, GWLP_WNDPROC, original)
        } else {
            retained += this
        }
    }

    companion object {
        private val retained: MutableSet<HitTestPassThrough> = Collections.synchronizedSet(HashSet())

        /** Starts passing input through [handle]'s window; null where this is not needed or not possible. */
        fun install(handle: SurfaceHandle): HitTestPassThrough? = when {
            !isWindows || handle !is SurfaceHandle.Win32 -> null
            else -> HitTestPassThrough(WinDef.HWND(Pointer(handle.hwnd))).also { it.attach() }
        }
    }
}
