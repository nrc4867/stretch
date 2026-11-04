package dev.chieppa.stretch.gpu

internal actual fun reportGpuError(message: String) = System.err.println("[webgpu] $message")
