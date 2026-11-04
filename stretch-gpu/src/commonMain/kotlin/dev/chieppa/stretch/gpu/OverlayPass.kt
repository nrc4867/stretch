package dev.chieppa.stretch.gpu

import dev.chieppa.stretch.frame.ViewportOverlay
import dev.chieppa.stretch.gpu.OverlayGeometry.FLOATS_PER_VERTEX
import dev.chieppa.stretch.gpu.webgpu.BlendState
import dev.chieppa.stretch.gpu.webgpu.BufferBinding
import dev.chieppa.stretch.gpu.webgpu.BufferUsage
import dev.chieppa.stretch.gpu.webgpu.ColorTarget
import dev.chieppa.stretch.gpu.webgpu.CompareFunction
import dev.chieppa.stretch.gpu.webgpu.DepthState
import dev.chieppa.stretch.gpu.webgpu.GpuBuffer
import dev.chieppa.stretch.gpu.webgpu.GpuRenderPass
import dev.chieppa.stretch.gpu.webgpu.RenderPipelineSpec
import dev.chieppa.stretch.gpu.webgpu.VertexAttribute
import dev.chieppa.stretch.gpu.webgpu.VertexBufferLayout
import dev.chieppa.stretch.gpu.webgpu.VertexFormat

private const val BYTES_PER_FLOAT = 4
private const val VIEWPORT_FLOATS = 4
private const val INITIAL_VERTICES = 1024

/**
 * Draws a [ViewportOverlay] as alpha-blended triangles over the scene, in the frame's own pass, ignoring depth. Its
 * vertex buffer grows to the largest overlay drawn so far.
 */
class OverlayPass internal constructor(context: GpuContext) : AutoCloseable {
    private val device = context.device
    private val shader = device.createShaderModule(OVERLAY_WGSL, "overlay")
    private val viewport = device.createBuffer(VIEWPORT_FLOATS.toLong() * BYTES_PER_FLOAT, BufferUsage.Uniform + BufferUsage.CopyDst, "overlay viewport")
    private val viewportData = FloatArray(VIEWPORT_FLOATS)
    private val pipeline = device.createRenderPipeline(
        RenderPipelineSpec(
            shader = shader,
            vertexEntry = "vs_main",
            buffers = listOf(
                VertexBufferLayout(
                    stride = (FLOATS_PER_VERTEX * BYTES_PER_FLOAT).toLong(),
                    attributes = listOf(
                        VertexAttribute(VertexFormat.Float32x2, 0, 0),
                        VertexAttribute(VertexFormat.Float32x4, (2 * BYTES_PER_FLOAT).toLong(), 1),
                    ),
                ),
            ),
            fragmentEntry = "fs_main",
            targets = listOf(ColorTarget(context.format, BlendState.AlphaBlending)),
            depth = DepthState(context.depthFormat, write = false, compare = CompareFunction.Always),
            sampleCount = context.sampleCount,
            label = "overlay",
        ),
    )
    private val bindGroup = pipeline.bindGroupLayout(0).use { device.createBindGroup(it, listOf(BufferBinding(0, viewport))) }
    private var vertices: GpuBuffer = vertexBuffer(INITIAL_VERTICES)
    private var capacity = INITIAL_VERTICES

    internal fun draw(pass: GpuRenderPass, overlay: ViewportOverlay, width: Int, height: Int) {
        if (overlay.isEmpty) return
        val triangles = OverlayGeometry.triangles(overlay)
        val count = triangles.size / FLOATS_PER_VERTEX
        if (count == 0) return
        if (count > capacity) {
            vertices.close()
            capacity = maxOf(count, capacity * 2)
            vertices = vertexBuffer(capacity)
        }
        viewportData[0] = width.toFloat()
        viewportData[1] = height.toFloat()
        device.queue.writeBuffer(viewport, viewportData)
        device.queue.writeBuffer(vertices, triangles)
        pass.setPipeline(pipeline)
        pass.setBindGroup(0, bindGroup)
        pass.setVertexBuffer(0, vertices)
        pass.draw(count)
    }

    private fun vertexBuffer(vertexCount: Int) =
        device.createBuffer(vertexCount.toLong() * FLOATS_PER_VERTEX * BYTES_PER_FLOAT, BufferUsage.Vertex + BufferUsage.CopyDst, "overlay vertices")

    override fun close() {
        bindGroup.close()
        vertices.close()
        viewport.close()
        pipeline.close()
        shader.close()
    }
}
