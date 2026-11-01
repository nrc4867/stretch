package dev.chieppa.stretch.gallery.gpu

import androidx.compose.ui.graphics.Color
import dev.chieppa.stretch.frame.ViewportOverlay
import dev.chieppa.stretch.frame.buildOverlay
import dev.chieppa.stretch.gpu.CameraView
import dev.chieppa.stretch.gpu.GpuContext
import dev.chieppa.stretch.gpu.GpuFrame
import dev.chieppa.stretch.gpu.GpuIdFrame
import dev.chieppa.stretch.gpu.GpuScene
import dev.chieppa.stretch.gpu.Mat4
import dev.chieppa.stretch.gpu.Vec3
import dev.chieppa.stretch.gpu.webgpu.BufferBinding
import dev.chieppa.stretch.gpu.webgpu.BufferUsage
import dev.chieppa.stretch.gpu.webgpu.ColorTarget
import dev.chieppa.stretch.gpu.webgpu.CullMode
import dev.chieppa.stretch.gpu.webgpu.DepthState
import dev.chieppa.stretch.gpu.webgpu.GpuBindGroup
import dev.chieppa.stretch.gpu.webgpu.GpuRenderPass
import dev.chieppa.stretch.gpu.webgpu.IndexFormat
import dev.chieppa.stretch.gpu.webgpu.RenderPipelineSpec
import dev.chieppa.stretch.gpu.webgpu.VertexAttribute
import dev.chieppa.stretch.gpu.webgpu.VertexBufferLayout
import dev.chieppa.stretch.gpu.webgpu.VertexFormat

/**
 * What the demo draws: the camera, flat or two-tone shading, whether the cube spins, the colors to use, and the
 * [overlay] of guides, handles and marquee to draw over the scene.
 */
data class CubeState(
    val camera: CameraView,
    val twoTone: Boolean,
    val spin: Boolean,
    val background: Color,
    val base: Color,
    val shade: Color,
    val overlay: ViewportOverlay = ViewportOverlay.Empty,
)

/** How far the cube has turned, in radians: set by the render thread as it spins, read by the UI to pick it. */
class CubeSpin {
    @Volatile var angle: Float = 0f
        internal set
}

/** Radians per second the cube turns while spinning. */
private const val SPIN_SPEED = 0.8f
private const val FRAME_FLOATS = 48
private const val AXIS_LENGTH = 1.6f
private const val VERTEX_STRIDE = 24L
private const val NORMAL_OFFSET = 12L
private const val NANOS_PER_SECOND = 1e9f

/**
 * A cube with two-tone shading, and the scene's axes drawn as an overlay. Spins while [CubeState.spin] is on, keeping
 * [spin] up to date. Picking finds the cube's faces, with IDs from [CUBE_FIRST_ID].
 */
class CubeScene(private val context: GpuContext, private val spin: CubeSpin = CubeSpin()) : GpuScene<CubeState> {
    private val device = context.device
    private val shader = device.createShaderModule(CUBE_WGSL, "cube")
    private val vertices = device.createBuffer(CUBE_VERTICES.size * 4L, BufferUsage.Vertex + BufferUsage.CopyDst, "cube vertices")
    private val indices = device.createBuffer(CUBE_INDICES.size * 4L, BufferUsage.Index + BufferUsage.CopyDst, "cube indices")
    private val uniforms = device.createBuffer(FRAME_FLOATS * 4L, BufferUsage.Uniform + BufferUsage.CopyDst, "cube frame")
    private val vertexLayout = VertexBufferLayout(
        VERTEX_STRIDE,
        listOf(VertexAttribute(VertexFormat.Float32x3, 0, 0), VertexAttribute(VertexFormat.Float32x3, NORMAL_OFFSET, 1)),
    )
    private val pipeline = device.createRenderPipeline(
        RenderPipelineSpec(
            shader = shader,
            vertexEntry = "vs_main",
            buffers = listOf(vertexLayout),
            fragmentEntry = "fs_main",
            targets = listOf(ColorTarget(context.format)),
            cullMode = CullMode.Back,
            depth = DepthState(context.depthFormat),
            sampleCount = context.sampleCount,
            label = "cube",
        ),
    )
    private val idPipeline = device.createRenderPipeline(
        RenderPipelineSpec(
            shader = shader,
            vertexEntry = "vs_ids",
            buffers = listOf(vertexLayout),
            fragmentEntry = "fs_ids",
            targets = listOf(ColorTarget(context.idFormat)),
            cullMode = CullMode.Back,
            depth = DepthState(context.idDepthFormat),
            label = "cube ids",
        ),
    )
    private val bindGroup = pipeline.bindGroupLayout(0).use { device.createBindGroup(it, listOf(BufferBinding(0, uniforms))) }
    private val idBindGroup = idPipeline.bindGroupLayout(0).use { device.createBindGroup(it, listOf(BufferBinding(0, uniforms))) }
    private val frameData = FloatArray(FRAME_FLOATS)
    private var lastFrame = System.nanoTime()

    init {
        device.queue.writeBuffer(vertices, CUBE_VERTICES)
        device.queue.writeBuffer(indices, CUBE_INDICES)
    }

    override fun render(frame: GpuFrame, state: CubeState): Boolean {
        val now = System.nanoTime()
        if (state.spin) spin.angle += (now - lastFrame) / NANOS_PER_SECOND * SPIN_SPEED
        lastFrame = now

        state.camera.viewProjection(frame.width, frame.height).copyInto(frameData, 0)
        Mat4.rotation(Vec3.Up, spin.angle).copyInto(frameData, 16)
        frameData.put(32, -0.4f, 0.8f, 0.5f, 0f)
        frameData.put(36, state.base)
        frameData.put(40, state.shade)
        frameData[44] = if (state.twoTone) 1f else 0f
        device.queue.writeBuffer(uniforms, frameData)

        val pass = frame.beginPass(state.background)
        pass.setPipeline(pipeline)
        pass.drawCube(bindGroup)
        frame.drawOverlay(pass, axes(state.camera, frame.width, frame.height) + state.overlay)
        pass.end()
        return state.spin
    }

    /** Draws the faces' IDs under the picked pixel, with the spin of the last frame. */
    override fun renderIds(frame: GpuIdFrame, state: CubeState) {
        frame.viewProjection(state.camera).copyInto(frameData, 0)
        device.queue.writeBuffer(uniforms, frameData)
        val pass = frame.beginPass()
        pass.setPipeline(idPipeline)
        pass.drawCube(idBindGroup)
        pass.end()
    }

    private fun GpuRenderPass.drawCube(group: GpuBindGroup) {
        setBindGroup(0, group)
        setVertexBuffer(0, vertices)
        setIndexBuffer(indices, IndexFormat.Uint32)
        drawIndexed(CUBE_INDICES.size)
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
        idBindGroup.close()
        bindGroup.close()
        vertices.close()
        indices.close()
        uniforms.close()
        idPipeline.close()
        pipeline.close()
        shader.close()
    }
}
