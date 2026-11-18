package dev.chieppa.stretch.gpu.readback

private const val WINDOW = 240
private const val P95 = 0.95
private const val NANOS_PER_MILLI = 1e6

/** The last [WINDOW] samples of one timing, in milliseconds. Safe to add from one thread and read from another. */
class TimingSamples {
    private val values = DoubleArray(WINDOW)
    private var count = 0
    private var next = 0

    @Synchronized fun addNanos(nanos: Long) {
        values[next] = nanos / NANOS_PER_MILLI
        next = (next + 1) % WINDOW
        if (count < WINDOW) count++
    }

    @Synchronized fun summary(): TimingSummary {
        if (count == 0) return TimingSummary.None
        val sorted = values.copyOf(count).also { it.sort() }
        return TimingSummary(sorted.average(), sorted[((count - 1) * P95).toInt()], count)
    }

    @Synchronized fun clear() {
        count = 0
        next = 0
    }
}

/** A timing's mean and 95th percentile in milliseconds over [count] samples. */
data class TimingSummary(val mean: Double, val p95: Double, val count: Int) {
    companion object {
        val None = TimingSummary(0.0, 0.0, 0)
    }
}

/**
 * What a readback surface measures. [gpuWait] is the render thread waiting on the GPU; [copy] is turning mapped bytes
 * into an image; [uiDraw] is the time in the UI thread's draw call; [latency] runs from a frame starting to render to
 * the UI drawing it. [delivered] counts frames handed to the UI, [draws] the times the UI drew one.
 */
class ReadbackStats {
    val gpuWait = TimingSamples()
    val copy = TimingSamples()
    val uiDraw = TimingSamples()
    val latency = TimingSamples()

    @Volatile var delivered: Long = 0
        internal set

    @Volatile var draws: Long = 0
        internal set

    fun clear() {
        gpuWait.clear()
        copy.clear()
        uiDraw.clear()
        latency.clear()
    }
}
