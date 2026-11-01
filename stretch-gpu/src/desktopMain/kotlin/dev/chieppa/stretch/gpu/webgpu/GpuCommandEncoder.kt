package dev.chieppa.stretch.gpu.webgpu

import dev.chieppa.stretch.gpu.ffi.WGPUColor
import dev.chieppa.stretch.gpu.ffi.WGPUExtent3D
import dev.chieppa.stretch.gpu.ffi.WGPUOrigin3D
import dev.chieppa.stretch.gpu.ffi.WGPURenderPassColorAttachment
import dev.chieppa.stretch.gpu.ffi.WGPURenderPassDepthStencilAttachment
import dev.chieppa.stretch.gpu.ffi.WGPURenderPassDescriptor
import dev.chieppa.stretch.gpu.ffi.WGPUTexelCopyBufferInfo
import dev.chieppa.stretch.gpu.ffi.WGPUTexelCopyBufferLayout
import dev.chieppa.stretch.gpu.ffi.WGPUTexelCopyTextureInfo
import dev.chieppa.stretch.gpu.ffi.wgpu_h
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment

/** Records commands for one submission. [finish] turns them into a [GpuCommandBuffer]; the encoder is spent then. */
class GpuCommandEncoder internal constructor(handle: MemorySegment) : GpuHandle(handle, wgpu_h::wgpuCommandEncoderRelease) {
    fun beginRenderPass(colors: List<ColorAttachment>, depth: DepthAttachment? = null, label: String? = null): GpuRenderPass =
        Arena.ofConfined().use { arena ->
            val pass = arena.allocate(WGPURenderPassDescriptor.layout())
            arena.setString(WGPURenderPassDescriptor.label(pass), label)
            WGPURenderPassDescriptor.colorAttachmentCount(pass, colors.size.toLong())
            WGPURenderPassDescriptor.colorAttachments(
                pass,
                arena.structs(colors.size, WGPURenderPassColorAttachment.layout()) { i, attachment ->
                    val color = colors[i]
                    WGPURenderPassColorAttachment.view(attachment, color.view.handle)
                    WGPURenderPassColorAttachment.depthSlice(attachment, DEPTH_SLICE_UNDEFINED)
                    WGPURenderPassColorAttachment.resolveTarget(attachment, color.resolveTarget?.handle ?: MemorySegment.NULL)
                    WGPURenderPassColorAttachment.loadOp(attachment, color.load.native)
                    WGPURenderPassColorAttachment.storeOp(attachment, color.store.native)
                    val clear = WGPURenderPassColorAttachment.clearValue(attachment)
                    WGPUColor.r(clear, color.clear.red.toDouble())
                    WGPUColor.g(clear, color.clear.green.toDouble())
                    WGPUColor.b(clear, color.clear.blue.toDouble())
                    WGPUColor.a(clear, color.clear.alpha.toDouble())
                },
            )
            if (depth != null) {
                val attachment = arena.allocate(WGPURenderPassDepthStencilAttachment.layout())
                WGPURenderPassDepthStencilAttachment.view(attachment, depth.view.handle)
                WGPURenderPassDepthStencilAttachment.depthLoadOp(attachment, depth.load.native)
                WGPURenderPassDepthStencilAttachment.depthStoreOp(attachment, depth.store.native)
                WGPURenderPassDepthStencilAttachment.depthClearValue(attachment, depth.clearDepth)
                WGPURenderPassDescriptor.depthStencilAttachment(pass, attachment)
            }
            GpuRenderPass(wgpu_h.wgpuCommandEncoderBeginRenderPass(handle, pass))
        }

    /**
     * Copies the whole of [texture] into [buffer], each row starting [bytesPerRow] bytes after the last; a multiple
     * of 256, see [dev.chieppa.stretch.gpu.paddedRowBytes].
     */
    fun copyTextureToBuffer(texture: GpuTexture, buffer: GpuBuffer, bytesPerRow: Int) = Arena.ofConfined().use { arena ->
        val source = arena.allocate(WGPUTexelCopyTextureInfo.layout())
        WGPUTexelCopyTextureInfo.texture(source, texture.handle)
        WGPUTexelCopyTextureInfo.aspect(source, wgpu_h.WGPUTextureAspect_All())
        val origin = WGPUTexelCopyTextureInfo.origin(source)
        WGPUOrigin3D.x(origin, 0)
        WGPUOrigin3D.y(origin, 0)
        WGPUOrigin3D.z(origin, 0)
        val destination = arena.allocate(WGPUTexelCopyBufferInfo.layout())
        WGPUTexelCopyBufferInfo.buffer(destination, buffer.handle)
        val layout = WGPUTexelCopyBufferInfo.layout(destination)
        WGPUTexelCopyBufferLayout.bytesPerRow(layout, bytesPerRow)
        WGPUTexelCopyBufferLayout.rowsPerImage(layout, texture.height)
        val size = arena.allocate(WGPUExtent3D.layout())
        WGPUExtent3D.width(size, texture.width)
        WGPUExtent3D.height(size, texture.height)
        WGPUExtent3D.depthOrArrayLayers(size, 1)
        wgpu_h.wgpuCommandEncoderCopyTextureToBuffer(handle, source, destination, size)
    }

    fun finish(): GpuCommandBuffer = GpuCommandBuffer(wgpu_h.wgpuCommandEncoderFinish(handle, MemorySegment.NULL))
}
