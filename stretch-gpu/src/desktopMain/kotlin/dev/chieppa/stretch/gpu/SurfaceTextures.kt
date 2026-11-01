package dev.chieppa.stretch.gpu

import io.ygdrasil.webgpu.GPUTexture
import io.ygdrasil.webgpu.NativeSurface
import io.ygdrasil.webgpu.Texture
import java.lang.foreign.MemorySegment

/**
 * The surface's next texture to draw into, or null when the surface gave none and has to be reconfigured. Decided by
 * whether a texture came back rather than by the reported status: wgpu4k 0.1.1 maps wgpu-native's statuses one value
 * off, so a texture acquired normally reports as a timeout. A texture that is acquired must be presented before the
 * next one is asked for.
 */
internal fun NativeSurface.acquireTexture(): GPUTexture? {
    val texture = getCurrentTexture().texture
    val handle = (texture as? Texture)?.handler?.handler
    return texture.takeUnless { handle == null || handle == MemorySegment.NULL }
}
