package dev.chieppa.stretch.gpu

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RayTest {
    private fun assertNear(expected: Float, actual: Float?, tolerance: Float = 1e-3f) {
        assertNotNull(actual)
        assertTrue(abs(expected - actual) <= tolerance, "expected $expected, was $actual")
    }

    private fun assertNear(expected: Vec3, actual: Vec3?, tolerance: Float = 1e-3f) {
        assertNotNull(actual)
        assertTrue((expected - actual).length <= tolerance, "expected $expected, was $actual")
    }

    private fun assertIdentity(m: Mat4) {
        for (column in 0..3) for (row in 0..3) assertNear(if (column == row) 1f else 0f, m[column, row])
    }

    @Test
    fun aMatrixTimesItsInverseIsTheIdentity() {
        val camera = CameraView(eye = Vec3(3f, 2f, 5f), target = Vec3(0f, 1f, 0f))
        val matrices = listOf(
            Mat4.translation(Vec3(1f, -2f, 3f)) * Mat4.rotation(Vec3(1f, 1f, 0f), 0.7f) * Mat4.scale(2.5f),
            camera.viewProjection(800, 600),
            Mat4.Identity,
        )
        for (m in matrices) {
            val inverse = assertNotNull(m.inverse())
            assertIdentity(m * inverse)
            assertIdentity(inverse * m)
        }
    }

    @Test
    fun aFlatMatrixHasNoInverse() {
        assertNull(Mat4.scale(0f).inverse())
    }

    @Test
    fun aRayThroughTheMiddleRunsFromTheEyeToTheTarget() {
        val camera = CameraView(eye = Vec3(3f, 2f, 5f), target = Vec3(0f, 1f, 0f))
        val ray = camera.ray(Offset(400f, 300f), 800, 600)
        assertEquals(camera.eye, ray.origin)
        assertNear((camera.target - camera.eye).normalized(), ray.direction)
        assertNear(1f, ray.direction.length)
    }

    @Test
    fun projectAndRayAgree() {
        val camera = CameraView(eye = Vec3(-2f, 4f, 6f), target = Vec3(0.5f, 0f, 0f))
        for (point in listOf(Vec3(1f, 0.5f, -0.3f), Vec3(-1.2f, 2f, 0.8f), Vec3(0f, -1f, 1f))) {
            val pixel = assertNotNull(camera.project(point, 640, 480))
            val ray = camera.ray(pixel, 640, 480)
            val t = (point - ray.origin).length
            assertNear(point, ray.at(t), 1e-2f)
        }
    }

    @Test
    fun depthUnprojectsToTheWorld() {
        val camera = CameraView(eye = Vec3(0f, 0f, 5f), target = Vec3.Zero, near = 0.5f, far = 50f)
        val point = Vec3(0.4f, -0.3f, 1f)
        val clip = camera.viewProjection(640, 480).transform(point)
        val pixel = assertNotNull(camera.project(point, 640, 480))
        assertNear(point, camera.unproject(pixel, clip[2] / clip[3], 640, 480), 1e-2f)
    }

    private val forward = Ray(Vec3(0f, 0f, 5f), Vec3(0f, 0f, -1f))

    @Test
    fun planes() {
        assertNear(Vec3(0f, 0f, 1f), forward.intersectPlane(Vec3(0f, 0f, 1f), Vec3(0f, 0f, 1f)))
        assertNull(forward.intersectPlane(Vec3(0f, 0f, 9f), Vec3(0f, 0f, 1f)), "behind the origin")
        assertNull(forward.intersectPlane(Vec3.Zero, Vec3(1f, 0f, 0f)), "parallel")
    }

    @Test
    fun spheres() {
        assertNear(4f, forward.intersectSphere(Vec3.Zero, 1f))
        assertNull(forward.intersectSphere(Vec3(3f, 0f, 0f), 1f), "a miss")
        assertNull(forward.intersectSphere(Vec3(0f, 0f, 9f), 1f), "behind the origin")
        assertNear(1f, Ray(Vec3.Zero, Vec3(0f, 0f, -1f)).intersectSphere(Vec3.Zero, 1f), message = "from inside, where it leaves")
    }

    private fun assertNear(expected: Float, actual: Float?, message: String) {
        assertNotNull(actual, message)
        assertTrue(abs(expected - actual) <= 1e-3f, "$message: expected $expected, was $actual")
    }

    @Test
    fun triangles() {
        val a = Vec3(-1f, -1f, 0f)
        val b = Vec3(1f, -1f, 0f)
        val c = Vec3(0f, 1f, 0f)
        assertNear(5f, forward.intersectTriangle(a, b, c))
        assertNear(5f, forward.intersectTriangle(a, c, b), message = "from the back too")
        assertNull(Ray(Vec3(2f, 0f, 5f), Vec3(0f, 0f, -1f)).intersectTriangle(a, b, c), "a miss beside it")
        assertNull(Ray(Vec3(0f, 0f, -5f), Vec3(0f, 0f, -1f)).intersectTriangle(a, b, c), "behind the origin")
        assertNull(Ray(Vec3(0f, 0f, 5f), Vec3(1f, 0f, 0f)).intersectTriangle(a, b, c), "parallel")
    }

    @Test
    fun boxes() {
        val low = Vec3(-0.5f, -0.5f, -0.5f)
        val high = Vec3(0.5f, 0.5f, 0.5f)
        assertNear(4.5f, forward.intersectBox(low, high))
        assertNull(Ray(Vec3(1f, 0f, 5f), Vec3(0f, 0f, -1f)).intersectBox(low, high), "a miss beside it")
        assertNull(Ray(Vec3(0f, 0f, 5f), Vec3(0f, 0f, 1f)).intersectBox(low, high), "pointing away")
        assertNear(0.5f, Ray(Vec3.Zero, Vec3(1f, 0f, 0f)).intersectBox(low, high), message = "from inside, where it leaves")
        assertNear(5f * kotlin.math.sqrt(2f) - 0.5f * kotlin.math.sqrt(2f), Ray(Vec3(5f, 5f, 0f), Vec3(-1f, -1f, 0f).normalized()).intersectBox(low, high), 1e-2f)
    }
}
