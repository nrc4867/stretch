package dev.chieppa.stretch.gpu.webgpu

/** Which triangles a pipeline skips. */
enum class CullMode {
    None,
    Front,
    Back,
}

/** How vertices make primitives. */
enum class PrimitiveTopology {
    PointList,
    LineList,
    LineStrip,
    TriangleList,
    TriangleStrip,
}

/** How a new depth value is compared with the stored one. */
enum class CompareFunction {
    Never,
    Less,
    LessEqual,
    Equal,
    Greater,
    GreaterEqual,
    NotEqual,
    Always,
}

/** One side of a blend equation. */
enum class BlendFactor {
    Zero,
    One,
    Src,
    OneMinusSrc,
    SrcAlpha,
    OneMinusSrcAlpha,
    Dst,
    OneMinusDst,
    DstAlpha,
    OneMinusDstAlpha,
}

/** How the two sides of a blend equation combine. */
enum class BlendOperation {
    Add,
    Subtract,
    ReverseSubtract,
    Min,
    Max,
}

/** Whether a vertex buffer advances per vertex or per instance. */
enum class VertexStepMode {
    Vertex,
    Instance,
}

/** What a pass does with an attachment's contents at its start. */
enum class LoadOp {
    Clear,
    Load,
}

/** What a pass does with an attachment's contents at its end. */
enum class StoreOp {
    Store,
    Discard,
}
