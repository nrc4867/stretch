package dev.chieppa.stretch.gpu

import io.ygdrasil.webgpu.GPUDevice
import io.ygdrasil.webgpu.GPUTextureFormat

/**
 * What a [GpuScene] builds its pipelines against: the [device], the [format] and [sampleCount] of the frame's color
 * target, and the [depthFormat] of its depth buffer. A pipeline drawn in [GpuFrame.beginPass] must match all three.
 * [overlay] draws a [dev.chieppa.stretch.frame.ViewportOverlay] in the same pass. [description] names the GPU and the
 * API underneath, for an about box or a bug report.
 */
class GpuContext internal constructor(
    val device: GPUDevice,
    val format: GPUTextureFormat,
    val sampleCount: Int,
    val description: String,
) : AutoCloseable {
    val depthFormat: GPUTextureFormat = GPUTextureFormat.Depth24Plus

    private var overlayPass: OverlayPass? = null

    /** The overlay pass, made the first time a scene asks for it. */
    val overlay: OverlayPass get() = overlayPass ?: OverlayPass(this).also { overlayPass = it }

    override fun close() {
        overlayPass?.close()
        overlayPass = null
    }
}
