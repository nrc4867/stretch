package dev.chieppa.stretch.gallery.spike

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import dev.chieppa.stretch.gpu.readback.TimingSamples
import java.lang.management.ManagementFactory

/**
 * The UI thread's frames: the time between them, and the CPU time the UI thread has spent in all, which covers
 * composing, drawing and handing images to the GPU. Windows counts thread CPU time in 15.6 ms steps, so it is only
 * meaningful summed over many frames. Ticked from [RunFrameMeter].
 */
class FrameMeter {
    val interval = TimingSamples()

    @Volatile var frames: Long = 0
        private set

    @Volatile var uiCpuNanos: Long = 0
        private set

    private val threads = ManagementFactory.getThreadMXBean()
    private var lastFrame = 0L
    private var lastCpu = 0L

    fun tick(frameNanos: Long) {
        val cpu = threads.currentThreadCpuTime
        if (lastFrame != 0L) {
            interval.addNanos(frameNanos - lastFrame)
            uiCpuNanos += cpu - lastCpu
        }
        lastFrame = frameNanos
        lastCpu = cpu
        frames++
    }

    fun clear() = interval.clear()
}

/** Ticks [meter] on every frame while composed. That keeps frames coming, as an animating viewport would anyway. */
@Composable
fun RunFrameMeter(meter: FrameMeter) {
    LaunchedEffect(meter) {
        while (true) withFrameNanos(meter::tick)
    }
}

/** Garbage collections so far and the milliseconds they took, over all collectors. */
data class GcTotals(val count: Long, val millis: Long) {
    companion object {
        fun now(): GcTotals {
            val beans = ManagementFactory.getGarbageCollectorMXBeans()
            return GcTotals(beans.sumOf { it.collectionCount.coerceAtLeast(0) }, beans.sumOf { it.collectionTime.coerceAtLeast(0) })
        }
    }
}
