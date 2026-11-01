package dev.chieppa.stretch.gpu

/**
 * What an app draws with WebGPU. A scene is made on the render thread with the frame's [GpuContext], keeps its own
 * pipelines and buffers, and draws one state per call to [render]: begin the pass with [GpuFrame.beginPass], draw,
 * draw the overlay last if there is one, and end the pass. A scene that can be picked also draws IDs in [renderIds]. [close] releases what the scene made.
 */
interface GpuScene<S> : AutoCloseable {
    /** Records the frame's commands for [state]; returns true to draw again next frame, as an animation does. */
    fun render(frame: GpuFrame, state: S): Boolean

    /**
     * Draws [state] again for picking, each object's ID in place of its color: begin the pass with
     * [GpuIdFrame.beginPass], draw with ID pipelines, and end it. IDs are the scene's own, any value but
     * [GpuPick.NOTHING]; one can pack an object and a part of it. By default the scene draws nothing, so picks find
     * nothing.
     */
    fun renderIds(frame: GpuIdFrame, state: S) = frame.beginPass().end()

    override fun close() = Unit
}
