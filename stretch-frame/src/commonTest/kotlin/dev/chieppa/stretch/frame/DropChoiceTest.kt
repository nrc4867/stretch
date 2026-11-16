package dev.chieppa.stretch.frame

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DropChoiceTest {
    private val outer = Rect(0f, 0f, 400f, 400f)
    private val inner = Rect(100f, 100f, 200f, 200f)

    @Test
    fun theSmallestTargetUnderThePointerWins() {
        assertEquals(1, innermostAt(listOf(outer, inner), Offset(150f, 150f)) { true })
        assertEquals(0, innermostAt(listOf(inner, outer), Offset(150f, 150f)) { true })
    }

    @Test
    fun aTargetThatRefusesTheItemLetsTheOneAroundItTakeIt() {
        assertEquals(0, innermostAt(listOf(outer, inner), Offset(150f, 150f)) { it == 0 })
    }

    @Test
    fun ofEqualTargetsTheLastWins() {
        assertEquals(1, innermostAt(listOf(inner, inner), Offset(150f, 150f)) { true })
    }

    @Test
    fun noTargetUnderThePointerIsNone() {
        assertNull(innermostAt(listOf(inner), Offset(50f, 50f)) { true })
    }

    @Test
    fun thePreviewKeepsThePointerWhereItWasGrabbed() {
        assertEquals(Offset(90f, 180f), previewTopLeft(Offset(100f, 200f), Offset(0.25f, 0.5f), Size(40f, 40f)))
    }
}
