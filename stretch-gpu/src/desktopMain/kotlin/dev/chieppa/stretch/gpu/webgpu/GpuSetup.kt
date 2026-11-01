package dev.chieppa.stretch.gpu.webgpu

import dev.chieppa.stretch.gpu.ffi.wgpu_h

/** The graphics API wgpu draws through. [bits] are wgpu.h's WGPUInstanceBackend bits. */
enum class GpuBackend(internal val bits: Long) {
    Vulkan(1L shl 0),
    OpenGL(1L shl 1),
    Metal(1L shl 2),
    DX12(1L shl 3),
}

/** How finished frames reach the display. Fifo waits for the display's refresh; Immediate never waits. */
enum class PresentMode(private val value: () -> Int) {
    Fifo(wgpu_h::WGPUPresentMode_Fifo),
    Mailbox(wgpu_h::WGPUPresentMode_Mailbox),
    Immediate(wgpu_h::WGPUPresentMode_Immediate),
    ;

    internal val native: Int get() = value()
}

/** Which GPU to prefer when there are several. */
enum class PowerPreference(private val value: () -> Int) {
    LowPower(wgpu_h::WGPUPowerPreference_LowPower),
    HighPerformance(wgpu_h::WGPUPowerPreference_HighPerformance),
    ;

    internal val native: Int get() = value()
}
