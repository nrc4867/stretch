package dev.chieppa.stretch.gpu.readback

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class FrameHandoffTest {
    private class Made(val frame: ReadbackFrame, val image: Image)

    private fun FrameHandoff.make(): Made {
        val (bitmap, image) = bgraFrameImage(1, 1, 256) {}
        return Made(ReadbackFrame(image, bitmap, nextSerial()), image)
    }

    @Test
    fun countsFramesNotDrawnAndTellsOfEachFirstDrawOnce() {
        var firstDraws = 0
        val handoff = FrameHandoff { firstDraws++ }
        val a = handoff.make().also { handoff.deliver(it.frame) }
        val b = handoff.make().also { handoff.deliver(it.frame) }
        assertEquals(2, handoff.undrawn)
        assertSame(b.frame, handoff.latest.value)

        handoff.drawn(b.frame)
        handoff.drawn(b.frame)
        assertEquals(0, handoff.undrawn)
        assertEquals(1, firstDraws)
        assertFalse(a.image.isClosed)
        handoff.closeAll()
    }

    @Test
    fun retiresOnlyFramesOlderThanTheLastDrawnAndKeepsTheNewest() {
        val handoff = FrameHandoff {}
        val a = handoff.make().also { handoff.deliver(it.frame) }
        val b = handoff.make().also { handoff.deliver(it.frame) }
        val c = handoff.make().also { handoff.deliver(it.frame) }

        handoff.retire()
        assertFalse(a.image.isClosed)

        handoff.drawn(b.frame)
        handoff.retire()
        assertTrue(a.image.isClosed)
        assertFalse(b.image.isClosed)
        assertFalse(c.image.isClosed)

        handoff.closeAll()
        assertTrue(b.image.isClosed && c.image.isClosed)
        assertNull(handoff.latest.value)
    }
}
