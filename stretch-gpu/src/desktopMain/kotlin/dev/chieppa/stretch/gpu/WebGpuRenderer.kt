package dev.chieppa.stretch.gpu

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.chieppa.stretch.frame.SurfaceHandle
import dev.chieppa.stretch.frame.SurfacePicker
import dev.chieppa.stretch.frame.SurfaceRenderer
import dev.chieppa.stretch.frame.SurfaceSize
import dev.chieppa.stretch.gpu.webgpu.BufferUsage
import dev.chieppa.stretch.gpu.webgpu.GpuBuffer
import dev.chieppa.stretch.gpu.webgpu.GpuInstance
import dev.chieppa.stretch.gpu.webgpu.TextureAspect
import dev.chieppa.stretch.gpu.webgpu.TextureFormat

/** The color formats a frame prefers, in order: plain 8-bit, so colors match Compose's. */
private val PREFERRED_FORMATS = listOf(TextureFormat.BGRA8Unorm, TextureFormat.RGBA8Unorm)

/**
 * Draws a [GpuScene] into a [dev.chieppa.stretch.frame.NativeSurface] with WebGPU. On attach it sets up a device for
 * the surface and makes the scene with [scene]; each frame it keeps the surface and the frame's targets at the
 * surface's size, lets the scene record its commands, then submits and presents them. A surface with no texture for
 * now is configured again and drawn on the next frame; a lost device or a WebGPU error ends the surface.
 *
 * It picks too: given a [dev.chieppa.stretch.frame.SurfacePickQueue], the surface asks it what is under a pixel, and it
 * has the scene draw the last state's IDs for that one pixel (see [GpuScene.renderIds]) and reads back its ID and
 * depth.
 */
class WebGpuRenderer<S>(
    private val options: GpuOptions = GpuOptions(),
    private val scene: (GpuContext) -> GpuScene<S>,
) : SurfaceRenderer<S>, SurfacePicker<GpuPick> {
    private var gpu: GpuInstance? = null
    private var context: GpuContext? = null
    private var targets: RenderTargets? = null
    private var current: GpuScene<S>? = null
    private var configured = SurfaceSize.Zero
    private var ids: IdTargets? = null
    private var pickReadback: GpuBuffer? = null

    /** The last frame's state and size, which a pick draws again. */
    private var last: Pair<S, SurfaceSize>? = null

    /** The GPU and API in use once attached. Compose state, so a label showing it updates. */
    var description: String? by mutableStateOf(null)
        private set

    override fun attach(handle: SurfaceHandle) {
        val gpu = GpuInstance.create(handle, options.backend, options.powerPreference, options.onError).also { gpu = it }
        val formats = gpu.surfaceFormats()
        val format = PREFERRED_FORMATS.firstOrNull { it in formats } ?: formats.firstOrNull()
            ?: error("The surface offers no format Stretch can draw")
        val context = GpuContext(gpu.device, format, options.sampleCount, gpu.description).also { context = it }
        targets = RenderTargets(context)
        current = scene(context)
        description = gpu.description
    }

    override fun render(state: S, size: SurfaceSize): Boolean {
        val gpu = checkNotNull(gpu) { "Not attached" }
        val context = checkNotNull(context)
        gpu.lost?.let { error(it) }
        val surface = checkNotNull(gpu.surface)
        if (size.width != configured.width || size.height != configured.height) configure(size)
        last = state to size
        val texture = surface.acquire()
        if (texture == null) {
            configure(size)
            return true
        }
        // The surface's texture is released only after it is presented: wgpu drops a frame whose texture is gone.
        texture.use {
            val animating = texture.createView().use { output ->
                val targets = checkNotNull(targets).also { it.ensure(size.width, size.height) }
                context.device.createCommandEncoder().use { encoder ->
                    val animating = checkNotNull(current).render(GpuFrame(context, encoder, size, targets, output), state)
                    context.device.queue.submit(encoder.finish())
                    animating
                }
            }
            if (!surface.present()) configured = SurfaceSize.Zero
            return animating
        }
    }

    override fun pick(x: Int, y: Int): GpuPick? {
        val context = checkNotNull(context) { "Not attached" }
        val (state, size) = last ?: return null
        val device = context.device
        val targets = ids ?: IdTargets(context).also { ids = it }
        val readback = pickReadback
            ?: device.createBuffer(GpuPick.BUFFER_BYTES.toLong(), BufferUsage.CopyDst + BufferUsage.MapRead, "pick readback").also { pickReadback = it }
        device.createCommandEncoder().use { encoder ->
            checkNotNull(current).renderIds(GpuIdFrame(context, encoder, size, x, y, targets.ids.view, targets.depth.view), state)
            encoder.copyTextureToBuffer(targets.ids.texture, readback, COPY_ROW_ALIGNMENT, bufferOffset = GpuPick.ID_OFFSET.toLong())
            encoder.copyTextureToBuffer(
                targets.depth.texture, readback, COPY_ROW_ALIGNMENT, bufferOffset = GpuPick.DEPTH_OFFSET.toLong(), aspect = TextureAspect.DepthOnly,
            )
            device.queue.submit(encoder.finish())
        }
        return GpuPick.decode(device.readBuffer(readback))
    }

    private fun configure(size: SurfaceSize) {
        val gpu = checkNotNull(gpu)
        checkNotNull(gpu.surface).configure(gpu.device, checkNotNull(context).format, size.width, size.height, options.presentMode)
        configured = size
    }

    override fun detach() {
        pickReadback?.close()
        ids?.close()
        current?.close()
        targets?.close()
        context?.close()
        gpu?.close()
        pickReadback = null
        ids = null
        last = null
        current = null
        targets = null
        context = null
        gpu = null
        configured = SurfaceSize.Zero
    }
}
