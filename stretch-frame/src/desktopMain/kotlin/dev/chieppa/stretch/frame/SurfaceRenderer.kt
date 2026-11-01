package dev.chieppa.stretch.frame

/** The native window a [SurfaceRenderer] draws into. */
sealed interface SurfaceHandle {
    /** A Win32 child window, [hwnd], and the module instance that owns it, [hinstance]. */
    data class Win32(val hwnd: Long, val hinstance: Long) : SurfaceHandle
}

/** A native surface's size: [width] by [height] physical pixels, at [scale] pixels per dp. */
data class SurfaceSize(val width: Int, val height: Int, val scale: Float) {
    val isEmpty: Boolean get() = width <= 0 || height <= 0

    companion object {
        val Zero = SurfaceSize(0, 0, 1f)
    }
}

/**
 * Draws into a [NativeSurface] with any GPU API. Every call comes on the surface's own render thread, one at a time:
 * [attach] once the native window exists, [render] whenever the state or the size changed, and [detach] before the
 * window goes away. A renderer that returns true from [render] is called again for the next frame even when nothing
 * changed, so it should wait for the display there (as a presenting swap chain does) instead of returning at once.
 */
interface SurfaceRenderer<S> {
    fun attach(handle: SurfaceHandle)

    /** Draws [state] at [size], never empty; returns true to draw again on the next frame. */
    fun render(state: S, size: SurfaceSize): Boolean

    fun detach()
}
