package dev.chieppa.stretch.gpu

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CameraTest {
    private fun assertNear(expected: Float, actual: Float, tolerance: Float = 1e-4f) =
        assertTrue(abs(expected - actual) <= tolerance, "expected $expected, was $actual")

    private fun assertNear(expected: Offset, actual: Offset?, tolerance: Float = 0.01f) {
        assertNotNull(actual)
        assertNear(expected.x, actual.x, tolerance)
        assertNear(expected.y, actual.y, tolerance)
    }

    @Test
    fun matricesComposeInOrder() {
        val move = Mat4.translation(Vec3(1f, 2f, 3f))
        val grow = Mat4.scale(2f)
        val point = (move * grow).transform(Vec3(1f, 1f, 1f))
        assertEquals(listOf(3f, 4f, 5f, 1f), point.toList())
        assertEquals(move, Mat4.Identity * move)
    }

    @Test
    fun rotationTurnsCounterClockwiseAroundItsAxis() {
        val turned = Mat4.rotation(Vec3.Up, (PI / 2).toFloat()).transform(Vec3(1f, 0f, 0f))
        assertNear(0f, turned[0])
        assertNear(-1f, turned[2])
    }

    @Test
    fun projectionMapsNearToZeroAndFarToOne() {
        val projection = Mat4.perspective(1f, 1.5f, 0.5f, 50f)
        val near = projection.transform(Vec3(0f, 0f, -0.5f))
        val far = projection.transform(Vec3(0f, 0f, -50f))
        assertNear(0f, near[2] / near[3])
        assertNear(1f, far[2] / far[3])
    }

    @Test
    fun orthographicMapsItsBoxToClipSpaceWithoutPerspective() {
        val lens = Mat4.orthographic(2f, 4f, near = 1f, far = 11f)
        val corner = lens.project(Vec3(2f, -4f, -1f))!!
        assertNear(1f, corner.x)
        assertNear(-1f, corner.y)
        assertNear(0f, corner.z)
        val far = lens.project(Vec3(1f, 2f, -11f))!!
        assertNear(0.5f, far.x)
        assertNear(0.5f, far.y)
        assertNear(1f, far.z)
    }

    @Test
    fun theTargetProjectsToTheMiddleOfTheView() {
        val camera = CameraView(eye = Vec3(3f, 2f, 5f), target = Vec3(0f, 1f, 0f))
        assertNear(Offset(400f, 300f), camera.project(Vec3(0f, 1f, 0f), 800, 600))
    }

    @Test
    fun pointsAboveAndRightLandAboveAndRight() {
        val camera = CameraView(eye = Vec3(0f, 0f, 5f), target = Vec3.Zero)
        val up = camera.project(Vec3(0f, 1f, 0f), 800, 600)!!
        val right = camera.project(Vec3(1f, 0f, 0f), 800, 600)!!
        assertTrue(up.y < 300f)
        assertTrue(right.x > 400f)
    }

    @Test
    fun pointsBehindTheCameraHaveNoScreenPosition() {
        val camera = CameraView(eye = Vec3(0f, 0f, 5f), target = Vec3.Zero)
        assertNull(camera.project(Vec3(0f, 0f, 10f), 800, 600))
    }

    @Test
    fun orbitingKeepsTheDistanceAndStopsShortOfThePoles() {
        val camera = OrbitCamera(target = Vec3(1f, 0f, 0f), distance = 4f)
        camera.orbitBy(120f, 10_000f)
        assertNear(4f, (camera.eye - camera.target).length)
        assertTrue(camera.pitch < (PI / 2).toFloat())
    }

    @Test
    fun panningMovesTheTargetAcrossTheView() {
        val camera = OrbitCamera(yaw = 0f, pitch = 0f, distance = 5f)
        camera.panBy(-100f, 0f, viewHeight = 600f)
        assertTrue(camera.target.x > 0f)
        assertNear(0f, camera.target.y)
        assertNear(0f, camera.target.z)
        assertNear(5f, (camera.eye - camera.target).length)
    }

    @Test
    fun dollyingStaysInRange() {
        val camera = OrbitCamera(distance = 5f, distanceRange = 1f..10f)
        camera.dollyBy(100f)
        assertEquals(1f, camera.distance)
        camera.dollyBy(0.001f)
        assertEquals(10f, camera.distance)
    }
}
