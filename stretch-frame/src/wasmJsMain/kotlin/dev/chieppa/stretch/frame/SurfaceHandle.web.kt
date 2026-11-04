package dev.chieppa.stretch.frame

import org.w3c.dom.HTMLCanvasElement

actual sealed interface SurfaceHandle {
    /** A canvas in the page, sized by the surface. */
    class Canvas(val element: HTMLCanvasElement) : SurfaceHandle
}
