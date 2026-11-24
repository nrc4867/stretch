package dev.chieppa.stretch.gpu.readback

import dev.chieppa.stretch.frame.SurfaceSize
import dev.chieppa.stretch.gpu.GpuFrame
import dev.chieppa.stretch.gpu.GpuOptions
import dev.chieppa.stretch.gpu.GpuScene
import dev.chieppa.stretch.gpu.nativeHandles
import dev.chieppa.webgpu.ffi.WGPUColor
import dev.chieppa.webgpu.ffi.WGPURenderPassColorAttachment
import dev.chieppa.webgpu.ffi.WGPURenderPassDescriptor
import dev.chieppa.webgpu.ffi.wgpu_h
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.jetbrains.skia.Bitmap
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Bounds the wait for a frame, so a broken loop fails the test rather than hanging it. */
private const val TIMEOUT_MILLIS = 30_000L

/** Opaque green, as Skia reads it back. */
private const val GREEN = 0xFF00FF00.toInt()

/**
 * A scene that draws with its own wgpu calls, as an engine with its own bindings does: a pass on the frame's native
 * encoder that clears the frame's native output to green, without [GpuFrame.beginPass].
 */
private class NativeCleared : GpuScene<Unit> {
    override fun render(frame: GpuFrame, state: Unit): Boolean {
        val handles = frame.nativeHandles
        Arena.ofConfined().use { arena ->
            val attachment = arena.allocate(WGPURenderPassColorAttachment.layout())
            WGPURenderPassColorAttachment.view(attachment, MemorySegment.ofAddress(handles.output))
            WGPURenderPassColorAttachment.depthSlice(attachment, -1)
            WGPURenderPassColorAttachment.loadOp(attachment, wgpu_h.WGPULoadOp_Clear())
            WGPURenderPassColorAttachment.storeOp(attachment, wgpu_h.WGPUStoreOp_Store())
            val clear = WGPURenderPassColorAttachment.clearValue(attachment)
            WGPUColor.g(clear, 1.0)
            WGPUColor.a(clear, 1.0)
            val descriptor = arena.allocate(WGPURenderPassDescriptor.layout())
            WGPURenderPassDescriptor.colorAttachmentCount(descriptor, 1)
            WGPURenderPassDescriptor.colorAttachments(descriptor, attachment)
            val pass = wgpu_h.wgpuCommandEncoderBeginRenderPass(MemorySegment.ofAddress(handles.encoder), descriptor)
            wgpu_h.wgpuRenderPassEncoderEnd(pass)
            wgpu_h.wgpuRenderPassEncoderRelease(pass)
        }
        return false
    }
}

/** A scene drawn through the native handles reaches the UI like any other; runs only with -Pgpu. */
class NativeHandlesGpuTest {
    @Test
    fun aPassOnTheNativeHandlesIsWhatTheFrameShows() = runBlocking {
        withTimeout(TIMEOUT_MILLIS) {
            val delivered = Channel<Unit>(Channel.UNLIMITED)
            val failed = CompletableDeferred<Throwable>()
            val errors = mutableListOf<String>()
            val loop = ReadbackLoop(
                Unit,
                GpuOptions(sampleCount = 4, onError = { synchronized(errors) { errors += it } }),
                null,
                onReady = {},
                onFailure = { failed.complete(it) },
                onDelivered = { delivered.trySend(Unit) },
            ) { context ->
                assertTrue(context.nativeHandles.device != 0L && context.nativeHandles.instance != 0L)
                NativeCleared()
            }
            try {
                loop.start()
                loop.resize(SurfaceSize(8, 4, 1f))
                delivered.receive()
                val frame = checkNotNull(loop.frames.latest.value)
                val bitmap = Bitmap.makeFromImage(frame.image)
                val colors = (0 until 4).flatMap { y -> (0 until 8).map { x -> bitmap.getColor(x, y) } }
                bitmap.close()
                assertTrue(colors.all { it == GREEN }, "Frame colors: ${colors.distinct().map(Integer::toHexString)}")
            } finally {
                loop.close()
            }
            assertNull(failed.takeIf { it.isCompleted }?.await(), "The loop failed")
            assertEquals(emptyList(), errors)
        }
    }
}
