package dev.chieppa.stretch.gpu

import dev.chieppa.stretch.gpu.webgpu.GpuDevice
import dev.chieppa.stretch.gpu.webgpu.GpuInstance

/**
 * A WebGPU [device] of its own, with no surface: for work whose answers come back to the CPU, such as compute passes
 * read back with [GpuDevice.readBuffer]. It is slow to open, so keep one for as long as the work goes on and [close]
 * it after. Use it from one thread at a time; on the desktop `readBuffer` blocks the calling thread until the GPU is
 * done. [description] names the GPU and the API underneath.
 */
class HeadlessGpu private constructor(private val gpu: GpuInstance) : AutoCloseable {
    val device: GpuDevice get() = gpu.device

    val description: String get() = gpu.description

    override fun close() = gpu.close()

    companion object {
        /** Sets up a device with [options]' backend, power preference and error callback. */
        suspend fun open(options: GpuOptions = GpuOptions()): HeadlessGpu =
            HeadlessGpu(GpuInstance.create(null, options.backend, options.powerPreference, options.onError))
    }
}
