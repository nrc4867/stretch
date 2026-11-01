package dev.chieppa.stretch.gpu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.test.setStretchContent
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class OrbitNavigationTest {
    private val centre = Offset(200f, 150f)

    private fun near(a: Float, b: Float, slack: Float = 1e-3f) = abs(a - b) <= slack

    private fun androidx.compose.ui.test.ComposeUiTest.orbitView(camera: OrbitCamera) =
        setStretchContent { Box(Modifier.size(400.dp, 300.dp).testTag("view").orbitNavigation(camera)) }

    /** Two fingers [radius] pixels either side of [centre], at [degrees] clockwise from horizontal. */
    private fun TouchInjectionScope.fingersAt(radius: Float, degrees: Float, centre: Offset = this@OrbitNavigationTest.centre): Pair<Offset, Offset> {
        val r = degrees * PI.toFloat() / 180f
        val d = Offset(cos(r), sin(r)) * radius
        return centre - d to centre + d
    }

    private fun TouchInjectionScope.twoFingers(steps: Int, at: (step: Int) -> Pair<Offset, Offset>) {
        val (a, b) = at(0)
        down(0, a)
        down(1, b)
        for (step in 1..steps) {
            val (p, q) = at(step)
            updatePointerTo(0, p)
            updatePointerTo(1, q)
            move()
        }
        up(0)
        up(1)
    }

    @Test
    fun twoFingersMovingTogetherPan() = runComposeUiTest {
        val camera = OrbitCamera()
        orbitView(camera)
        val (yaw, pitch, distance) = Triple(camera.yaw, camera.pitch, camera.distance)
        onNodeWithTag("view").performTouchInput {
            twoFingers(10) { step -> fingersAt(50f, 0f, centre + Offset(0f, step * 4f)) }
        }
        assertTrue(camera.target != Vec3.Zero, "the target moved: ${camera.target}")
        assertTrue(camera.target.y > 0f, "the scene followed the fingers down, so the target rose: ${camera.target}")
        assertTrue(near(camera.yaw, yaw) && near(camera.pitch, pitch) && near(camera.distance, distance), "only the target moved")
    }

    @Test
    fun spreadingTwoFingersDolliesIn() = runComposeUiTest {
        val camera = OrbitCamera(distance = 6f)
        orbitView(camera)
        onNodeWithTag("view").performTouchInput { twoFingers(10) { step -> fingersAt(50f + step * 5f, 0f) } }
        assertTrue(near(camera.distance, 3f, 0.05f), "twice the spread is half the distance: ${camera.distance}")
        assertEquals(Vec3.Zero, camera.target)
    }

    @Test
    fun twistingTurnsOnlyTheYaw() = runComposeUiTest {
        val camera = OrbitCamera()
        orbitView(camera)
        val (yaw, pitch, distance) = Triple(camera.yaw, camera.pitch, camera.distance)
        onNodeWithTag("view").performTouchInput { twoFingers(9) { step -> fingersAt(80f, step * 10f) } }
        assertTrue(near(camera.yaw - yaw, (PI / 2).toFloat(), 0.01f), "a quarter turn clockwise: ${camera.yaw - yaw}")
        assertTrue(near(camera.pitch, pitch), "the pitch stays: ${camera.pitch}")
        assertTrue(near(camera.distance, distance, 0.01f), "the distance stays: ${camera.distance}")
        assertEquals(Vec3.Zero, camera.target)
    }

    @Test
    fun oneFingerLeavesTheCameraAlone() = runComposeUiTest {
        val camera = OrbitCamera()
        orbitView(camera)
        val before = camera.view
        onNodeWithTag("view").performTouchInput { swipe(Offset(100f, 100f), Offset(250f, 180f)) }
        assertEquals(before, camera.view)
    }

    @Test
    fun theWheelDolliesWithOrWithoutCtrl() = runComposeUiTest {
        val camera = OrbitCamera(distance = 6f)
        orbitView(camera)
        onNodeWithTag("view").performMouseInput { scroll(-2f) }
        val plain = camera.distance
        assertTrue(plain < 6f, "scrolling up moves in: $plain")
        onNodeWithTag("view").performKeyInput { keyDown(Key.CtrlLeft) }
        onNodeWithTag("view").performMouseInput { scroll(-2f) }
        onNodeWithTag("view").performKeyInput { keyUp(Key.CtrlLeft) }
        assertTrue(camera.distance < plain, "Ctrl with the wheel, as a touchpad pinch sends it, moves in too: ${camera.distance}")
    }
}
