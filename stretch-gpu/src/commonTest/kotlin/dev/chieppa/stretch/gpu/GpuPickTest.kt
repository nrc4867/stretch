package dev.chieppa.stretch.gpu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GpuPickTest {
    private fun put(bytes: ByteArray, offset: Int, value: Int) {
        for (i in 0..3) bytes[offset + i] = (value ushr (8 * i)).toByte()
    }

    @Test
    fun theIdAndDepthSitACopyRowApart() {
        assertEquals(0, GpuPick.ID_OFFSET)
        assertEquals(256, GpuPick.DEPTH_OFFSET, "a texture copy into a buffer starts on a multiple of 256 bytes")
        assertEquals(512, GpuPick.BUFFER_BYTES)
    }

    @Test
    fun aPickDecodesFromLittleEndianTexels() {
        val bytes = ByteArray(GpuPick.BUFFER_BYTES)
        put(bytes, GpuPick.ID_OFFSET, 0x01020304)
        put(bytes, GpuPick.DEPTH_OFFSET, 0.75f.toRawBits())
        assertEquals(GpuPick(0x01020304, 0.75f), GpuPick.decode(bytes))

        put(bytes, GpuPick.ID_OFFSET, -2)
        assertEquals(-2, GpuPick.decode(bytes)?.id, "IDs use all 32 bits")
    }

    @Test
    fun emptySpaceIsNoPick() {
        assertNull(GpuPick.decode(ByteArray(GpuPick.BUFFER_BYTES)))
    }

    @Test
    fun aPickMatrixFillsTheViewWithOnePixel() {
        val camera = CameraView(eye = Vec3(1f, 2f, 5f), target = Vec3.Zero)
        val point = Vec3(0.3f, -0.2f, 0.4f)
        val pixel = camera.project(point, 640, 480)!!
        val x = pixel.x.toInt()
        val y = pixel.y.toInt()
        val zoomed = pickMatrix(x, y, 640, 480) * camera.viewProjection(640, 480)
        val (cx, cy, cz, cw) = zoomed.transform(point)
        // The point's place inside its pixel, -1..1 across it.
        assertEquals((pixel.x - x - 0.5f) * 2f, cx / cw, 1e-2f)
        assertEquals(-(pixel.y - y - 0.5f) * 2f, cy / cw, 1e-2f)
        val plain = camera.viewProjection(640, 480).transform(point)
        assertEquals(plain[2] / plain[3], cz / cw, 1e-6f, "depth is left alone")
    }
}
