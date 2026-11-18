package dev.chieppa.stretch.gallery.spike

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.gallery.gpu.CubeScene
import dev.chieppa.stretch.gallery.gpu.CubeSpin
import dev.chieppa.stretch.gallery.gpu.CubeState
import dev.chieppa.stretch.gpu.OrbitCamera
import dev.chieppa.stretch.gpu.WebGpuRenderer
import dev.chieppa.stretch.gpu.orbitNavigation
import dev.chieppa.stretch.gpu.readback.readbackSource
import dev.chieppa.stretch.theme.Stretch
import kotlinx.coroutines.delay
import kotlin.math.sin

private const val SHADE_DARKEN = 0.35f
private const val PANEL_WIDTH = 280f
private const val SWAY_DP = 90f
private const val SWAY_RADIANS_PER_SECOND = 1.6
private const val NANOS_PER_SECOND = 1e9
private const val READING_MILLIS = 1000L

/**
 * The readback spike: the gallery's cube in a viewport drawn by today's native surface when [heavy], or else read back
 * into Compose, beside its settings and live readings. With [bench] it runs the measurement sequence and calls
 * [onBenchDone]. [renderApi] names what Compose draws with.
 */
@Composable
fun ReadbackSpikePage(heavy: Boolean, bench: Boolean, renderApi: String, onBenchDone: () -> Unit) {
    val c = Stretch.colors
    val camera = remember { OrbitCamera(distance = 4.5f) }
    val spin = remember { CubeSpin() }
    var mode by remember { mutableStateOf(if (heavy) SpikeMode.None else SpikeMode.Pipelined) }
    var resolution by remember { mutableStateOf(SpikeResolution.Fit) }
    var spinning by remember { mutableStateOf(true) }
    var sway by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }
    val meter = remember { FrameMeter() }
    val state = CubeState(camera.view, twoTone = true, spin = spinning, c.surfaceWell, c.accent, lerp(c.accent, Color.Black, SHADE_DARKEN))

    val surface = rememberSpikeSurface(heavy, mode, resolution, state, spin) { failure = it.message ?: it.toString() }
    val stats = (surface as? SpikeSurface.Readback)?.source?.stats
    val description = when (surface) {
        is SpikeSurface.Heavy -> surface.renderer.description
        is SpikeSurface.Readback -> surface.source.description
        SpikeSurface.Blank -> null
    }
    val header = "Compose on $renderApi · GPU ${description ?: "–"}"

    RunFrameMeter(meter)
    var readings by remember { mutableStateOf(emptyList<String>()) }
    // The bench measures windows of its own; readings would clear them.
    if (!bench) LaunchedEffect(surface) {
        while (true) {
            val window = SpikeWindow.start(meter, stats)
            delay(READING_MILLIS)
            readings = window.report(meter, stats).lines() + listOfNotNull(failure)
        }
    }
    if (bench) {
        RunBench(
            title = "${if (heavy) "heavyweight" else "readback"} · Compose on $renderApi",
            cases = if (heavy) HeavyBenchCases else ReadbackBenchCases,
            meter = meter,
            stats = { (surface as? SpikeSurface.Readback)?.source?.stats },
            apply = { case ->
                if (!heavy) mode = case.mode
                resolution = case.resolution
            },
            onDone = onBenchDone,
        )
    }

    var swayOffset by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(sway) {
        if (!sway) {
            swayOffset = 0f
            return@LaunchedEffect
        }
        val start = withFrameNanos { it }
        while (true) withFrameNanos { now -> swayOffset = (sin((now - start) / NANOS_PER_SECOND * SWAY_RADIANS_PER_SECOND) * SWAY_DP).toFloat() }
    }

    Row(Modifier.fillMaxSize()) {
        SpikeControls(
            heavy = heavy,
            mode = mode,
            onMode = { mode = it },
            resolution = resolution,
            onResolution = { resolution = it },
            spinning = spinning,
            onSpinning = { spinning = it },
            sway = sway,
            onSway = { sway = it },
            header = header,
            readings = readings,
            modifier = Modifier.width((PANEL_WIDTH + swayOffset).dp).fillMaxHeight().background(c.surfacePanel).padding(16.dp),
        )
        SpikeViewport(
            state = state,
            surface = surface,
            modifier = Modifier.weight(1f).fillMaxHeight(),
            input = Modifier.orbitNavigation(camera),
            onFailure = { failure = it.message ?: it.toString() },
        )
    }
}

/** The surface for the current settings, made once per setting and closed when it changes. */
@Composable
private fun rememberSpikeSurface(
    heavy: Boolean,
    mode: SpikeMode,
    resolution: SpikeResolution,
    state: CubeState,
    spin: CubeSpin,
    onFailure: (Throwable) -> Unit,
): SpikeSurface {
    if (heavy) return remember { SpikeSurface.Heavy(WebGpuRenderer { CubeScene(it, spin) }) }
    val readback = mode.readback ?: return SpikeSurface.Blank
    val source = remember(readback) { readbackSource(readback, state, onFailure = onFailure) { CubeScene(it, spin) } }
    DisposableEffect(source) { onDispose { source.close() } }
    return SpikeSurface.Readback(source, resolution.size)
}
