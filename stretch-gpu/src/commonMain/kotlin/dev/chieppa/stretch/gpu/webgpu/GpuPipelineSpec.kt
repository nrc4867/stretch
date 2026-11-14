package dev.chieppa.stretch.gpu.webgpu

/** One attribute in a vertex buffer: its [format], its byte [offset] in a vertex, and the shader's `@location`. */
data class VertexAttribute(val format: VertexFormat, val offset: Long, val location: Int)

/** One vertex buffer's layout: [stride] bytes per vertex (or instance, by [stepMode]) and its [attributes]. */
data class VertexBufferLayout(
    val stride: Long,
    val attributes: List<VertexAttribute>,
    val stepMode: VertexStepMode = VertexStepMode.Vertex,
)

/** One side of a blend equation: `src * srcFactor (operation) dst * dstFactor`. */
data class BlendComponent(
    val srcFactor: BlendFactor = BlendFactor.One,
    val dstFactor: BlendFactor = BlendFactor.Zero,
    val operation: BlendOperation = BlendOperation.Add,
)

/** How a color target blends new colors over old ones. */
data class BlendState(val color: BlendComponent, val alpha: BlendComponent) {
    companion object {
        /** Straight alpha over what is there. */
        val AlphaBlending = BlendState(
            color = BlendComponent(BlendFactor.SrcAlpha, BlendFactor.OneMinusSrcAlpha),
            alpha = BlendComponent(BlendFactor.One, BlendFactor.OneMinusSrcAlpha),
        )
    }
}

/** A color target a pipeline writes, in [format], blended by [blend] or replaced when it is null. */
data class ColorTarget(val format: TextureFormat, val blend: BlendState? = null)

/** The depth test: the buffer's [format], whether draws write it, and how they [compare]. */
data class DepthState(val format: TextureFormat, val write: Boolean = true, val compare: CompareFunction = CompareFunction.Less)

/**
 * Everything a render pipeline is made of: one [shader] module with a vertex entry point and an optional fragment
 * one, the vertex [buffers], the color [targets], how primitives are assembled and culled, the [depth] test, and
 * the multisample count. Bind group layouts come from the shader's declarations.
 */
data class RenderPipelineSpec(
    val shader: GpuShaderModule,
    val vertexEntry: String,
    val buffers: List<VertexBufferLayout>,
    val fragmentEntry: String?,
    val targets: List<ColorTarget>,
    val topology: PrimitiveTopology = PrimitiveTopology.TriangleList,
    val cullMode: CullMode = CullMode.None,
    val depth: DepthState? = null,
    val sampleCount: Int = 1,
    val label: String? = null,
)

/**
 * A compute pipeline: the [entry] point of [shader] it runs. Bind group layouts come from the shader's declarations,
 * read-only (`var<storage, read>`) and read-write (`var<storage, read_write>`) storage buffers among them; only the
 * bindings the entry point uses are in them.
 */
data class ComputePipelineSpec(val shader: GpuShaderModule, val entry: String, val label: String? = null)
