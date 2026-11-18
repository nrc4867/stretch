package dev.chieppa.stretch.gallery.spike

import dev.chieppa.stretch.gpu.readback.ReadbackStats
import dev.chieppa.stretch.gpu.readback.TimingSummary

private const val BYTES_PER_MB = 1024.0 * 1024.0
private const val NANOS_PER_SECOND = 1e9
private const val NANOS_PER_MILLI = 1e6

/** One reading of everything the spike measures, over the time since [SpikeWindow.start]. */
data class SpikeReport(
    val uiFps: Double,
    val deliveredFps: Double,
    val drawsPerSecond: Double,
    val interval: TimingSummary,
    val uiCpuPerFrame: Double,
    val processCpuPerFrame: Double,
    val gpuWait: TimingSummary?,
    val copy: TimingSummary?,
    val uiDraw: TimingSummary?,
    val latency: TimingSummary?,
    val gcPerSecond: Double,
    val gcMillisPerSecond: Double,
    val heapMb: Double,
) {
    fun lines(): List<String> = buildList {
        add("UI ${uiFps.f1} fps · viewport drawn ${drawsPerSecond.f1}/s · frames made ${deliveredFps.f1}/s")
        add("UI frame gap ${interval.show()}")
        add("CPU per UI frame: UI thread ${uiCpuPerFrame.f2} ms, whole app ${processCpuPerFrame.f2} ms")
        gpuWait?.let { add("GPU wait ${it.show()}") }
        copy?.let { add("Copy to image ${it.show()}") }
        uiDraw?.let { add("UI draw call ${it.show()}") }
        latency?.let { add("Render start → drawn ${it.show()}") }
        add("GC ${gcPerSecond.f1}/s, ${gcMillisPerSecond.f1} ms/s · heap ${heapMb.f0} MB")
    }

    /** One tab-separated row for the bench output. */
    fun row(): String = listOf(
        uiFps.f1, drawsPerSecond.f1, deliveredFps.f1, interval.p95.f2, uiCpuPerFrame.f2, processCpuPerFrame.f2,
        gpuWait?.mean?.f2 ?: "-", copy?.mean?.f2 ?: "-", copy?.p95?.f2 ?: "-", uiDraw?.mean?.f2 ?: "-",
        latency?.mean?.f2 ?: "-", latency?.p95?.f2 ?: "-", gcPerSecond.f1, gcMillisPerSecond.f1, heapMb.f0,
    ).joinToString("\t")

    companion object {
        const val HEADER = "ui fps	draws/s	made/s	gap p95	ui cpu/frame	app cpu/frame	gpu wait	copy	copy p95	draw call	latency	latency p95	gc/s	gc ms/s	heap MB"
    }
}

/** The start of a measuring window: counts to subtract when the window is read. */
class SpikeWindow private constructor(
    private val nanos: Long,
    private val uiFrames: Long,
    private val uiCpuNanos: Long,
    private val processCpuNanos: Long,
    private val delivered: Long,
    private val draws: Long,
    private val gc: GcTotals,
) {
    fun report(meter: FrameMeter, stats: ReadbackStats?): SpikeReport {
        val seconds = ((System.nanoTime() - nanos) / NANOS_PER_SECOND).coerceAtLeast(1e-3)
        val gcNow = GcTotals.now()
        val runtime = Runtime.getRuntime()
        return SpikeReport(
            uiFps = (meter.frames - uiFrames) / seconds,
            deliveredFps = stats?.let { (it.delivered - delivered) / seconds } ?: 0.0,
            drawsPerSecond = stats?.let { (it.draws - draws) / seconds } ?: 0.0,
            interval = meter.interval.summary(),
            uiCpuPerFrame = (meter.uiCpuNanos - uiCpuNanos) / NANOS_PER_MILLI / (meter.frames - uiFrames).coerceAtLeast(1),
            processCpuPerFrame = (processCpuNanos() - processCpuNanos) / NANOS_PER_MILLI / (meter.frames - uiFrames).coerceAtLeast(1),
            gpuWait = stats?.gpuWait?.summary(),
            copy = stats?.copy?.summary(),
            uiDraw = stats?.uiDraw?.summary(),
            latency = stats?.latency?.summary(),
            gcPerSecond = (gcNow.count - gc.count) / seconds,
            gcMillisPerSecond = (gcNow.millis - gc.millis) / seconds,
            heapMb = (runtime.totalMemory() - runtime.freeMemory()) / BYTES_PER_MB,
        )
    }

    companion object {
        fun start(meter: FrameMeter, stats: ReadbackStats?): SpikeWindow {
            meter.clear()
            stats?.clear()
            return SpikeWindow(System.nanoTime(), meter.frames, meter.uiCpuNanos, processCpuNanos(), stats?.delivered ?: 0, stats?.draws ?: 0, GcTotals.now())
        }
    }
}

/** CPU time the whole process has used, on all its threads. */
private fun processCpuNanos(): Long =
    (java.lang.management.ManagementFactory.getOperatingSystemMXBean() as com.sun.management.OperatingSystemMXBean).processCpuTime

private fun TimingSummary.show() = if (count == 0) "–" else "${mean.f2} ms (p95 ${p95.f2})"

private val Double.f0 get() = "%.0f".format(this)
private val Double.f1 get() = "%.1f".format(this)
private val Double.f2 get() = "%.2f".format(this)
