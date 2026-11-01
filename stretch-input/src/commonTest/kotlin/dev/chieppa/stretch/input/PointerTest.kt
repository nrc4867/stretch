package dev.chieppa.stretch.input

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PointerTest {
    @Test
    fun axisMappingIsLinearBothWays() {
        val mapping = AxisMapping(0f, 30000f, 100f, 2020f)
        assertEquals(100f, mapping.map(0f))
        assertEquals(2020f, mapping.map(30000f))
        assertEquals(1060f, mapping.map(15000f))
        assertEquals(500f, AxisMapping(10f, 0f, 0f, 1000f).map(5f))
        assertFailsWith<IllegalArgumentException> { AxisMapping(5f, 5f, 0f, 1f) }
    }

    @Test
    fun penPhaseFollowsContact() {
        assertEquals(PointerPhase.Hover, penPhase(inContact = false, wasInContact = false))
        assertEquals(PointerPhase.Down, penPhase(inContact = true, wasInContact = false))
        assertEquals(PointerPhase.Move, penPhase(inContact = true, wasInContact = true))
        assertEquals(PointerPhase.Up, penPhase(inContact = false, wasInContact = true))
        assertEquals(PointerPhase.Leave, penPhase(inContact = false, wasInContact = true, left = true))
    }

    @Test
    fun drainEmptiesTheQueue() {
        val source = object : PenSource {
            val queue = ArrayDeque(listOf(1f, 2f, 3f))
            override fun attach() = Unit
            override fun close() = Unit
            override fun poll() = queue.removeFirstOrNull()?.let { PointerSample(it, it, PointerPhase.Move, PointerKind.Pen) }
        }
        assertEquals(listOf(1f, 2f, 3f), source.drain().map { it.x })
        assertEquals(emptyList(), source.drain())
    }
}
