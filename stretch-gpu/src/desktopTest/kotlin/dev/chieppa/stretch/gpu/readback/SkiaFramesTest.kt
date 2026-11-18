package dev.chieppa.stretch.gpu.readback

import dev.chieppa.stretch.gpu.paddedRowBytes
import java.lang.foreign.MemorySegment
import kotlin.test.Test
import kotlin.test.assertEquals

class SkiaFramesTest {
    @Test
    fun aFrameWhoseRowsArePaddedGivesFillOnlyTheBytesSkiaAllocated() {
        // 333 pixels is 1332 bytes a row, padded to 1536 for copies: the last row has no padding after it.
        val width = 333
        val height = 3
        val rowBytes = paddedRowBytes(width)
        var filled: MemorySegment? = null
        val (bitmap, image) = bgraFrameImage(width, height, rowBytes) { filled = it }
        image.close()
        bitmap.close()
        assertEquals(rowBytes.toLong() * (height - 1) + width * 4L, filled?.byteSize())
    }

    @Test
    fun theLastRowOfAFrameEndsAtItsLastPixel() {
        assertEquals(1536L * 2 + 1332, frameByteSize(333, 3, 1536))
        assertEquals(1024L, frameByteSize(256, 1, 1024))
    }
}
