package dev.chieppa.stretch.gpu.webgpu

/** WebGPU's names for Stretch's enums, as its JavaScript API spells them. */
internal val TextureFormat.webName: String
    get() = when (this) {
        TextureFormat.BGRA8Unorm -> "bgra8unorm"
        TextureFormat.RGBA8Unorm -> "rgba8unorm"
        TextureFormat.BGRA8UnormSrgb -> "bgra8unorm-srgb"
        TextureFormat.RGBA8UnormSrgb -> "rgba8unorm-srgb"
        TextureFormat.RGBA16Float -> "rgba16float"
        TextureFormat.R32Uint -> "r32uint"
        TextureFormat.Depth24Plus -> "depth24plus"
        TextureFormat.Depth32Float -> "depth32float"
    }

internal val TextureAspect.webName: String
    get() = when (this) {
        TextureAspect.All -> "all"
        TextureAspect.DepthOnly -> "depth-only"
        TextureAspect.StencilOnly -> "stencil-only"
    }

internal val VertexFormat.webName: String
    get() = when (this) {
        VertexFormat.Float32 -> "float32"
        VertexFormat.Float32x2 -> "float32x2"
        VertexFormat.Float32x3 -> "float32x3"
        VertexFormat.Float32x4 -> "float32x4"
        VertexFormat.Uint32 -> "uint32"
        VertexFormat.Uint32x4 -> "uint32x4"
        VertexFormat.Unorm8x4 -> "unorm8x4"
    }

internal val IndexFormat.webName: String
    get() = when (this) {
        IndexFormat.Uint16 -> "uint16"
        IndexFormat.Uint32 -> "uint32"
    }

internal val CullMode.webName: String
    get() = when (this) {
        CullMode.None -> "none"
        CullMode.Front -> "front"
        CullMode.Back -> "back"
    }

internal val PrimitiveTopology.webName: String
    get() = when (this) {
        PrimitiveTopology.PointList -> "point-list"
        PrimitiveTopology.LineList -> "line-list"
        PrimitiveTopology.LineStrip -> "line-strip"
        PrimitiveTopology.TriangleList -> "triangle-list"
        PrimitiveTopology.TriangleStrip -> "triangle-strip"
    }

internal val CompareFunction.webName: String
    get() = when (this) {
        CompareFunction.Never -> "never"
        CompareFunction.Less -> "less"
        CompareFunction.LessEqual -> "less-equal"
        CompareFunction.Equal -> "equal"
        CompareFunction.Greater -> "greater"
        CompareFunction.GreaterEqual -> "greater-equal"
        CompareFunction.NotEqual -> "not-equal"
        CompareFunction.Always -> "always"
    }

internal val BlendFactor.webName: String
    get() = when (this) {
        BlendFactor.Zero -> "zero"
        BlendFactor.One -> "one"
        BlendFactor.Src -> "src"
        BlendFactor.OneMinusSrc -> "one-minus-src"
        BlendFactor.SrcAlpha -> "src-alpha"
        BlendFactor.OneMinusSrcAlpha -> "one-minus-src-alpha"
        BlendFactor.Dst -> "dst"
        BlendFactor.OneMinusDst -> "one-minus-dst"
        BlendFactor.DstAlpha -> "dst-alpha"
        BlendFactor.OneMinusDstAlpha -> "one-minus-dst-alpha"
    }

internal val BlendOperation.webName: String
    get() = when (this) {
        BlendOperation.Add -> "add"
        BlendOperation.Subtract -> "subtract"
        BlendOperation.ReverseSubtract -> "reverse-subtract"
        BlendOperation.Min -> "min"
        BlendOperation.Max -> "max"
    }

internal val VertexStepMode.webName: String
    get() = when (this) {
        VertexStepMode.Vertex -> "vertex"
        VertexStepMode.Instance -> "instance"
    }

internal val LoadOp.webName: String
    get() = when (this) {
        LoadOp.Clear -> "clear"
        LoadOp.Load -> "load"
    }

internal val StoreOp.webName: String
    get() = when (this) {
        StoreOp.Store -> "store"
        StoreOp.Discard -> "discard"
    }

internal val PowerPreference.webName: String
    get() = when (this) {
        PowerPreference.LowPower -> "low-power"
        PowerPreference.HighPerformance -> "high-performance"
    }
