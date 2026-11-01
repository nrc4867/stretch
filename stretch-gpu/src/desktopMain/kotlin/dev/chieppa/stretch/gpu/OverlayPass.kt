package dev.chieppa.stretch.gpu

import dev.chieppa.stretch.frame.ViewportOverlay
import io.ygdrasil.webgpu.BindGroupDescriptor
import io.ygdrasil.webgpu.BindGroupEntry
import io.ygdrasil.webgpu.BlendComponent
import io.ygdrasil.webgpu.BlendState
import io.ygdrasil.webgpu.BufferBinding
import io.ygdrasil.webgpu.BufferDescriptor
import io.ygdrasil.webgpu.ColorTargetState
import io.ygdrasil.webgpu.DepthStencilState
import io.ygdrasil.webgpu.FragmentState
import io.ygdrasil.webgpu.GPUBlendFactor
import io.ygdrasil.webgpu.GPUBuffer
import io.ygdrasil.webgpu.GPUBufferUsage
import io.ygdrasil.webgpu.GPUCompareFunction
import io.ygdrasil.webgpu.GPURenderPassEncoder
import io.ygdrasil.webgpu.GPUVertexFormat
import io.ygdrasil.webgpu.MultisampleState
import io.ygdrasil.webgpu.RenderPipelineDescriptor
import io.ygdrasil.webgpu.ShaderModuleDescriptor
import io.ygdrasil.webgpu.VertexAttribute
import io.ygdrasil.webgpu.VertexBufferLayout
import io.ygdrasil.webgpu.VertexState
import io.ygdrasil.webgpu.writeBuffer
import dev.chieppa.stretch.gpu.OverlayGeometry.FLOATS_PER_VERTEX

private const val BYTES_PER_FLOAT = 4
private const val VIEWPORT_FLOATS = 4
private const val INITIAL_VERTICES = 1024

/**
 * Draws a [ViewportOverlay] as alpha-blended triangles over the scene, in the frame's own pass, ignoring depth. Its
 * vertex buffer grows to the largest overlay drawn so far.
 */
class OverlayPass internal constructor(private val context: GpuContext) : AutoCloseable {
    private val device = context.device
    private val shader = device.createShaderModule(ShaderModuleDescriptor(code = OVERLAY_WGSL))
    private val viewport = buffer(VIEWPORT_FLOATS.toLong() * BYTES_PER_FLOAT, GPUBufferUsage.Uniform)
    private val viewportData = FloatArray(VIEWPORT_FLOATS)
    private val pipeline = device.createRenderPipeline(
        RenderPipelineDescriptor(
            vertex = VertexState(
                module = shader,
                entryPoint = "vs_main",
                buffers = listOf(
                    VertexBufferLayout(
                        arrayStride = (FLOATS_PER_VERTEX * BYTES_PER_FLOAT).toULong(),
                        attributes = listOf(
                            VertexAttribute(GPUVertexFormat.Float32x2, 0u, 0u),
                            VertexAttribute(GPUVertexFormat.Float32x4, (2 * BYTES_PER_FLOAT).toULong(), 1u),
                        ),
                    ),
                ),
            ),
            fragment = FragmentState(
                module = shader,
                entryPoint = "fs_main",
                targets = listOf(
                    ColorTargetState(
                        format = context.format,
                        blend = BlendState(
                            color = BlendComponent(srcFactor = GPUBlendFactor.SrcAlpha, dstFactor = GPUBlendFactor.OneMinusSrcAlpha),
                            alpha = BlendComponent(srcFactor = GPUBlendFactor.One, dstFactor = GPUBlendFactor.OneMinusSrcAlpha),
                        ),
                    ),
                ),
            ),
            depthStencil = DepthStencilState(format = context.depthFormat, depthWriteEnabled = false, depthCompare = GPUCompareFunction.Always),
            multisample = MultisampleState(count = context.sampleCount.toUInt()),
        ),
    )
    private val bindGroup = device.createBindGroup(
        BindGroupDescriptor(
            layout = pipeline.getBindGroupLayout(0u),
            entries = listOf(BindGroupEntry(binding = 0u, resource = BufferBinding(buffer = viewport))),
        ),
    )
    private var vertices: GPUBuffer = vertexBuffer(INITIAL_VERTICES)
    private var capacity = INITIAL_VERTICES

    internal fun draw(pass: GPURenderPassEncoder, overlay: ViewportOverlay, width: Int, height: Int) {
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
        device.queue.writeBuffer(viewport, 0u, viewportData)
        device.queue.writeBuffer(vertices, 0u, triangles)
        pass.setPipeline(pipeline)
        pass.setBindGroup(0u, bindGroup)
        pass.setVertexBuffer(0u, vertices)
        pass.draw(count.toUInt())
    }

    private fun vertexBuffer(vertexCount: Int) = buffer(vertexCount.toLong() * FLOATS_PER_VERTEX * BYTES_PER_FLOAT, GPUBufferUsage.Vertex)

    private fun buffer(bytes: Long, usage: GPUBufferUsage): GPUBuffer =
        device.createBuffer(BufferDescriptor(size = bytes.toULong(), usage = setOf(usage, GPUBufferUsage.CopyDst)))

    override fun close() {
        vertices.close()
        viewport.close()
        pipeline.close()
        shader.close()
    }
}
