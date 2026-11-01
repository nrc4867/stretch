package dev.chieppa.stretch.gpu

import kotlin.test.Test
import kotlin.test.assertEquals

class ReadbackTest {
    @Test
    fun rowsArePaddedToTheCopyAlignment() {
        assertEquals(256, paddedRowBytes(1))
        assertEquals(256, paddedRowBytes(64))
        assertEquals(512, paddedRowBytes(65))
    }

    @Test
    fun paddingIsSkippedAndChannelsReordered() {
        val rowBytes = paddedRowBytes(2)
        val bytes = ByteArray(rowBytes * 2)
        // Two pixels per row, in blue, green, red, alpha order.
        fun put(x: Int, y: Int, b: Int, g: Int, r: Int, a: Int) {
            val i = y * rowBytes + x * 4
            bytes[i] = b.toByte()
            bytes[i + 1] = g.toByte()
            bytes[i + 2] = r.toByte()
            bytes[i + 3] = a.toByte()
        }
        put(0, 0, 0x10, 0x20, 0x30, 0xFF)
        put(1, 1, 0xC0, 0x80, 0x40, 0x7F)
        val bgra = GpuImage(2, 2, argbPixels(bytes, 2, 2, rowBytes, bgra = true))
        assertEquals(0xFF302010.toInt(), bgra[0, 0])
        assertEquals(0x7F4080C0, bgra[1, 1])
        val rgba = GpuImage(2, 2, argbPixels(bytes, 2, 2, rowBytes, bgra = false))
        assertEquals(0xFF102030.toInt(), rgba[0, 0])
    }
}
