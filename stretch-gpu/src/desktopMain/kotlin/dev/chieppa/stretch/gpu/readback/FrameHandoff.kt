package dev.chieppa.stretch.gpu.readback

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image

/** One rendered frame as an image, [startedNanos] being when it started to render; [serial] counts up from 1. */
class ReadbackFrame internal constructor(
    internal val image: Image,
    private val bitmap: Bitmap,
    val serial: Long,
    val startedNanos: Long,
) : AutoCloseable {
    val width: Int get() = image.width
    val height: Int get() = image.height

    override fun close() {
        image.close()
        bitmap.close()
    }
}

/**
 * Hands frames from the render thread to the UI. The render thread [deliver]s each new frame; the UI draws [latest]
 * and says so with [consumed]. Frames older than the last one drawn are closed by [retire] on the render thread, so a
 * frame is never closed while the UI may still draw it. [onConsumed] runs on the UI thread each time it draws a frame
 * it had not drawn before.
 */
class FrameHandoff internal constructor(private val onConsumed: () -> Unit) {
    private val state = mutableStateOf<ReadbackFrame?>(null)
    private val live = ArrayDeque<ReadbackFrame>()
    private var drawn = 0L
    private var serial = 0L

    /** The newest frame. Compose state: reading it in a draw redraws when a frame arrives. */
    val latest: State<ReadbackFrame?> get() = state

    internal fun nextSerial(): Long = ++serial

    internal fun deliver(frame: ReadbackFrame) {
        synchronized(live) { live.addLast(frame) }
        state.value = frame
    }

    /** Frames delivered and not drawn yet. */
    internal val undrawn: Int get() = synchronized(live) { live.count { it.serial > drawn } }

    fun consumed(frame: ReadbackFrame) {
        val first = synchronized(live) { (frame.serial > drawn).also { if (it) drawn = frame.serial } }
        if (first) onConsumed()
    }

    internal fun retire() {
        val old = synchronized(live) {
            buildList { while (live.size > 1 && live.first().serial < drawn) add(live.removeFirst()) }
        }
        old.forEach { it.close() }
    }

    internal fun closeAll() {
        state.value = null
        synchronized(live) { live.toList().also { live.clear() } }.forEach { it.close() }
    }
}
