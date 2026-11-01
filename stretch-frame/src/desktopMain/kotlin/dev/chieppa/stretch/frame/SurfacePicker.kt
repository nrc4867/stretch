package dev.chieppa.stretch.frame

/**
 * A [SurfaceRenderer] that can say what is under a pixel. [pick] comes on the render thread, between frames and after
 * at least one, so it sees the scene as last drawn; [x] and [y] are in the surface's pixels from its top-left corner,
 * always inside it. Null means nothing is there.
 */
interface SurfacePicker<R> {
    fun pick(x: Int, y: Int): R?
}
