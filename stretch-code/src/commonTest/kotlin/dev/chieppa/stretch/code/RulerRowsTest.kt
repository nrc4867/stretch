package dev.chieppa.stretch.code

import kotlin.test.Test
import kotlin.test.assertEquals

class RulerRowsTest {
    @Test
    fun countsWrappedRows() {
        assertEquals(1, wrappedRows("", 10))
        assertEquals(1, wrappedRows("x".repeat(10), 10))
        assertEquals(2, wrappedRows("x".repeat(11), 10))
        assertEquals(2, wrappedRows("\t\t\tx", 10))
        assertEquals(1, wrappedRows("x".repeat(500), 0))
    }

    @Test
    fun placesItemsByTheirExtents() {
        val rows = RulerRows(floatArrayOf(1f, 3f, 1f))
        assertEquals(5f, rows.length)
        assertEquals(1f, rows.start(1))
        assertEquals(4f, rows.end(1))
        assertEquals(0, rows.indexAt(0.5f))
        assertEquals(1, rows.indexAt(1f))
        assertEquals(1, rows.indexAt(3.9f))
        assertEquals(2, rows.indexAt(4.5f))
        assertEquals(2, rows.indexAt(99f))
        assertEquals(0, RulerRows(FloatArray(0)).indexAt(3f))
    }
}
