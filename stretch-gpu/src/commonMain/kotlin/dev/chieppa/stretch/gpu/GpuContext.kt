package dev.chieppa.stretch.gpu

import dev.chieppa.stretch.gpu.webgpu.GpuDevice
import dev.chieppa.stretch.gpu.webgpu.TextureFormat

/**
 * What a [GpuScene] builds its pipelines against: the [device], the [format] and [sampleCount] of the frame's color
 * target, and the [depthFormat] of its depth buffer. A pipeline drawn in [GpuFrame.beginPass] must match all three.
 * [overlay] draws a [dev.chieppa.stretch.frame.ViewportOverlay] in the same pass. A scene that can be picked draws
 * object IDs in [GpuIdFrame.beginPass] with pipelines made for [idFormat] and [idDepthFormat], single-sampled. [description] names the GPU and the
 * API underneath, for an about box or a bug report.
 */
class GpuContext internal constructor(
    val device: GpuDevice,
    val format: TextureFormat,
    val sampleCount: Int,
    val description: String,
) : AutoCloseable {
    val depthFormat: TextureFormat = TextureFormat.Depth24Plus

    /** The format of the target picking draws object IDs into: one unsigned 32-bit integer per pixel. */
    val idFormat: TextureFormat = TextureFormat.R32Uint

    /** The depth format of picking's ID pass, whose depth is read back for the picked point's position. */
    val idDepthFormat: TextureFormat = TextureFormat.Depth32Float

    private var overlayPass: OverlayPass? = null

    /** The overlay pass, made the first time a scene asks for it. */
    val overlay: OverlayPass get() = overlayPass ?: OverlayPass(this).also { overlayPass = it }

    override fun close() {
        overlayPass?.close()
        overlayPass = null
    }
}
