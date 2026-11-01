package dev.chieppa.stretch.gpu.webgpu

import java.lang.foreign.MemorySegment

/** A native WebGPU object, released once on [close]. Closing it twice does nothing. */
abstract class GpuHandle internal constructor(internal val handle: MemorySegment, private val release: (MemorySegment) -> Unit) : AutoCloseable {
    private var released = false

    override fun close() {
        if (released) return
        released = true
        release(handle)
    }
}
