package dev.chieppa.stretch.gpu

import io.ygdrasil.webgpu.GPUPowerPreference
import io.ygdrasil.webgpu.PresentMode
import io.ygdrasil.webgpu.WGPUInstanceBackend

/**
 * How a WebGPU device is set up. [backend] picks the API underneath (Vulkan, D3D12, Metal or OpenGL); null lets
 * wgpu choose the best one for the machine. [presentMode] Fifo waits for the display, so a scene that animates draws
 * once per refresh. [sampleCount] is the multisampling of the frame's color and depth targets: 4 smooths edges, 1
 * turns it off. [onError] gets validation and out-of-memory errors the scene's own calls cause.
 */
data class GpuOptions(
    val backend: WGPUInstanceBackend? = null,
    val presentMode: PresentMode = PresentMode.Fifo,
    val powerPreference: GPUPowerPreference = GPUPowerPreference.HighPerformance,
    val sampleCount: Int = 4,
    val onError: (String) -> Unit = { System.err.println("[webgpu] $it") },
)
