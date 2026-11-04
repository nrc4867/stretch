package dev.chieppa.stretch.gpu

import dev.chieppa.stretch.gpu.webgpu.GpuBackend
import dev.chieppa.stretch.gpu.webgpu.PowerPreference
import dev.chieppa.stretch.gpu.webgpu.PresentMode

/**
 * How a WebGPU device is set up. [backend] picks the API underneath (Vulkan, Direct3D 12, Metal or OpenGL); null lets
 * wgpu choose the best one for the machine. [presentMode] Fifo waits for the display, so a scene that animates draws
 * once per refresh. [sampleCount] is the multisampling of the frame's color and depth targets: 4 smooths edges, 1
 * turns it off. [onError] gets validation and out-of-memory errors the scene's own calls cause, and the device's loss,
 * on whichever thread wgpu reports them.
 */
data class GpuOptions(
    val backend: GpuBackend? = null,
    val presentMode: PresentMode = PresentMode.Fifo,
    val powerPreference: PowerPreference = PowerPreference.HighPerformance,
    val sampleCount: Int = 4,
    val onError: (String) -> Unit = ::reportGpuError,
)

/** Where errors go when the app gives no [GpuOptions.onError]: the platform's error output. */
internal expect fun reportGpuError(message: String)
