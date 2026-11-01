package dev.chieppa.stretch.gallery.gpu

import androidx.compose.ui.graphics.Color
import dev.chieppa.stretch.frame.buildOverlay
import dev.chieppa.stretch.gpu.CameraView
import dev.chieppa.stretch.gpu.GpuContext
import dev.chieppa.stretch.gpu.GpuFrame
import dev.chieppa.stretch.gpu.GpuScene
import dev.chieppa.stretch.gpu.Mat4
import dev.chieppa.stretch.gpu.Vec3
import dev.chieppa.stretch.gpu.webgpu.BufferBinding
import dev.chieppa.stretch.gpu.webgpu.BufferUsage
import dev.chieppa.stretch.gpu.webgpu.ColorTarget
import dev.chieppa.stretch.gpu.webgpu.CullMode
import dev.chieppa.stretch.gpu.webgpu.DepthState
import dev.chieppa.stretch.gpu.webgpu.IndexFormat
import dev.chieppa.stretch.gpu.webgpu.RenderPipelineSpec
import dev.chieppa.stretch.gpu.webgpu.VertexAttribute
import dev.chieppa.stretch.gpu.webgpu.VertexBufferLayout
import dev.chieppa.stretch.gpu.webgpu.VertexFormat

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
    private val shader = device.createShaderModule(CUBE_WGSL, "cube")
    private val vertices = device.createBuffer(CUBE_VERTICES.size * 4L, BufferUsage.Vertex + BufferUsage.CopyDst, "cube vertices")
    private val indices = device.createBuffer(CUBE_INDICES.size * 4L, BufferUsage.Index + BufferUsage.CopyDst, "cube indices")
    private val uniforms = device.createBuffer(FRAME_FLOATS * 4L, BufferUsage.Uniform + BufferUsage.CopyDst, "cube frame")
    private val pipeline = device.createRenderPipeline(
        RenderPipelineSpec(
            shader = shader,
            vertexEntry = "vs_main",
            buffers = listOf(
                VertexBufferLayout(24, listOf(VertexAttribute(VertexFormat.Float32x3, 0, 0), VertexAttribute(VertexFormat.Float32x3, 12, 1))),
            ),
            fragmentEntry = "fs_main",
            targets = listOf(ColorTarget(context.format)),
            cullMode = CullMode.Back,
            depth = DepthState(context.depthFormat),
            sampleCount = context.sampleCount,
            label = "cube",
        ),
    )
    private val bindGroup = pipeline.bindGroupLayout(0).use { device.createBindGroup(it, listOf(BufferBinding(0, uniforms))) }
    private val frameData = FloatArray(FRAME_FLOATS)
    private val start = System.nanoTime()
    private var angle = 0f
    private var lastFrame = start

    init {
        device.queue.writeBuffer(vertices, CUBE_VERTICES)
        device.queue.writeBuffer(indices, CUBE_INDICES)
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
        device.queue.writeBuffer(uniforms, frameData)

        val pass = frame.beginPass(state.background)
        pass.setPipeline(pipeline)
        pass.setBindGroup(0, bindGroup)
        pass.setVertexBuffer(0, vertices)
        pass.setIndexBuffer(indices, IndexFormat.Uint32)
        pass.drawIndexed(CUBE_INDICES.size)
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
        bindGroup.close()
        vertices.close()
        indices.close()
        uniforms.close()
        pipeline.close()
        shader.close()
    }
}
