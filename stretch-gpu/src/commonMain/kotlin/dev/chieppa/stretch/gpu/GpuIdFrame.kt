package dev.chieppa.stretch.gpu

import dev.chieppa.stretch.frame.SurfaceSize
import dev.chieppa.stretch.gpu.webgpu.ColorAttachment
import dev.chieppa.stretch.gpu.webgpu.DepthAttachment
import dev.chieppa.stretch.gpu.webgpu.GpuCommandEncoder
import dev.chieppa.stretch.gpu.webgpu.GpuRenderPass
import dev.chieppa.stretch.gpu.webgpu.GpuTextureView
import dev.chieppa.stretch.gpu.webgpu.StoreOp

/**
 * One ID pass for picking the pixel at [x], [y] of a surface of [size]. [beginPass] sets up a one-pixel target of
 * object IDs and its depth; the scene draws as for the whole surface, with [viewProjection] (or [pickMatrix] in front
 * of its own) so that only what covers the picked pixel lands there. Pipelines are single-sampled, with
 * [GpuContext.idFormat] and [GpuContext.idDepthFormat]; see [GpuScene.renderIds].
 */
class GpuIdFrame internal constructor(
    val context: GpuContext,
    val encoder: GpuCommandEncoder,
    val size: SurfaceSize,
    val x: Int,
    val y: Int,
    private val ids: GpuTextureView,
    private val depth: GpuTextureView,
) {
    val width: Int get() = size.width
    val height: Int get() = size.height

    /** Blows the picked pixel up to fill the target; goes in front of a view-projection made for [width] by [height]. */
    val pickMatrix: Mat4 = pickMatrix(x, y, width, height)

    /** [camera]'s view-projection for this pass: the whole surface's, narrowed to the picked pixel. */
    fun viewProjection(camera: CameraView): Mat4 = pickMatrix * camera.viewProjection(width, height)

    /** Starts the ID pass, cleared to [GpuPick.NOTHING] with a cleared depth buffer. */
    fun beginPass(): GpuRenderPass =
        encoder.beginRenderPass(listOf(ColorAttachment(ids)), DepthAttachment(depth, store = StoreOp.Store), label = "picking")
}
