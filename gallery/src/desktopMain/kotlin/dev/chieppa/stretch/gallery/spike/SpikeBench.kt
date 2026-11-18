package dev.chieppa.stretch.gallery.spike

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import dev.chieppa.stretch.gpu.readback.ReadbackStats
import kotlinx.coroutines.delay

/** How long each bench case settles, then measures. */
private const val SETTLE_MILLIS = 2500L
private const val MEASURE_MILLIS = 5000L

/** One bench case: a way of drawing at a size. */
data class BenchCase(val mode: SpikeMode, val resolution: SpikeResolution)

/** The cases a readback run measures: the empty baseline once, then each readback mode at each size. */
val ReadbackBenchCases: List<BenchCase> = listOf(BenchCase(SpikeMode.None, SpikeResolution.Fit)) +
    listOf(SpikeMode.Naive, SpikeMode.Pipelined).flatMap { mode -> SpikeResolution.entries.map { BenchCase(mode, it) } }

/** The heavyweight run measures only what it can: the UI thread at the window's size. */
val HeavyBenchCases: List<BenchCase> = listOf(BenchCase(SpikeMode.None, SpikeResolution.Fit))

/**
 * Steps through [cases], applying each with [apply], letting it settle, measuring, and printing a row; then prints a
 * blank line and calls [onDone]. [stats] gives the current case's readback stats, if it has any.
 */
@Composable
fun RunBench(
    title: String,
    cases: List<BenchCase>,
    meter: FrameMeter,
    stats: () -> ReadbackStats?,
    apply: (BenchCase) -> Unit,
    onDone: () -> Unit,
) {
    val currentStats by rememberUpdatedState(stats)
    LaunchedEffect(cases) {
        println("BENCH $title · display ${java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.displayMode.refreshRate} Hz")
        println("BENCH mode\tsize\t${SpikeReport.HEADER}")
        for (case in cases) {
            apply(case)
            delay(SETTLE_MILLIS)
            val window = SpikeWindow.start(meter, currentStats())
            delay(MEASURE_MILLIS)
            println("BENCH ${case.mode.label}\t${case.resolution.label}\t${window.report(meter, currentStats()).row()}")
        }
        println("BENCH done")
        onDone()
    }
}

