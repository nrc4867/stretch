package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.test.setStretchContent
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class NativeViewportTest {
    private fun near(a: Float, b: Float, slack: Float = 0.5f) = abs(a - b) <= slack

    private val margins = CornerMargins(top = 12, start = 12, end = 12, bottom = 12, gap = 8)

    private fun place(width: Int, ts: IntSize? = null, te: IntSize? = null, bs: IntSize? = null, be: IntSize? = null, bc: IntSize? = null, ltr: Boolean = true) =
        placeCorners(listOf(ts, te, bs, be, bc), width, 400, margins, ltr)

    @Test
    fun endCornersMoveUnderStartOnesWhenThereIsNoRoom() {
        val bar = IntSize(200, 40)
        val wide = place(800, ts = bar, te = bar)
        assertEquals(IntRect(12, 12, 212, 52), wide[CornerSlot.TopStart.ordinal])
        assertEquals(IntRect(588, 12, 788, 52), wide[CornerSlot.TopEnd.ordinal], "side by side when there is room")

        val narrow = place(380, ts = bar, te = bar)
        assertEquals(60, narrow[CornerSlot.TopEnd.ordinal]!!.top, "under the toolbar, 8px apart")

        val readout = IntSize(200, 20)
        val bottom = place(380, bs = readout, be = readout)
        assertEquals(bottom[CornerSlot.BottomStart.ordinal]!!.top - 28, bottom[CornerSlot.BottomEnd.ordinal]!!.top, "above the readout")
    }

    @Test
    fun theBottomCentreRisesAboveCornersItWouldTouch() {
        val places = place(300, bs = IntSize(200, 30), bc = IntSize(120, 40))
        val readout = places[CornerSlot.BottomStart.ordinal]!!
        val toast = places[CornerSlot.BottomCenter.ordinal]!!
        assertEquals(readout.top - 8, toast.bottom)
        assertNull(places[CornerSlot.TopStart.ordinal], "an empty slot has no place")
    }

    @Test
    fun rightToLeftMirrorsTheCorners() {
        val places = place(300, ts = IntSize(40, 40), ltr = false)
        assertEquals(IntRect(248, 12, 288, 52), places[CornerSlot.TopStart.ordinal])
    }

    @Test
    fun guidesBecomeLinesAcrossTheView() {
        val camera = ViewportCamera(zoom = 2f, offset = Offset(10f, 0f))
        camera.viewSize = Size(400f, 300f)
        val overlay = guidesOverlay(camera, listOf(Guide(vertical = true, position = 50f), Guide(vertical = false, position = 20f)), Color.Red)
        assertEquals(OverlayLine(Offset(110f, 0f), Offset(110f, 300f), Color.Red), overlay.lines[0])
        assertEquals(OverlayLine(Offset(0f, 40f), Offset(400f, 40f), Color.Red), overlay.lines[1])
    }

    @Test
    fun handlesSitOnTheBoxCornersEdgesAndStem() {
        val camera = ViewportCamera()
        val overlay = BoxTransform(Rect(100f, 100f, 200f, 160f)).handlesOverlay(camera, Color.Blue, Color.White, Density(1f))
        assertEquals(5, overlay.lines.size, "four edges and the stem")
        assertEquals(Offset(100f, 100f), overlay.lines[0].from)
        val centres = overlay.dots.map { it.center }.distinct()
        assertEquals(9, centres.size, "eight resize handles and the rotate handle")
        assertTrue(Offset(200f, 160f) in centres)
        assertTrue(Offset(150f, 100f - 22f) in centres, "the rotate handle sits above the top edge")
        assertTrue(overlay.dots.chunked(2).all { (ring, fill) -> ring.color == Color.Blue && fill.color == Color.White && ring.radius > fill.radius })
    }

    @Test
    fun turnedHandlesTurnWithTheBox() {
        val camera = ViewportCamera()
        val overlay = BoxTransform(Rect(100f, 100f, 200f, 200f), rotation = 90f).handlesOverlay(camera, Color.Blue, Color.White, Density(1f))
        val corner = overlay.lines[0].from
        assertTrue(near(corner.x, 200f) && near(corner.y, 100f), "a quarter turn clockwise takes the top-left to the top-right: $corner")
        assertTrue(overlay.dots.any { near(it.center.x, 150f + 50f + 22f) && near(it.center.y, 150f) }, "the rotate handle is now on the right")
    }

    @Test
    fun aMarqueeIsATintAndAnEdge() {
        val box = Rect(10f, 10f, 60f, 50f)
        val overlay = marqueeOverlay(box, Color.Blue)
        assertEquals(box, overlay.rects.single().rect)
        assertEquals(4, overlay.lines.size)
        assertTrue(marqueeOverlay(null, Color.Blue).isEmpty)
    }

    @Test
    fun aGuideBeingDraggedShowsAtThePointer() {
        val drag = GuideDragState()
        val guides = listOf(Guide(vertical = true, position = 10f), Guide(vertical = false, position = 20f))
        assertEquals(guides, drag.shown(guides))
        drag.moving = 0
        drag.pending = Guide(vertical = true, position = 30f)
        assertEquals(listOf(guides[1], Guide(vertical = true, position = 30f)), drag.shown(guides))
    }

    @Test
    fun rulersFormABandBesideNativeContent() = runComposeUiTest {
        val camera = ViewportCamera()
        var guides by mutableStateOf(emptyList<Guide>())
        val drag = GuideDragState()
        setStretchContent {
            Box(Modifier.size(400.dp, 300.dp)) {
                Viewport(
                    Modifier.testTag("view"),
                    camera = camera,
                    rulers = true,
                    guides = guides,
                    onGuidesChange = { guides = it },
                    guideDrag = drag,
                    nativeContent = true,
                ) { Box(Modifier.fillMaxSize().testTag("scene")) }
            }
        }
        val scene = onNodeWithTag("scene").fetchSemanticsNode().boundsInRoot
        val ruler = 20f * density.density
        assertTrue(near(scene.left, ruler) && near(scene.top, ruler), "the content starts past the rulers: $scene")
        assertTrue(near(camera.viewSize.width, 400f * density.density - ruler), "the camera's view is the content: ${camera.viewSize}")

        onNodeWithContentDescription("Horizontal ruler. Drag down into the view to place a guide").performTouchInput {
            down(Offset(150f, 10f))
            moveTo(Offset(150f, 60f))
            moveTo(Offset(150f, 120f + ruler))
            up()
        }
        val guide = guides.single()
        assertTrue(!guide.vertical && near(guide.position, 120f, 2f), "a guide at the content's y, not the viewport's: $guide")
    }

    @Test
    fun nativeContentPutsTheCornersInPopups() = runComposeUiTest {
        setStretchContent {
            Box(Modifier.size(400.dp, 300.dp)) {
                Viewport(
                    nativeContent = true,
                    topStart = { Box(Modifier.size(80.dp, 30.dp).testTag("tools")) },
                    bottomEnd = { Box(Modifier.size(60.dp, 20.dp).testTag("readout")) },
                ) {}
            }
        }
        onNode(hasTestTag("tools") and hasAnyAncestor(isPopup())).assertExists()
        onNode(hasTestTag("readout") and hasAnyAncestor(isPopup())).assertExists()
        val tools = onNodeWithTag("tools").fetchSemanticsNode().boundsInRoot
        val readout = onNodeWithTag("readout").fetchSemanticsNode().boundsInRoot
        assertTrue(near(tools.left, 12f * density.density) && near(tools.top, 12f * density.density), "the toolbar in the top-start corner: $tools")
        assertTrue(near(readout.right, 388f * density.density) && near(readout.bottom, 288f * density.density), "the readout in the bottom-end corner: $readout")
    }

    @Test
    fun aCornerThatChangesSizeIsPlacedAgain() = runComposeUiTest {
        var wide by mutableStateOf(true)
        setStretchContent {
            Box(Modifier.size(400.dp, 300.dp)) {
                Viewport(nativeContent = true, bottomEnd = { Box(Modifier.size(if (wide) 200.dp else 60.dp, 20.dp).testTag("readout")) }) {}
            }
        }
        wide = false
        waitForIdle()
        val readout = onNodeWithTag("readout").fetchSemanticsNode().boundsInRoot
        assertTrue(near(readout.right, 388f * density.density) && near(readout.width, 60f * density.density), "still in its corner after folding: $readout")
    }
}
