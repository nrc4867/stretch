package dev.chieppa.stretch.gpu

/**
 * What an app draws with WebGPU. A scene is made on the render thread with the frame's [GpuContext], keeps its own
 * pipelines and buffers, and draws one state per call to [render]: begin the pass with [GpuFrame.beginPass], draw,
 * draw the overlay last if there is one, and end the pass. [close] releases what the scene made.
 */
interface GpuScene<S> : AutoCloseable {
    /** Records the frame's commands for [state]; returns true to draw again next frame, as an animation does. */
    fun render(frame: GpuFrame, state: S): Boolean

    override fun close() = Unit
}
