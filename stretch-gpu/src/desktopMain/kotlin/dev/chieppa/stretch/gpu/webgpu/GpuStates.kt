package dev.chieppa.stretch.gpu.webgpu

import dev.chieppa.stretch.gpu.ffi.wgpu_h

/** Which triangles a pipeline skips. */
enum class CullMode(private val value: () -> Int) {
    None(wgpu_h::WGPUCullMode_None),
    Front(wgpu_h::WGPUCullMode_Front),
    Back(wgpu_h::WGPUCullMode_Back),
    ;

    internal val native: Int get() = value()
}

/** How vertices make primitives. */
enum class PrimitiveTopology(private val value: () -> Int) {
    PointList(wgpu_h::WGPUPrimitiveTopology_PointList),
    LineList(wgpu_h::WGPUPrimitiveTopology_LineList),
    LineStrip(wgpu_h::WGPUPrimitiveTopology_LineStrip),
    TriangleList(wgpu_h::WGPUPrimitiveTopology_TriangleList),
    TriangleStrip(wgpu_h::WGPUPrimitiveTopology_TriangleStrip),
    ;

    internal val native: Int get() = value()
}

/** How a new depth value is compared with the stored one. */
enum class CompareFunction(private val value: () -> Int) {
    Never(wgpu_h::WGPUCompareFunction_Never),
    Less(wgpu_h::WGPUCompareFunction_Less),
    LessEqual(wgpu_h::WGPUCompareFunction_LessEqual),
    Equal(wgpu_h::WGPUCompareFunction_Equal),
    Greater(wgpu_h::WGPUCompareFunction_Greater),
    GreaterEqual(wgpu_h::WGPUCompareFunction_GreaterEqual),
    NotEqual(wgpu_h::WGPUCompareFunction_NotEqual),
    Always(wgpu_h::WGPUCompareFunction_Always),
    ;

    internal val native: Int get() = value()
}

/** One side of a blend equation. */
enum class BlendFactor(private val value: () -> Int) {
    Zero(wgpu_h::WGPUBlendFactor_Zero),
    One(wgpu_h::WGPUBlendFactor_One),
    Src(wgpu_h::WGPUBlendFactor_Src),
    OneMinusSrc(wgpu_h::WGPUBlendFactor_OneMinusSrc),
    SrcAlpha(wgpu_h::WGPUBlendFactor_SrcAlpha),
    OneMinusSrcAlpha(wgpu_h::WGPUBlendFactor_OneMinusSrcAlpha),
    Dst(wgpu_h::WGPUBlendFactor_Dst),
    OneMinusDst(wgpu_h::WGPUBlendFactor_OneMinusDst),
    DstAlpha(wgpu_h::WGPUBlendFactor_DstAlpha),
    OneMinusDstAlpha(wgpu_h::WGPUBlendFactor_OneMinusDstAlpha),
    ;

    internal val native: Int get() = value()
}

/** How the two sides of a blend equation combine. */
enum class BlendOperation(private val value: () -> Int) {
    Add(wgpu_h::WGPUBlendOperation_Add),
    Subtract(wgpu_h::WGPUBlendOperation_Subtract),
    ReverseSubtract(wgpu_h::WGPUBlendOperation_ReverseSubtract),
    Min(wgpu_h::WGPUBlendOperation_Min),
    Max(wgpu_h::WGPUBlendOperation_Max),
    ;

    internal val native: Int get() = value()
}

/** Whether a vertex buffer advances per vertex or per instance. */
enum class VertexStepMode(private val value: () -> Int) {
    Vertex(wgpu_h::WGPUVertexStepMode_Vertex),
    Instance(wgpu_h::WGPUVertexStepMode_Instance),
    ;

    internal val native: Int get() = value()
}

/** What a pass does with an attachment's contents at its start. */
enum class LoadOp(private val value: () -> Int) {
    Clear(wgpu_h::WGPULoadOp_Clear),
    Load(wgpu_h::WGPULoadOp_Load),
    ;

    internal val native: Int get() = value()
}

/** What a pass does with an attachment's contents at its end. */
enum class StoreOp(private val value: () -> Int) {
    Store(wgpu_h::WGPUStoreOp_Store),
    Discard(wgpu_h::WGPUStoreOp_Discard),
    ;

    internal val native: Int get() = value()
}
