package dev.chieppa.stretch.gallery.gpu

import androidx.compose.ui.graphics.Color
import dev.chieppa.stretch.frame.buildOverlay
import dev.chieppa.stretch.gpu.CameraView
import dev.chieppa.stretch.gpu.GpuContext
import dev.chieppa.stretch.gpu.GpuFrame
import dev.chieppa.stretch.gpu.GpuScene
import dev.chieppa.stretch.gpu.Mat4
import dev.chieppa.stretch.gpu.Vec3
import io.ygdrasil.webgpu.BindGroupDescriptor
import io.ygdrasil.webgpu.BindGroupEntry
import io.ygdrasil.webgpu.BufferBinding
import io.ygdrasil.webgpu.BufferDescriptor
import io.ygdrasil.webgpu.ColorTargetState
import io.ygdrasil.webgpu.DepthStencilState
import io.ygdrasil.webgpu.FragmentState
import io.ygdrasil.webgpu.GPUBufferUsage
import io.ygdrasil.webgpu.GPUCompareFunction
import io.ygdrasil.webgpu.GPUCullMode
import io.ygdrasil.webgpu.GPUIndexFormat
import io.ygdrasil.webgpu.GPUVertexFormat
import io.ygdrasil.webgpu.MultisampleState
import io.ygdrasil.webgpu.PrimitiveState
import io.ygdrasil.webgpu.RenderPipelineDescriptor
import io.ygdrasil.webgpu.ShaderModuleDescriptor
import io.ygdrasil.webgpu.VertexAttribute
import io.ygdrasil.webgpu.VertexBufferLayout
import io.ygdrasil.webgpu.VertexState
import io.ygdrasil.webgpu.writeBuffer

/** What the demo draws: the camera, flat or two-tone shading, whether the cube spins, and the colors to use. */
data class CubeState(
    val camera: CameraView,
    val twoTone: Boolean,
    val spin: Boolean,
    val background: Color,
    val base: Color,
    val shade: Color,
)

/** Radians per second the cube turns while spinning. */
private const val SPIN_SPEED = 0.8f
private const val FRAME_FLOATS = 48
private const val AXIS_LENGTH = 1.6f

/** A cube with two-tone shading, and the scene's axes drawn as an overlay. Spins while [CubeState.spin] is on. */
class CubeScene(private val context: GpuContext) : GpuScene<CubeState> {
    private val device = context.device
    private val shader = device.createShaderModule(ShaderModuleDescriptor(code = CUBE_WGSL))
    private val vertices = device.createBuffer(BufferDescriptor(size = (CUBE_VERTICES.size * 4).toULong(), usage = setOf(GPUBufferUsage.Vertex, GPUBufferUsage.CopyDst)))
    private val indices = device.createBuffer(BufferDescriptor(size = (CUBE_INDICES.size * 4).toULong(), usage = setOf(GPUBufferUsage.Index, GPUBufferUsage.CopyDst)))
    private val uniforms = device.createBuffer(BufferDescriptor(size = (FRAME_FLOATS * 4).toULong(), usage = setOf(GPUBufferUsage.Uniform, GPUBufferUsage.CopyDst)))
    private val pipeline = device.createRenderPipeline(
        RenderPipelineDescriptor(
            vertex = VertexState(
                module = shader,
                entryPoint = "vs_main",
                buffers = listOf(
                    VertexBufferLayout(
                        arrayStride = 24u,
                        attributes = listOf(VertexAttribute(GPUVertexFormat.Float32x3, 0u, 0u), VertexAttribute(GPUVertexFormat.Float32x3, 12u, 1u)),
                    ),
                ),
            ),
            fragment = FragmentState(module = shader, entryPoint = "fs_main", targets = listOf(ColorTargetState(format = context.format))),
            primitive = PrimitiveState(cullMode = GPUCullMode.Back),
            depthStencil = DepthStencilState(format = context.depthFormat, depthWriteEnabled = true, depthCompare = GPUCompareFunction.Less),
            multisample = MultisampleState(count = context.sampleCount.toUInt()),
        ),
    )
    private val bindGroup = device.createBindGroup(
        BindGroupDescriptor(layout = pipeline.getBindGroupLayout(0u), entries = listOf(BindGroupEntry(binding = 0u, resource = BufferBinding(buffer = uniforms)))),
    )
    private val frameData = FloatArray(FRAME_FLOATS)
    private val start = System.nanoTime()
    private var angle = 0f
    private var lastFrame = start

    init {
        device.queue.writeBuffer(vertices, 0u, CUBE_VERTICES)
        device.queue.writeBuffer(indices, 0u, CUBE_INDICES)
    }

    override fun render(frame: GpuFrame, state: CubeState): Boolean {
        val now = System.nanoTime()
        if (state.spin) angle += (now - lastFrame) / 1e9f * SPIN_SPEED
        lastFrame = now

        state.camera.viewProjection(frame.width, frame.height).copyInto(frameData, 0)
        Mat4.rotation(Vec3.Up, angle).copyInto(frameData, 16)
        frameData.put(32, -0.4f, 0.8f, 0.5f, 0f)
        frameData.put(36, state.base)
        frameData.put(40, state.shade)
        frameData[44] = if (state.twoTone) 1f else 0f
        device.queue.writeBuffer(uniforms, 0u, frameData)

        val pass = frame.beginPass(state.background)
        pass.setPipeline(pipeline)
        pass.setBindGroup(0u, bindGroup)
        pass.setVertexBuffer(0u, vertices)
        pass.setIndexBuffer(indices, GPUIndexFormat.Uint32)
        pass.drawIndexed(CUBE_INDICES.size.toUInt())
        frame.drawOverlay(pass, axes(state.camera, frame.width, frame.height))
        pass.end()
        return state.spin
    }

    private fun axes(camera: CameraView, width: Int, height: Int) = buildOverlay {
        val origin = camera.project(Vec3.Zero, width, height) ?: return@buildOverlay
        listOf(
            Vec3(AXIS_LENGTH, 0f, 0f) to Color(0xFFE5484D),
            Vec3(0f, AXIS_LENGTH, 0f) to Color(0xFF46A758),
            Vec3(0f, 0f, AXIS_LENGTH) to Color(0xFF3E63DD),
        ).forEach { (end, color) ->
            camera.project(end, width, height)?.let { line(origin, it, color, width = 2f) }
        }
        dot(origin, 4f, Color.White)
    }

    private fun FloatArray.put(offset: Int, a: Float, b: Float, c: Float, d: Float) {
        this[offset] = a
        this[offset + 1] = b
        this[offset + 2] = c
        this[offset + 3] = d
    }

    private fun FloatArray.put(offset: Int, color: Color) = put(offset, color.red, color.green, color.blue, color.alpha)

    override fun close() {
        vertices.close()
        indices.close()
        uniforms.close()
        pipeline.close()
        shader.close()
    }
}
