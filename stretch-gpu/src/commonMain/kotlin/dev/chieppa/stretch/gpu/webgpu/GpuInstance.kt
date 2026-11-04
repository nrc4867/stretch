package dev.chieppa.stretch.gpu.webgpu

import dev.chieppa.stretch.frame.SurfaceHandle

/** A surface frames are presented to. */
internal expect class GpuSurface : AutoCloseable {
    fun configure(device: GpuDevice, format: TextureFormat, width: Int, height: Int, presentMode: PresentMode)

    /**
     * The texture to draw the next frame into, or null when the surface has none for now and should be configured
     * again. Throws on a lasting error.
     */
    fun acquire(): GpuTexture?

    /** Shows the acquired texture; false when the surface could not, and should be configured again. */
    fun present(): Boolean
}

/** A WebGPU instance with the adapter and device it picked, and the surface it presents to when it has one. */
internal expect class GpuInstance : AutoCloseable {
    val surface: GpuSurface?
    val device: GpuDevice

    /** The adapter's name and the API underneath, for an about box or a bug report. */
    val description: String

    /** Set once the device is lost; nothing drawn with it shows after that. */
    val lost: String?

    /** The formats the surface can show, or empty without a surface. */
    fun surfaceFormats(): List<TextureFormat>

    companion object {
        /**
         * Sets up WebGPU, presenting to [window] when there is one. [onError] gets validation and out-of-memory errors
         * and the device's loss. [backend] is honoured where the platform lets an app choose.
         */
        suspend fun create(window: SurfaceHandle?, backend: GpuBackend?, power: PowerPreference, onError: (String) -> Unit): GpuInstance
    }
}
