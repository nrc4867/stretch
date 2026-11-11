package dev.chieppa.stretch.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KeymapTest {
    private fun action(id: String, keys: String?, mode: String? = null, available: Boolean = true) = Action(
        id = ActionId(id),
        textKey = id,
        keys = listOfNotNull(keys?.let(KeyChord::parse)),
        mode = mode,
        isAvailable = { available },
        run = {},
    )

    @Test
    fun parsesAndPrintsChords() {
        val chord = KeyChord.parse("ctrl+shift+z")
        assertEquals(KeyChord("Z", ctrl = true, shift = true), chord)
        assertEquals("Ctrl Shift Z", chord.toString())
        assertEquals(chord, KeyChord.parse(chord.toString()))
        assertFailsWith<IllegalArgumentException> { KeyChord.parse("Hyper K") }
    }

    @Test
    fun modeActionWinsOverGlobal() {
        val registry = ActionRegistry().apply {
            register(action("mirror", "M"))
            register(action("measure", "M", mode = "pose"))
        }
        val keymap = Keymap(registry)
        assertEquals("measure", keymap.resolve(KeyChord("M"), "pose")?.id?.value)
        assertEquals("mirror", keymap.resolve(KeyChord("M"), "build")?.id?.value)
    }

    @Test
    fun anUnregisteredActionAndItsKeysAreGoneAndItsIdFree() {
        val registry = ActionRegistry().apply { register(action("run", "R")) }
        val keymap = Keymap(registry)
        assertEquals("run", registry.unregister(ActionId("run"))?.id?.value)
        assertNull(keymap.resolve(KeyChord("R"), null))
        assertNull(registry.unregister(ActionId("run")))
        registry.register(action("run", "T"))
        assertEquals("run", keymap.resolve(KeyChord("T"), null)?.id?.value)
    }

    @Test
    fun unavailableActionsDoNotResolve() {
        val registry = ActionRegistry().apply { register(action("export", "Ctrl E", available = false)) }
        assertNull(Keymap(registry).resolve(KeyChord("E", ctrl = true), null))
    }

    @Test
    fun conflictsAreFoundWithinOverlappingModes() {
        val registry = ActionRegistry().apply {
            register(action("draw", "B", mode = "build"))
            register(action("bend", "B", mode = "pose"))
            register(action("brush", "B"))
        }
        val conflicts = Keymap(registry).conflicts()
        assertEquals(2, conflicts.size)
        assertTrue(conflicts.all { ActionId("brush") in listOf(it.first, it.second) })
    }

    @Test
    fun rebindingReplacesDefaults() {
        val registry = ActionRegistry().apply { register(action("undo", "Ctrl Z")) }
        val keymap = Keymap(registry)
        keymap.rebind(ActionId("undo"), listOf(KeyChord.parse("Ctrl U")))
        assertNull(keymap.resolve(KeyChord("Z", ctrl = true), null))
        assertEquals("undo", keymap.resolve(KeyChord("U", ctrl = true), null)?.id?.value)
        keymap.reset(ActionId("undo"))
        assertEquals("undo", keymap.resolve(KeyChord("Z", ctrl = true), null)?.id?.value)
    }

    @Test
    fun duplicateIdsAreRefused() {
        val registry = ActionRegistry().apply { register(action("a", null)) }
        assertFailsWith<IllegalArgumentException> { registry.register(action("a", null)) }
    }
}
