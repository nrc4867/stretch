package dev.chieppa.stretch.gallery.spike

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.frame.NativeSurface
import dev.chieppa.stretch.frame.Popover
import dev.chieppa.stretch.gallery.gpu.CubeState
import dev.chieppa.stretch.gpu.WebGpuRenderer
import dev.chieppa.stretch.gpu.readback.ReadbackSource
import dev.chieppa.stretch.gpu.readback.ReadbackSurface
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.ui.Button
import dev.chieppa.stretch.ui.Slider
import dev.chieppa.stretch.ui.Toggle

/** What draws the viewport: today's native surface, a readback source, or nothing. */
sealed interface SpikeSurface {
    data class Heavy(val renderer: WebGpuRenderer<CubeState>) : SpikeSurface
    data class Readback(val source: ReadbackSource<CubeState>, val renderSize: IntSize?) : SpikeSurface
    data object Blank : SpikeSurface
}

/**
 * The viewport under test: a strip with a button that opens a popover hanging over the scene, and the scene itself.
 * As the layout sways the strip moves, and the popover has to move with it.
 */
@Composable
fun SpikeViewport(state: CubeState, surface: SpikeSurface, modifier: Modifier, input: Modifier, onFailure: (Throwable) -> Unit) {
    val c = Stretch.colors
    Column(modifier) {
        Row(Modifier.fillMaxWidth().background(c.surfacePanel).padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
            BrushButton()
        }
        val fill = Modifier.fillMaxSize().then(input)
        when (surface) {
            is SpikeSurface.Heavy -> NativeSurface(state, surface.renderer, fill, picks = null, onFailure = onFailure)
            is SpikeSurface.Readback -> ReadbackSurface(state, surface.source, fill.background(c.surfaceWell), surface.renderSize)
            SpikeSurface.Blank -> Box(fill.background(c.surfaceWell))
        }
    }
}

/** A button whose popover opens over the scene, with a few live controls to work it with. */
@Composable
private fun BrushButton() {
    var open by remember { mutableStateOf(false) }
    var size by remember { mutableFloatStateOf(0.4f) }
    var soft by remember { mutableStateOf(true) }
    val label = Stretch.type.body.merge(TextStyle(color = Stretch.colors.inkStrong))
    Box {
        Button("Brush", { open = !open })
        Popover(open, onDismiss = { open = false }, title = "Brush") {
            BasicText("Size", style = label)
            Slider(size, { size = it }, "Size")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Toggle(soft, { soft = it }, "Soft edge")
                BasicText("  Soft edge", style = label)
            }
            BasicText("Drag the window or turn on Sway: this should stay pinned under the button.", style = label)
        }
    }
}
