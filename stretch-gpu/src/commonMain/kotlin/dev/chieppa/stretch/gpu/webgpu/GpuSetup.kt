package dev.chieppa.stretch.gpu.webgpu

/** The graphics API wgpu draws through. [bits] are wgpu.h's WGPUInstanceBackend bits. */
enum class GpuBackend(internal val bits: Long) {
    Vulkan(1L shl 0),
    OpenGL(1L shl 1),
    Metal(1L shl 2),
    DX12(1L shl 3),
}

/** How finished frames reach the display. Fifo waits for the display's refresh; Immediate never waits. */
enum class PresentMode {
    Fifo,
    Mailbox,
    Immediate,
}

/** Which GPU to prefer when there are several. */
enum class PowerPreference {
    LowPower,
    HighPerformance,
}
