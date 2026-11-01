package dev.chieppa.stretch.gallery.gpu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.frame.NativeSurface
import dev.chieppa.stretch.frame.Viewport
import dev.chieppa.stretch.gpu.OrbitCamera
import dev.chieppa.stretch.gpu.WebGpuRenderer
import dev.chieppa.stretch.gpu.orbitNavigation
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.ui.Segmented
import dev.chieppa.stretch.ui.Toggle

/** How much darker the shadow tone is than the lit one. */
private const val SHADE_DARKEN = 0.35f

private enum class Shading(val label: String) { Flat("Flat"), TwoTone("Two-tone") }

/**
 * A WebGPU viewport inside the frame: a cube drawn by [CubeScene] in a native surface, orbited with the middle button
 * and the wheel, with the scene's axes drawn as an overlay. Labels here are examples, not app text.
 */
@Composable
fun GpuPage() {
    val c = Stretch.colors
    val camera = remember { OrbitCamera(distance = 4.5f) }
    val renderer = remember { WebGpuRenderer<CubeState> { CubeScene(it) } }
    var shading by remember { mutableStateOf(Shading.TwoTone) }
    var spin by remember { mutableStateOf(true) }
    var failure by remember { mutableStateOf<String?>(null) }
    val label = Stretch.type.body.merge(TextStyle(color = c.inkStrong))
    Row(Modifier.fillMaxSize()) {
        Column(
            Modifier.width(260.dp).fillMaxHeight().background(c.surfacePanel).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Segmented(Shading.entries, shading, { shading = it }, { it.label })
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Toggle(spin, { spin = it }, "Spin")
                BasicText("Spin", style = label)
            }
            BasicText("Middle drag orbits, Shift pans, the wheel zooms.", style = label)
            BasicText(failure ?: renderer.description.orEmpty(), style = Stretch.type.bodySm.merge(TextStyle(color = c.inkMuted)))
        }
        Viewport(Modifier.orbitNavigation(camera), grid = false) {
            NativeSurface(
                state = CubeState(camera.view, shading == Shading.TwoTone, spin, c.surfaceWell, c.accent, lerp(c.accent, Color.Black, SHADE_DARKEN)),
                renderer = renderer,
                modifier = Modifier.fillMaxSize(),
                onFailure = { failure = it.message ?: it.toString() },
            )
        }
    }
}
