package dev.chieppa.stretch.frame

actual sealed interface SurfaceHandle {
    /** A Win32 child window, [hwnd], and the module instance that owns it, [hinstance]. */
    data class Win32(val hwnd: Long, val hinstance: Long) : SurfaceHandle
}
