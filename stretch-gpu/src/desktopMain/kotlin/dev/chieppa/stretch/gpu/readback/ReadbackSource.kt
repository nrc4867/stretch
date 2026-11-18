package dev.chieppa.stretch.gpu.readback

import dev.chieppa.stretch.frame.SurfaceSize
import dev.chieppa.stretch.gpu.GpuContext
import dev.chieppa.stretch.gpu.GpuOptions
import dev.chieppa.stretch.gpu.GpuScene

/**
 * A scene rendered off screen and read back into images for Compose to draw (see [ReadbackSurface]). It renders on a
 * thread of its own; [publish] and [resize] may be called from any thread.
 */
interface ReadbackSource<S> : AutoCloseable {
    val frames: FrameHandoff
    val stats: ReadbackStats

    /** The GPU and API in use once set up. Compose state. */
    val description: String?

    fun publish(state: S)

    fun resize(size: SurfaceSize)
}

/**
 * How frames come back. [Naive] does what [dev.chieppa.stretch.gpu.GpuSnapshots] does, every frame: render, wait for
 * the GPU, swizzle RGBA into ARGB ints, and copy those into an image. [Pipelined] renders in BGRA into a ring of
 * buffers mapped without waiting, and copies each mapped buffer once, straight into the image's pixels.
 */
enum class ReadbackMode { Naive, Pipelined }

/** Frames a pipelined source keeps ahead of the UI. */
private const val PIPELINED_DEPTH = 2

/** Starts rendering [scene] from [initial] the way [mode] says. [onFailure] runs on the render thread. */
fun <S> readbackSource(
    mode: ReadbackMode,
    initial: S,
    options: GpuOptions = GpuOptions(),
    onFailure: (Throwable) -> Unit = { it.printStackTrace() },
    scene: (GpuContext) -> GpuScene<S>,
): ReadbackSource<S> = when (mode) {
    ReadbackMode.Naive -> NaiveReadbackLoop(initial, options, onFailure, scene)
    ReadbackMode.Pipelined -> PipelinedReadbackLoop(initial, PIPELINED_DEPTH, options, onFailure, scene)
}.start()

