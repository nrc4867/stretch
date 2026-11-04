@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.chieppa.stretch.gpu

private fun consoleError(message: String): Unit = js("console.error(message)")

internal actual fun reportGpuError(message: String) = consoleError("[webgpu] $message")

private fun hasWebGpu(): Boolean = js("!!navigator.gpu")

/** Whether this browser offers WebGPU. An adapter can still be missing; a surface then reports the failure. */
fun webGpuAvailable(): Boolean = hasWebGpu()
