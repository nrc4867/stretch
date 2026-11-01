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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.test.setStretchContent
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ViewportToolsTest {
    private fun near(a: Float, b: Float, slack: Float = 0.5f) = abs(a - b) <= slack

    @Test
    fun theCameraZoomsAboutAPointAndFits() {
        val camera = ViewportCamera()
        camera.viewSize = Size(800f, 600f)
        val focus = Offset(200f, 100f)
        val before = camera.toScene(focus)
        camera.zoomTo(2f, focus)
        val after = camera.toScene(focus)
        assertTrue(near(before.x, after.x) && near(before.y, after.y), "the point under the pointer stays put")
        camera.zoomTo(100f)
        assertEquals(camera.zoomRange.endInclusive, camera.zoom)

        camera.fit(Rect(0f, 0f, 400f, 100f), margin = 0f)
        assertEquals(2f, camera.zoom)
        val centre = camera.toScreen(Offset(200f, 50f))
        assertTrue(near(centre.x, 400f) && near(centre.y, 300f), "the scene sits in the middle")
    }

    @Test
    fun rulerTicksAreOneTwoOrFiveTimesAPowerOfTen() {
        assertEquals(100f, rulerStep(1f))
        assertEquals(50f, rulerStep(2f))
        assertEquals(500f, rulerStep(0.25f))
        assertEquals(2f, rulerStep(40f))
    }

    @Test
    fun resizingKeepsTheOppositeSideWhereItWas() {
        val box = BoxTransform(Rect(0f, 0f, 100f, 50f))
        assertEquals(Rect(0f, 0f, 150f, 80f), resize(box, 4, Offset(150f, 80f), keepRatio = false).bounds)
        assertEquals(Rect(0f, 0f, 160f, 80f), resize(box, 4, Offset(160f, 60f), keepRatio = true).bounds, "Shift keeps 2:1")
        assertEquals(Rect(0f, 0f, 100f, 90f), resize(box, 5, Offset(400f, 90f), keepRatio = false).bounds, "an edge handle moves one side")

        val turned = BoxTransform(Rect(0f, 0f, 100f, 50f), rotation = 90f)
        val anchorBefore = rotated(turned, Offset(0f, 0f))
        val grown = resize(turned, 4, rotated(turned, Offset(140f, 70f)), keepRatio = false)
        val anchorAfter = rotated(grown, grown.bounds.topLeft)
        assertTrue(near(anchorBefore.x, anchorAfter.x) && near(anchorBefore.y, anchorAfter.y), "a rotated box keeps its anchor: $anchorBefore $anchorAfter")
        assertTrue(near(grown.bounds.width, 140f) && near(grown.bounds.height, 70f))
    }

    /** Where unrotated point [p] of [t]'s box ends up once the box is turned. */
    private fun rotated(t: BoxTransform, p: Offset): Offset {
        val r = t.rotation * kotlin.math.PI.toFloat() / 180f
        val d = p - t.bounds.center
        return t.bounds.center + Offset(d.x * kotlin.math.cos(r) - d.y * kotlin.math.sin(r), d.x * kotlin.math.sin(r) + d.y * kotlin.math.cos(r))
    }

    @Test
    fun twoFingersPanAndPinchTheView() = runComposeUiTest {
        val camera = ViewportCamera()
        setStretchContent { Box(Modifier.size(400.dp, 300.dp)) { Viewport(Modifier.testTag("view"), camera = camera) {} } }
        onNodeWithTag("view").performTouchInput {
            down(0, Offset(150f, 150f))
            down(1, Offset(250f, 150f))
            repeat(10) {
                updatePointerBy(0, Offset(-5f, 0f))
                updatePointerBy(1, Offset(5f, 0f))
                move()
            }
            up(0)
            up(1)
        }
        assertTrue(camera.zoom > 1.8f, "spreading two fingers zooms in: ${camera.zoom}")

        val zoom = camera.zoom
        val offset = camera.offset
        onNodeWithTag("view").performTouchInput {
            down(0, Offset(150f, 150f))
            down(1, Offset(250f, 150f))
            repeat(10) {
                updatePointerBy(0, Offset(0f, 4f))
                updatePointerBy(1, Offset(0f, 4f))
                move()
            }
            up(0)
            up(1)
        }
        assertTrue(near(camera.zoom, zoom, 0.01f), "moving together only pans")
        assertTrue(camera.offset.y - offset.y > 30f, "the view followed the fingers down")

        val one = camera.offset
        onNodeWithTag("view").performTouchInput { swipe(Offset(100f, 100f), Offset(200f, 100f)) }
        assertEquals(one, camera.offset, "one finger is left to the tools")
    }

    @Test
    fun theWheelPansAndCtrlWheelZooms() = runComposeUiTest {
        val camera = ViewportCamera()
        setStretchContent { Box(Modifier.size(400.dp, 300.dp)) { Viewport(Modifier.testTag("view"), camera = camera) {} } }
        onNodeWithTag("view").performMouseInput { scroll(2f) }
        assertTrue(camera.offset.y < -50f, "scrolling down moves the scene up: ${camera.offset}")
        assertEquals(1f, camera.zoom)
    }

    @Test
    fun guidesComeOutOfTheRulersAndGoBack() = runComposeUiTest {
        val camera = ViewportCamera()
        var guides by mutableStateOf(emptyList<Guide>())
        setStretchContent {
            Box(Modifier.size(400.dp, 300.dp)) {
                Viewport(Modifier.testTag("view"), camera = camera, rulers = true, guides = guides, onGuidesChange = { guides = it }) {}
            }
        }
        onNodeWithContentDescription("Horizontal ruler. Drag down into the view to place a guide").performTouchInput {
            down(Offset(150f, 10f))
            moveTo(Offset(150f, 60f))
            moveTo(Offset(150f, 120f))
            up()
        }
        assertEquals(1, guides.size)
        val guide = guides.single()
        assertTrue(!guide.vertical && near(guide.position, camera.toScene(Offset(0f, 120f)).y, 2f), "$guide")

        onNodeWithTag("view").performTouchInput {
            val at = camera.toScreen(Offset(0f, guide.position)).y
            down(Offset(200f, at + 1f))
            moveTo(Offset(200f, at - 40f))
            moveTo(Offset(200f, 5f))
            up()
        }
        assertTrue(guides.isEmpty(), "dropped back on the ruler: $guides")
    }

    @Test
    fun marqueeAndHandlesShareTheCanvas() = runComposeUiTest {
        val camera = ViewportCamera()
        var box by mutableStateOf(BoxTransform(Rect(100f, 100f, 200f, 160f)))
        var marquee by mutableStateOf<Rect?>(null)
        var finished = 0
        setStretchContent {
            Box(
                Modifier
                    .size(400.dp, 300.dp)
                    .testTag("scene")
                    .marquee(camera) { r, _ -> marquee = r }
                    .transformHandles(box, camera, { box = it }, { finished++ }),
            ) { Box(Modifier.fillMaxSize()) }
        }
        onNodeWithTag("scene").performTouchInput {
            down(Offset(150f, 130f))
            moveTo(Offset(170f, 140f))
            moveTo(Offset(190f, 150f))
            up()
        }
        assertNull(marquee, "dragging inside the box moves it rather than starting a marquee")
        assertEquals(Rect(140f, 120f, 240f, 180f), box.bounds)
        assertEquals(1, finished)

        onNodeWithTag("scene").performTouchInput {
            down(Offset(240f, 180f))
            moveTo(Offset(260f, 190f))
            moveTo(Offset(280f, 200f))
            up()
        }
        assertEquals(Rect(140f, 120f, 280f, 200f), box.bounds, "the bottom-right handle resizes")

        onNodeWithTag("scene").performTouchInput {
            down(Offset(10f, 10f))
            moveTo(Offset(40f, 30f))
            moveTo(Offset(60f, 50f))
            up()
        }
        val area = assertNotNull(marquee)
        assertEquals(Rect(10f, 10f, 60f, 50f), area)
    }

    @Test
    fun theMouseDragsHandlesToo() = runComposeUiTest {
        val camera = ViewportCamera()
        var box by mutableStateOf(BoxTransform(Rect(100f, 100f, 200f, 160f)))
        setStretchContent {
            Box(Modifier.size(400.dp, 300.dp).testTag("scene").marquee(camera) { _, _ -> }.transformHandles(box, camera, { box = it })) {}
        }
        onNodeWithTag("scene").performMouseInput {
            moveTo(Offset(200f, 160f))
            press()
            moveTo(Offset(220f, 170f))
            moveTo(Offset(240f, 180f))
            release()
        }
        assertEquals(Rect(100f, 100f, 240f, 180f), box.bounds, "the corner handle follows the mouse")
        val turned = box.rotation
        onNodeWithTag("scene").performMouseInput {
            moveTo(Offset(170f, 100f - 22f * density))
            press()
            moveTo(Offset(260f, 140f))
            moveTo(Offset(300f, 140f))
            release()
        }
        assertTrue(box.rotation != turned, "the round handle rotates: ${box.rotation}")
    }

    @Test
    fun theSceneStillGetsThePointerUnderRulersAndGuides() = runComposeUiTest {
        val camera = ViewportCamera()
        var box by mutableStateOf(BoxTransform(Rect(100f, 100f, 200f, 160f)))
        var guides by mutableStateOf(listOf(Guide(vertical = true, position = 300f)))
        setStretchContent {
            Box(Modifier.size(400.dp, 300.dp)) {
                Viewport(Modifier.testTag("view"), camera = camera, rulers = true, guides = guides, onGuidesChange = { guides = it }) {
                    Box(Modifier.fillMaxSize().marquee(camera) { _, _ -> }.transformHandles(box, camera, { box = it }))
                }
            }
        }
        onNodeWithTag("view").performMouseInput {
            moveTo(Offset(200f, 160f))
            press()
            moveTo(Offset(220f, 170f))
            moveTo(Offset(240f, 180f))
            release()
            exit()
        }
        assertEquals(Rect(100f, 100f, 240f, 180f), box.bounds, "the corner handle follows the mouse with rulers on")

        onNodeWithTag("view").performTouchInput {
            down(Offset(300f, 250f))
            moveTo(Offset(330f, 250f))
            moveTo(Offset(350f, 250f))
            up()
        }
        assertTrue(near(guides.single().position, camera.toScene(Offset(350f, 0f)).x, 2f), "guides still drag: $guides")
        assertEquals(Rect(100f, 100f, 240f, 180f), box.bounds, "dragging a guide leaves the scene alone")
    }

    @Test
    fun fingersReachFurtherForGuidesAndRulersThanTheMouse() {
        val camera = ViewportCamera()
        val guides = listOf(Guide(vertical = true, position = 200f), Guide(vertical = false, position = 150f))
        val ruler = 20f
        fun grab(at: Offset, reach: Float, rulerReach: Float, blocked: List<Rect> = emptyList()) =
            grabAt(camera, guides, at, ruler, reach, rulerReach, blocked)

        assertEquals(GuideGrab(vertical = true, index = 0), grab(Offset(204f, 250f), reach = 5f, rulerReach = 0f))
        assertNull(grab(Offset(212f, 250f), reach = 5f, rulerReach = 0f), "the mouse must be close")
        assertEquals(GuideGrab(vertical = true, index = 0), grab(Offset(212f, 250f), reach = 14f, rulerReach = 14f), "a finger reaches")
        assertEquals(GuideGrab(vertical = false, index = 1), grab(Offset(300f, 147f), reach = 5f, rulerReach = 0f))

        assertEquals(GuideGrab(vertical = false, index = null), grab(Offset(300f, 10f), reach = 5f, rulerReach = 0f), "the top ruler places a horizontal guide")
        assertEquals(GuideGrab(vertical = true, index = null), grab(Offset(10f, 250f), reach = 5f, rulerReach = 0f), "the left ruler places a vertical guide")
        assertNull(grab(Offset(300f, 28f), reach = 5f, rulerReach = 0f), "the mouse must be on the ruler")
        assertEquals(GuideGrab(vertical = false, index = null), grab(Offset(300f, 28f), reach = 14f, rulerReach = 14f), "a finger just below the ruler")
        assertNull(grab(Offset(10f, 10f), reach = 14f, rulerReach = 14f), "the corner square places nothing")
        assertNull(grab(Offset(204f, 250f), reach = 5f, rulerReach = 0f, blocked = listOf(Rect(190f, 240f, 260f, 280f))), "the viewport's own controls come first")
    }

    @Test
    fun aSecondFingerTurnsAHandleDragIntoAPinch() = runComposeUiTest {
        val camera = ViewportCamera()
        var box by mutableStateOf(BoxTransform(Rect(100f, 100f, 200f, 160f)))
        setStretchContent {
            Box(Modifier.size(400.dp, 300.dp)) {
                Viewport(Modifier.testTag("view"), camera = camera) {
                    Box(Modifier.fillMaxSize().marquee(camera) { _, _ -> }.transformHandles(box, camera, { box = it }))
                }
            }
        }
        onNodeWithTag("view").performTouchInput {
            down(0, Offset(150f, 130f))
            moveTo(0, Offset(160f, 130f))
            down(1, Offset(260f, 130f))
            repeat(8) {
                updatePointerBy(0, Offset(-5f, 0f))
                updatePointerBy(1, Offset(5f, 0f))
                move()
            }
            up(0)
            up(1)
        }
        assertEquals(Rect(100f, 100f, 200f, 160f), box.bounds, "the box went back when the pinch began")
        assertTrue(camera.zoom > 1.2f, "and the view zoomed: ${camera.zoom}")
    }
}
