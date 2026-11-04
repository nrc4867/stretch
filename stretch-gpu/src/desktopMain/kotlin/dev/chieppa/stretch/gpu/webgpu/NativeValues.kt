package dev.chieppa.stretch.gpu.webgpu

import dev.chieppa.stretch.gpu.ffi.wgpu_h

internal val TextureFormat.native: Int
    get() = when (this) {
        TextureFormat.BGRA8Unorm -> wgpu_h.WGPUTextureFormat_BGRA8Unorm()
        TextureFormat.RGBA8Unorm -> wgpu_h.WGPUTextureFormat_RGBA8Unorm()
        TextureFormat.BGRA8UnormSrgb -> wgpu_h.WGPUTextureFormat_BGRA8UnormSrgb()
        TextureFormat.RGBA8UnormSrgb -> wgpu_h.WGPUTextureFormat_RGBA8UnormSrgb()
        TextureFormat.RGBA16Float -> wgpu_h.WGPUTextureFormat_RGBA16Float()
        TextureFormat.R32Uint -> wgpu_h.WGPUTextureFormat_R32Uint()
        TextureFormat.Depth24Plus -> wgpu_h.WGPUTextureFormat_Depth24Plus()
        TextureFormat.Depth32Float -> wgpu_h.WGPUTextureFormat_Depth32Float()
    }

internal val TextureAspect.native: Int
    get() = when (this) {
        TextureAspect.All -> wgpu_h.WGPUTextureAspect_All()
        TextureAspect.DepthOnly -> wgpu_h.WGPUTextureAspect_DepthOnly()
        TextureAspect.StencilOnly -> wgpu_h.WGPUTextureAspect_StencilOnly()
    }

internal val VertexFormat.native: Int
    get() = when (this) {
        VertexFormat.Float32 -> wgpu_h.WGPUVertexFormat_Float32()
        VertexFormat.Float32x2 -> wgpu_h.WGPUVertexFormat_Float32x2()
        VertexFormat.Float32x3 -> wgpu_h.WGPUVertexFormat_Float32x3()
        VertexFormat.Float32x4 -> wgpu_h.WGPUVertexFormat_Float32x4()
        VertexFormat.Uint32 -> wgpu_h.WGPUVertexFormat_Uint32()
        VertexFormat.Uint32x4 -> wgpu_h.WGPUVertexFormat_Uint32x4()
        VertexFormat.Unorm8x4 -> wgpu_h.WGPUVertexFormat_Unorm8x4()
    }

internal val IndexFormat.native: Int
    get() = when (this) {
        IndexFormat.Uint16 -> wgpu_h.WGPUIndexFormat_Uint16()
        IndexFormat.Uint32 -> wgpu_h.WGPUIndexFormat_Uint32()
    }

internal val CullMode.native: Int
    get() = when (this) {
        CullMode.None -> wgpu_h.WGPUCullMode_None()
        CullMode.Front -> wgpu_h.WGPUCullMode_Front()
        CullMode.Back -> wgpu_h.WGPUCullMode_Back()
    }

internal val PrimitiveTopology.native: Int
    get() = when (this) {
        PrimitiveTopology.PointList -> wgpu_h.WGPUPrimitiveTopology_PointList()
        PrimitiveTopology.LineList -> wgpu_h.WGPUPrimitiveTopology_LineList()
        PrimitiveTopology.LineStrip -> wgpu_h.WGPUPrimitiveTopology_LineStrip()
        PrimitiveTopology.TriangleList -> wgpu_h.WGPUPrimitiveTopology_TriangleList()
        PrimitiveTopology.TriangleStrip -> wgpu_h.WGPUPrimitiveTopology_TriangleStrip()
    }

internal val CompareFunction.native: Int
    get() = when (this) {
        CompareFunction.Never -> wgpu_h.WGPUCompareFunction_Never()
        CompareFunction.Less -> wgpu_h.WGPUCompareFunction_Less()
        CompareFunction.LessEqual -> wgpu_h.WGPUCompareFunction_LessEqual()
        CompareFunction.Equal -> wgpu_h.WGPUCompareFunction_Equal()
        CompareFunction.Greater -> wgpu_h.WGPUCompareFunction_Greater()
        CompareFunction.GreaterEqual -> wgpu_h.WGPUCompareFunction_GreaterEqual()
        CompareFunction.NotEqual -> wgpu_h.WGPUCompareFunction_NotEqual()
        CompareFunction.Always -> wgpu_h.WGPUCompareFunction_Always()
    }

internal val BlendFactor.native: Int
    get() = when (this) {
        BlendFactor.Zero -> wgpu_h.WGPUBlendFactor_Zero()
        BlendFactor.One -> wgpu_h.WGPUBlendFactor_One()
        BlendFactor.Src -> wgpu_h.WGPUBlendFactor_Src()
        BlendFactor.OneMinusSrc -> wgpu_h.WGPUBlendFactor_OneMinusSrc()
        BlendFactor.SrcAlpha -> wgpu_h.WGPUBlendFactor_SrcAlpha()
        BlendFactor.OneMinusSrcAlpha -> wgpu_h.WGPUBlendFactor_OneMinusSrcAlpha()
        BlendFactor.Dst -> wgpu_h.WGPUBlendFactor_Dst()
        BlendFactor.OneMinusDst -> wgpu_h.WGPUBlendFactor_OneMinusDst()
        BlendFactor.DstAlpha -> wgpu_h.WGPUBlendFactor_DstAlpha()
        BlendFactor.OneMinusDstAlpha -> wgpu_h.WGPUBlendFactor_OneMinusDstAlpha()
    }

internal val BlendOperation.native: Int
    get() = when (this) {
        BlendOperation.Add -> wgpu_h.WGPUBlendOperation_Add()
        BlendOperation.Subtract -> wgpu_h.WGPUBlendOperation_Subtract()
        BlendOperation.ReverseSubtract -> wgpu_h.WGPUBlendOperation_ReverseSubtract()
        BlendOperation.Min -> wgpu_h.WGPUBlendOperation_Min()
        BlendOperation.Max -> wgpu_h.WGPUBlendOperation_Max()
    }

internal val VertexStepMode.native: Int
    get() = when (this) {
        VertexStepMode.Vertex -> wgpu_h.WGPUVertexStepMode_Vertex()
        VertexStepMode.Instance -> wgpu_h.WGPUVertexStepMode_Instance()
    }

internal val LoadOp.native: Int
    get() = when (this) {
        LoadOp.Clear -> wgpu_h.WGPULoadOp_Clear()
        LoadOp.Load -> wgpu_h.WGPULoadOp_Load()
    }

internal val StoreOp.native: Int
    get() = when (this) {
        StoreOp.Store -> wgpu_h.WGPUStoreOp_Store()
        StoreOp.Discard -> wgpu_h.WGPUStoreOp_Discard()
    }

internal val PresentMode.native: Int
    get() = when (this) {
        PresentMode.Fifo -> wgpu_h.WGPUPresentMode_Fifo()
        PresentMode.Mailbox -> wgpu_h.WGPUPresentMode_Mailbox()
        PresentMode.Immediate -> wgpu_h.WGPUPresentMode_Immediate()
    }

internal val PowerPreference.native: Int
    get() = when (this) {
        PowerPreference.LowPower -> wgpu_h.WGPUPowerPreference_LowPower()
        PowerPreference.HighPerformance -> wgpu_h.WGPUPowerPreference_HighPerformance()
    }

/** The format with webgpu.h value [value], or null for one Stretch does not name. */
internal fun textureFormatOf(value: Int): TextureFormat? = TextureFormat.entries.firstOrNull { it.native == value }
