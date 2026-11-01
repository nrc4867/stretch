package dev.chieppa.stretch.gpu.webgpu

import androidx.compose.ui.graphics.Color

/**
 * A color attachment of a render pass: the [view] drawn into, an optional single-sample [resolveTarget] a
 * multisampled view resolves into, and what happens to its contents at the start and end of the pass.
 */
data class ColorAttachment(
    val view: GpuTextureView,
    val resolveTarget: GpuTextureView? = null,
    val load: LoadOp = LoadOp.Clear,
    val store: StoreOp = StoreOp.Store,
    val clear: Color = Color.Transparent,
)

/** The depth attachment of a render pass, cleared to [clearDepth] when [load] clears it. */
data class DepthAttachment(
    val view: GpuTextureView,
    val clearDepth: Float = 1f,
    val load: LoadOp = LoadOp.Clear,
    val store: StoreOp = StoreOp.Discard,
)

/** A texture to make: its size in pixels, [format], [usage], and multisample count. */
data class TextureSpec(
    val width: Int,
    val height: Int,
    val format: TextureFormat,
    val usage: TextureUsage,
    val sampleCount: Int = 1,
    val label: String? = null,
)

/** A buffer bound at [binding] in a bind group, from [offset] for [size] bytes, or to its end when size is null. */
data class BufferBinding(val binding: Int, val buffer: GpuBuffer, val offset: Long = 0, val size: Long? = null)
