package dev.chieppa.stretch.gallery.gpu

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.frame.BoxTransform
import dev.chieppa.stretch.frame.Guide
import dev.chieppa.stretch.frame.Hud
import dev.chieppa.stretch.frame.NativeSurface
import dev.chieppa.stretch.frame.SurfacePickQueue
import dev.chieppa.stretch.frame.Viewport
import dev.chieppa.stretch.frame.ViewportCamera
import dev.chieppa.stretch.frame.guidesOverlay
import dev.chieppa.stretch.frame.handlesOverlay
import dev.chieppa.stretch.frame.marquee
import dev.chieppa.stretch.frame.marqueeOverlay
import dev.chieppa.stretch.frame.rememberGuideDragState
import dev.chieppa.stretch.frame.rememberMarqueeState
import dev.chieppa.stretch.frame.transformHandleInput
import dev.chieppa.stretch.gpu.GpuPick
import dev.chieppa.stretch.gpu.OrbitCamera
import dev.chieppa.stretch.gpu.Vec3
import dev.chieppa.stretch.gpu.WebGpuRenderer
import dev.chieppa.stretch.gpu.orbitNavigation
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.ui.Segmented
import dev.chieppa.stretch.ui.Toggle
import dev.chieppa.stretch.ui.formatNumber
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.floor

/** How much darker the shadow tone is than the lit one, and how much lighter a picked cube is. */
private const val SHADE_DARKEN = 0.35f
private const val PICKED_LIGHTEN = 0.35f

/** Where the frame the handles go around starts, in the view's pixels. */
private val START_FRAME = Rect(60f, 60f, 260f, 180f)
private const val START_GUIDE = 140f

private const val DEGREES_PER_RADIAN = (180 / PI).toFloat()

/** The middle of a pixel, where its depth was drawn. */
private const val PIXEL_CENTRE = 0.5f

private enum class Shading(val label: String) { Flat("Flat"), TwoTone("Two-tone") }

/** How a click finds the cube: a ray against its box, or the GPU's ID pass. */
private enum class PickPath(val label: String) { Ray("Ray"), Gpu("GPU") }

/**
 * A WebGPU viewport inside the frame: a cube drawn by [CubeScene] in a native surface, orbited with the middle button,
 * the wheel and two fingers, with the scene's axes drawn as an overlay. Rulers sit beside the surface; guides, a frame
 * with handles and the marquee are drawn by the renderer; the toolbar and a readout float over it in popups. Clicking
 * with the pick tool finds the cube by ray or on the GPU. Labels here are examples, not app text.
 */
@Composable
fun GpuPage() {
    val c = Stretch.colors
    val density = LocalDensity.current
    val camera = remember { OrbitCamera(distance = 4.5f) }
    val spin = remember { CubeSpin() }
    val renderer = remember { WebGpuRenderer<CubeState> { CubeScene(it, spin) } }
    val picks = remember { SurfacePickQueue<GpuPick>() }
    // The rulers, guides, marquee and frame measure the view in pixels, so their camera stays put.
    val view = remember { ViewportCamera() }
    var guides by remember { mutableStateOf(listOf(Guide(vertical = true, position = START_GUIDE))) }
    val guideDrag = rememberGuideDragState()
    val marquee = rememberMarqueeState()
    var frame by remember { mutableStateOf(BoxTransform(START_FRAME)) }
    var tool by remember { mutableStateOf(GpuTool.Pick) }
    var folded by remember { mutableStateOf(false) }
    var path by remember { mutableStateOf(PickPath.Ray) }
    var shading by remember { mutableStateOf(Shading.TwoTone) }
    var spinning by remember { mutableStateOf(true) }
    var picked by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Pick the cube with a click.") }
    var failure by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val label = Stretch.type.body.merge(TextStyle(color = c.inkStrong))

    fun pickAt(at: Offset) {
        val width = view.viewSize.width.toInt()
        val height = view.viewSize.height.toInt()
        if (width <= 0 || height <= 0) return
        val lens = camera.view
        when (path) {
            PickPath.Ray -> {
                val hit = rayPickCube(lens.ray(at, width, height), spin.angle)
                picked = hit != null
                status = if (hit == null) "Ray: nothing there." else "Ray: the cube at ${hit.readout()}."
            }
            PickPath.Gpu -> scope.launch {
                val pick = picks.pick(at)
                picked = pick != null
                status = if (pick == null) {
                    "GPU: nothing there."
                } else {
                    val centre = Offset(floor(at.x) + PIXEL_CENTRE, floor(at.y) + PIXEL_CENTRE)
                    val world = lens.unproject(centre, pick.depth, width, height)
                    "GPU: face ${pick.id - CUBE_FIRST_ID + 1} (ID ${pick.id}) at ${world?.readout()}."
                }
            }
        }
    }

    Row(Modifier.fillMaxSize()) {
        Column(
            Modifier.width(260.dp).fillMaxHeight().background(c.surfacePanel).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Segmented(Shading.entries, shading, { shading = it }, { it.label })
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Toggle(spinning, { spinning = it }, "Spin")
                BasicText("Spin", style = label)
            }
            BasicText("Middle drag orbits, Shift pans, the wheel zooms. Two fingers pan, pinch and twist.", style = label)
            BasicText("Pick with", style = label)
            Segmented(PickPath.entries, path, { path = it }, { it.label })
            BasicText(status, style = label)
            BasicText(failure ?: renderer.description.orEmpty(), style = Stretch.type.bodySm.merge(TextStyle(color = c.inkMuted)))
        }
        Viewport(
            Modifier.orbitNavigation(camera),
            grid = false,
            camera = view,
            rulers = true,
            guides = guides,
            onGuidesChange = { guides = it },
            guideDrag = guideDrag,
            nativeContent = true,
            topStart = { GpuToolbar(tool, { tool = it }, folded, { folded = it }) },
            bottomEnd = { Hud(camera.readout()) },
        ) {
            val overlay = guidesOverlay(view, guideDrag.shown(guides), c.accent) +
                frame.handlesOverlay(view, c.ringSelected, c.surfaceFloat, density) +
                marqueeOverlay(marquee.box, c.ringSelected)
            val input = when (tool) {
                GpuTool.Pick -> Modifier.pointerInput(path) { detectTapGestures(onTap = ::pickAt) }
                GpuTool.Select -> Modifier
                    .marquee(marquee, view) { area, _ -> status = "Marquee: ${formatNumber(area.width)} by ${formatNumber(area.height)} px." }
                    .transformHandleInput(frame, view, { frame = it })
            }
            val base = if (picked) lerp(c.accent, Color.White, PICKED_LIGHTEN) else c.accent
            NativeSurface(
                state = CubeState(camera.view, shading == Shading.TwoTone, spinning, c.surfaceWell, base, lerp(base, Color.Black, SHADE_DARKEN), overlay),
                renderer = renderer,
                modifier = Modifier.fillMaxSize().then(input),
                picks = picks,
                onFailure = { failure = it.message ?: it.toString() },
            )
        }
    }
}

private fun Vec3.readout() = "(${formatNumber(x)}, ${formatNumber(y)}, ${formatNumber(z)})"

private fun OrbitCamera.readout() =
    "yaw ${formatNumber(yaw * DEGREES_PER_RADIAN)}° · pitch ${formatNumber(pitch * DEGREES_PER_RADIAN)}° · ${formatNumber(distance)} units"
