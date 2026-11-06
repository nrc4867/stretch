package dev.chieppa.stretch.markdown

import kotlin.test.Test
import kotlin.test.assertEquals

class TableColumnsTest {
    @Test
    fun columnsThatFitShareTheRestByTheirWidth() {
        assertEquals(listOf(100f, 300f), columnWidths(listOf(ColumnExtent(20f, 50f), ColumnExtent(40f, 150f)), 400f))
    }

    @Test
    fun aLongColumnWrapsWhileShortOnesKeepTheirLine() {
        val widths = columnWidths(listOf(ColumnExtent(40f, 40f), ColumnExtent(60f, 900f), ColumnExtent(30f, 30f)), 400f)
        assertEquals(listOf(40f, 330f, 30f), widths)
    }

    @Test
    fun wordsTooWideForTheRoomKeepTheirWidth() {
        assertEquals(listOf(200f, 300f), columnWidths(listOf(ColumnExtent(200f, 400f), ColumnExtent(300f, 500f)), 400f))
    }
}
