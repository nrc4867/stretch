package dev.chieppa.stretch.frame

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FrameLayoutTest {
    private val layout = FrameLayout(
        listOf(
            DrawerPlacement("layers", Side.Left, open = true),
            DrawerPlacement("library", Side.Left),
            DrawerPlacement("brush", Side.Right, open = true),
        ),
    )

    @Test
    fun toggleOpensAndCloses() {
        val toggled = layout.toggle("library")
        assertEquals(listOf("layers", "library"), toggled.openOn(Side.Left).map { it.id })
        assertEquals(listOf("layers"), toggled.toggle("library").openOn(Side.Left).map { it.id })
    }

    @Test
    fun widthIsClamped() {
        assertEquals(FrameLayout.MIN_WIDTH, layout.resize("layers", 10f)["layers"]!!.width)
        assertEquals(FrameLayout.MAX_WIDTH, layout.resize("layers", 9000f)["layers"]!!.width)
        assertEquals(333f, layout.resize("layers", 333f)["layers"]!!.width)
    }

    @Test
    fun dockingMovesToTheEndOfTheOtherSide() {
        val moved = layout.dock("layers", Side.Right)
        assertEquals(listOf("brush", "layers"), moved.on(Side.Right).map { it.id })
        assertTrue(moved["layers"]!!.open)
        assertEquals(listOf("library"), moved.on(Side.Left).map { it.id })
    }

    @Test
    fun unknownIdsChangeNothing() {
        assertEquals(layout, layout.toggle("nope").dock("nope", Side.Right).resize("nope", 300f))
    }

    @Test
    fun duplicateIdsAreRefused() {
        assertFailsWith<IllegalArgumentException> {
            FrameLayout(listOf(DrawerPlacement("a", Side.Left), DrawerPlacement("a", Side.Right)))
        }
    }

    @Test
    fun modesRememberTheirOwnLayoutUntilReset() {
        val modes = ModeLayouts(mapOf("build" to layout, "pose" to layout))
        val changed = modes.update("build") { it.close("layers") }
        assertEquals(emptyList(), changed["build"].openOn(Side.Left))
        assertEquals(layout, changed["pose"])
        assertEquals(layout, changed.reset("build")["build"])
    }

    @Test
    fun searchRanksPrefixThenWordThenContainsThenLetters() {
        val labels = listOf("Smooth", "Mirror tool", "Dock · mirror", "Make it round", "Admiral", "Mirror to other side")
        assertEquals(
            listOf("Mirror tool", "Mirror to other side", "Dock · mirror", "Admiral", "Make it round"),
            searchLabels("mir", labels) { it },
        )
        assertEquals(listOf("Mirror tool", "Mirror to other side"), searchLabels("mirror t", labels) { it })
        assertEquals(labels, searchLabels("  ", labels) { it })
    }
}
