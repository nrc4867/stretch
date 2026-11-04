package dev.chieppa.stretch.gpu.webgpu

/** Texture formats Stretch draws with. */
enum class TextureFormat {
    BGRA8Unorm,
    RGBA8Unorm,
    BGRA8UnormSrgb,
    RGBA8UnormSrgb,
    RGBA16Float,
    R32Uint,
    Depth24Plus,
    Depth32Float,
}

/** Which part of a texture a copy reads: all of it, or only the depth or the stencil of a depth-stencil texture. */
enum class TextureAspect {
    All,
    DepthOnly,
    StencilOnly,
}

/** The type of one vertex attribute. */
enum class VertexFormat {
    Float32,
    Float32x2,
    Float32x3,
    Float32x4,
    Uint32,
    Uint32x4,
    Unorm8x4,
}

/** The type of an index buffer's entries. */
enum class IndexFormat {
    Uint16,
    Uint32,
}
