package dev.chieppa.stretch.frame

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import dev.chieppa.stretch.test.setStretchContent
import dev.chieppa.stretch.theme.StretchIcons
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DrawerChangesTest {
    private val base = FrameLayout(listOf(DrawerPlacement("layers", Side.Left, open = true), DrawerPlacement("help", Side.Left, bottom = true)))

    @Test
    fun aDrawerAddedGoesAtTheEndOfItsGroup() {
        val added = base.addDrawer(DrawerPlacement("files", Side.Left, open = true))
        assertEquals(listOf("layers", "files", "help"), added.drawers.map { it.id })
        assertTrue(added["files"]!!.open)
        assertEquals(added, added.addDrawer(DrawerPlacement("files", Side.Right)))
    }

    @Test
    fun aDrawerThatComesBackReturnsWhereItWas() {
        val moved = base.addDrawer(DrawerPlacement("files", Side.Left)).move("files", Side.Right, bottom = false, index = 0).resize("files", 380f).open("files")
        val gone = moved.removeDrawer("files")
        assertNull(gone["files"])
        assertEquals(Side.Right, gone.removed["files"]?.side)
        val back = gone.addDrawer(DrawerPlacement("files", Side.Left))
        assertEquals(Side.Right, back["files"]!!.side)
        assertEquals(380f, back["files"]!!.width)
        assertTrue(back["files"]!!.open)
        assertTrue(back.removed.isEmpty())
    }

    @Test
    fun aDrawerInAWindowComesBackInItsWindow() {
        val bounds = WindowBounds(10f, 20f, 300f, 400f)
        val floating = base.addDrawer(DrawerPlacement("files", Side.Left)).popOut("files").moveWindow("files", bounds)
        val back = floating.removeDrawer("files").addDrawer(DrawerPlacement("files", Side.Left))
        assertTrue(back["files"]!!.detached)
        assertEquals(bounds, back["files"]!!.window)
    }

    @Test
    fun aBottomDrawerComingBackOpenClosesTheOneOpenThere() {
        val open = base.open("help")
        val withLog = open.addDrawer(DrawerPlacement("log", Side.Left, bottom = true, open = true))
        assertTrue(withLog["log"]!!.open)
        assertFalse(withLog["help"]!!.open)
    }

    @Test
    fun syncingKeepsExactlyTheOfferedDrawers() {
        val synced = base.syncDrawers(listOf("layers", "files")) { DrawerPlacement(it, Side.Right) }
        assertEquals(listOf("layers", "files"), synced.drawers.map { it.id })
        assertEquals(setOf("help"), synced.removed.keys)
        assertEquals(synced, synced.syncDrawers(listOf("layers", "files")) { DrawerPlacement(it, Side.Left) })
    }

    @Test
    fun modesAllGainAndLoseADrawer() {
        val modes = ModeLayouts(mapOf("build" to base, "pose" to base)).update("pose") { it.close("layers") }
        val added = modes.addDrawer(DrawerPlacement("files", Side.Right))
        assertTrue(added["build"]["files"] != null && added["pose"]["files"] != null)
        assertTrue(added.reset("pose")["pose"]["files"] != null)
        val removed = added.removeDrawer("files")
        assertNull(removed["build"]["files"])
        assertFalse(removed["pose"]["layers"]!!.open)
    }

    @Test
    fun theFrameFollowsTheDrawersTheAppOffers() = runComposeUiTest {
        var layout by mutableStateOf(FrameLayout(listOf(DrawerPlacement("layers", Side.Left))))
        var specs by mutableStateOf(listOf(DrawerSpec("layers", "Layers", StretchIcons.Layers) { RowText("Body") }))
        setStretchContent {
            StretchApp(
                layout, { layout = it }, specs, onResetLayout = {},
                placeNewDrawer = { DrawerPlacement(it.id, Side.Right, open = true) },
                topBar = { TopBar("App", "Untitled") },
            ) { Viewport {} }
        }
        onNodeWithContentDescription("Search").assertDoesNotExist()
        specs = specs + DrawerSpec("search", "Search", StretchIcons.Search) { RowText("Results") }
        waitForIdle()
        onNodeWithText("Results").assertExists()
        assertEquals(Side.Right, layout["search"]?.side)
        onNodeWithContentDescription("Search").performClick()
        onNodeWithText("Results").assertDoesNotExist()
        specs = specs.dropLast(1)
        waitForIdle()
        assertNull(layout["search"])
        assertFalse(layout.removed.getValue("search").open)
        specs = specs + DrawerSpec("search", "Search", StretchIcons.Search) { RowText("Results") }
        waitForIdle()
        // It comes back closed, as it was when it left.
        onNodeWithText("Results").assertDoesNotExist()
        onNodeWithContentDescription("Search").assertExists()
    }
}
