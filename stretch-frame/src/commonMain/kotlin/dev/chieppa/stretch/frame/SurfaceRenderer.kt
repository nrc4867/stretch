package dev.chieppa.stretch.frame

/** What a [SurfaceRenderer] draws into, which each platform names in its own way. */
expect sealed interface SurfaceHandle

/** A native surface's size: [width] by [height] physical pixels, at [scale] pixels per dp. */
data class SurfaceSize(val width: Int, val height: Int, val scale: Float) {
    val isEmpty: Boolean get() = width <= 0 || height <= 0

    companion object {
        val Zero = SurfaceSize(0, 0, 1f)
    }
}

/**
 * Draws into a [NativeSurface] with any GPU API. Calls come one at a time, on the surface's render thread where the
 * platform has threads: [attach] once the surface exists, [render] whenever the state or the size changed, and [detach]
 * before the surface goes away. A renderer that returns true from [render] is called again for the next frame even when
 * nothing changed. On the desktop it should wait for the display there (as a presenting swap chain does) instead of
 * returning at once; in a browser the loop waits for the display itself.
 */
interface SurfaceRenderer<S> {
    suspend fun attach(handle: SurfaceHandle)

    /** Draws [state] at [size], never empty; returns true to draw again on the next frame. */
    fun render(state: S, size: SurfaceSize): Boolean

    fun detach()
}
