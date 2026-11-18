package dev.chieppa.stretch.gpu.readback

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image

/** One rendered frame as an image; [serial] counts up from 1. */
internal class ReadbackFrame(val image: Image, private val bitmap: Bitmap, val serial: Long) : AutoCloseable {
    override fun close() {
        image.close()
        bitmap.close()
    }
}

/**
 * Hands frames from the render thread to the UI. The render thread [deliver]s each new frame; the UI draws [latest]
 * and says so with [drawn]. Frames older than the last one drawn are closed by [retire] on the render thread, so a
 * frame is never closed while the UI may still draw it. [onFirstDraw] runs on the UI thread each time it draws a frame
 * it had not drawn before.
 */
internal class FrameHandoff(private val onFirstDraw: () -> Unit) {
    private val state = mutableStateOf<ReadbackFrame?>(null)
    private val live = ArrayDeque<ReadbackFrame>()
    private var lastDrawn = 0L
    private var serial = 0L

    /** The newest frame. Compose state: reading it in a draw redraws when a frame arrives. */
    val latest: State<ReadbackFrame?> get() = state

    /** Frames delivered and not drawn yet. */
    val undrawn: Int get() = synchronized(live) { live.count { it.serial > lastDrawn } }

    fun nextSerial(): Long = ++serial

    fun deliver(frame: ReadbackFrame) {
        synchronized(live) { live.addLast(frame) }
        state.value = frame
    }

    fun drawn(frame: ReadbackFrame) {
        val first = synchronized(live) { (frame.serial > lastDrawn).also { if (it) lastDrawn = frame.serial } }
        if (first) onFirstDraw()
    }

    fun retire() {
        val old = synchronized(live) {
            buildList { while (live.size > 1 && live.first().serial < lastDrawn) add(live.removeFirst()) }
        }
        old.forEach { it.close() }
    }

    fun closeAll() {
        state.value = null
        synchronized(live) { live.toList().also { live.clear() } }.forEach { it.close() }
    }
}
