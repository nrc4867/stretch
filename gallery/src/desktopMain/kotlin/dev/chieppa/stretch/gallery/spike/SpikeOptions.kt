package dev.chieppa.stretch.gallery.spike

import androidx.compose.ui.unit.IntSize
import dev.chieppa.stretch.gpu.readback.ReadbackMode

/** How the spike's viewport is drawn: not at all (a baseline), or read back one of two ways. */
enum class SpikeMode(val label: String, val readback: ReadbackMode?) {
    None("None", null),
    Naive("Naive", ReadbackMode.Naive),
    Pipelined("Pipelined", ReadbackMode.Pipelined),
}

/** The size frames render at: the viewport's own, or a fixed one scaled to fit, to measure big frames on any screen. */
enum class SpikeResolution(val label: String, val size: IntSize?) {
    Fit("Fit", null),
    P1080("1080p", IntSize(1920, 1080)),
    P1440("1440p", IntSize(2560, 1440)),
    P2160("4K", IntSize(3840, 2160)),
}

/** System properties the spike reads at launch. */
object SpikeProperties {
    /** Draw the viewport with today's native surface instead, popups in windows of their own. */
    const val HEAVY = "spike.heavy"

    /** Run the measurement sequence, print it, and quit. */
    const val BENCH = "spike.bench"
}
