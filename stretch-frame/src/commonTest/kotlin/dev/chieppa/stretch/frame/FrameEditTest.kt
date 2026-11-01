package dev.chieppa.stretch.frame

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.test.setStretchContent
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.ui.Button
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class FrameEditTest {
    private val drawers = listOf(
        DrawerSpec("layers", "Layers", StretchIcons.Layers) { RowText("Body") },
        DrawerSpec("library", "Library", StretchIcons.Grid) { RowText("Shapes") },
        DrawerSpec("help", "Help", StretchIcons.Help) { RowText(if (it == DrawerPosition.Bottom) "Guides, wide" else "Guides, tall") },
        DrawerSpec("timeline", "Timeline", StretchIcons.Keyframe) { RowText("Frames") },
    )

    private fun start() = FrameLayout(
        listOf(
            DrawerPlacement("layers", Side.Left, open = true),
            DrawerPlacement("library", Side.Left, open = true),
            DrawerPlacement("help", Side.Left, bottom = true),
        ),
    )

    @Test
    fun movingKeepsGroupsInOrder() {
        val layout = start()
        assertEquals(listOf("library", "layers"), layout.move("layers", Side.Left, false, 1).group(Side.Left, false).map { it.id })
        val down = layout.move("layers", Side.Left, true, 0)
        assertEquals(listOf("layers", "help"), down.group(Side.Left, true).map { it.id })
        assertEquals(listOf("library"), down.openOn(Side.Left).map { it.id })
        assertEquals("layers", down.openBottom(Side.Left)?.id)
        assertEquals(listOf("layers"), layout.move("layers", Side.Right, true, 5).group(Side.Right, true).map { it.id })
    }

    @Test
    fun splitsShareTheColumnWithinLimits() {
        val layout = start().resizeSplit("layers", "library", 0.25f)
        assertEquals(1.5f, layout["layers"]!!.height, 0.001f)
        assertEquals(0.5f, layout["library"]!!.height, 0.001f)
        val squeezed = start().resizeSplit("layers", "library", 5f)
        assertEquals(2f * (1 - FrameLayout.MIN_SHARE), squeezed["layers"]!!.height, 0.001f)
    }

    @Test
    fun bottomDrawersOpenOneAtATimePerSide() {
        val layout = FrameLayout(
            listOf(
                DrawerPlacement("help", Side.Left, bottom = true, open = true),
                DrawerPlacement("guides", Side.Left, bottom = true),
                DrawerPlacement("timeline", Side.Right, bottom = true, open = true),
            ),
        )
        val swapped = layout.open("guides")
        assertEquals("guides", swapped.openBottom(Side.Left)?.id)
        assertEquals(false, swapped["help"]!!.open)
        assertEquals("timeline", swapped.openBottom(Side.Right)?.id)
        assertEquals("timeline", layout.move("timeline", Side.Left, true, 0).openBottom(Side.Left)?.id)
        assertEquals(FrameLayout.MAX_BOTTOM_HEIGHT, layout.resizeBottom(9000f).bottomHeight)
        assertEquals(1 - FrameLayout.MIN_BOTTOM_SHARE, layout.splitBottom(0.99f).bottomSplit)
        assertEquals(DrawerPosition.Bottom, layout["help"]!!.position)
    }

    @Test
    fun bottomPanelSpansOrSplitsAndTellsDrawersWhereTheyAre() = runComposeUiTest {
        var layout by mutableStateOf(
            FrameLayout(
                listOf(
                    DrawerPlacement("layers", Side.Left),
                    DrawerPlacement("help", Side.Left, bottom = true, open = true),
                    DrawerPlacement("timeline", Side.Right, bottom = true),
                ),
            ),
        )
        setStretchContent {
            StretchApp(layout, { layout = it }, drawers, onResetLayout = {}, topBar = { TopBar("App", "Untitled") }) { Viewport {} }
        }
        onNodeWithText("Guides, wide").assertExists()
        onNodeWithContentDescription("Resize split").assertDoesNotExist()
        onNodeWithContentDescription("Timeline").performClick()
        onNodeWithText("Frames").assertExists()
        onNodeWithContentDescription("Resize split").assertExists()
        onNodeWithContentDescription("Resize bottom panel").performTouchInput {
            down(center)
            repeat(10) { moveBy(Offset(0f, -6f)) }
            up()
        }
        assertTrue(layout.bottomHeight > FrameLayout.DEFAULT_BOTTOM_HEIGHT + 30f, "Height ${layout.bottomHeight}")
        layout = layout.move("help", Side.Left, false, 0)
        onNodeWithText("Guides, tall").assertExists()
    }

    @Test
    fun railDropsLandInTheNearestSlot() {
        assertEquals(RailDrop(false, 0), railDrop(0f, 600f, 3, 1, 44f, 8f))
        assertEquals(RailDrop(false, 2), railDrop(98f, 600f, 3, 1, 44f, 8f))
        assertEquals(RailDrop(true, 1), railDrop(580f, 600f, 3, 1, 44f, 8f))
        assertEquals(RailDrop(true, 0), railDrop(520f, 600f, 3, 1, 44f, 8f))
    }

    @Test
    fun railButtonsMoveWithAltArrowsAndTheMenu() = runComposeUiTest {
        var layout by mutableStateOf(start())
        setStretchContent {
            StretchApp(layout, { layout = it }, drawers, onResetLayout = {}, topBar = { TopBar("App", "Untitled") }) { Viewport {} }
        }
        onNodeWithContentDescription("Layers").requestFocus()
        onNodeWithContentDescription("Layers").performKeyInput { withKeyDown(Key.AltLeft) { pressKey(Key.DirectionDown) } }
        assertEquals(listOf("library", "layers"), layout.group(Side.Left, false).map { it.id })
        onAllNodesWithContentDescription("Drawer menu").onFirst().performClick()
        onNodeWithText("Dock to bottom").performClick()
        assertEquals(listOf("help", "library"), layout.group(Side.Left, true).map { it.id })
    }

    @Test
    fun draggingTheEdgeResizesByTheWholeDrag() = runComposeUiTest {
        var layout by mutableStateOf(FrameLayout(listOf(DrawerPlacement("layers", Side.Left, open = true))))
        setStretchContent {
            StretchApp(layout, { layout = it }, drawers, onResetLayout = {}, topBar = { TopBar("App", "Untitled") }) { Viewport {} }
        }
        onNodeWithContentDescription("Resize").performTouchInput {
            down(center)
            repeat(20) { moveBy(Offset(5f, 0f)) }
            up()
        }
        assertTrue(layout["layers"]!!.width > 360f, "Width ${layout["layers"]!!.width}")
    }

    @Test
    fun draggingTheSplitSharesTheColumn() = runComposeUiTest {
        var layout by mutableStateOf(start())
        setStretchContent {
            StretchApp(layout, { layout = it }, drawers, onResetLayout = {}, topBar = { TopBar("App", "Untitled") }) { Viewport {} }
        }
        onNodeWithContentDescription("Resize split").performTouchInput {
            down(center)
            repeat(20) { moveBy(Offset(0f, 5f)) }
            up()
        }
        assertTrue(layout["layers"]!!.height > 1.1f, "Height ${layout["layers"]!!.height}")
        assertEquals(2f, layout["layers"]!!.height + layout["library"]!!.height, 0.001f)
    }

    @Test
    fun keyCastShowsPressesAndCountsRepeats() = runComposeUiTest {
        val cast = KeyCastState()
        setStretchContent {
            val focus = remember { FocusRequester() }
            Box(Modifier.keyCast(cast)) {
                Box(Modifier.size(40.dp).testTag("target").focusRequester(focus).focusable())
                KeyCast(cast)
            }
        }
        onNodeWithTag("target").requestFocus()
        onNodeWithTag("target").performKeyInput {
            withKeyDown(Key.CtrlLeft) { pressKey(Key.Z) }
            withKeyDown(Key.CtrlLeft) { pressKey(Key.Z) }
            pressKey(Key.B)
        }
        assertEquals(listOf(KeyCastEntry(listOf("Ctrl", "Z"), 2), KeyCastEntry(listOf("B"))), cast.entries.toList())
        onNodeWithText("×2").assertExists()
    }

    @Test
    fun dialogsMoveByTheirHeader() = runComposeUiTest {
        setStretchContent { StretchDialog("Export", onDismiss = {}) { RowText("Format") } }
        val before = onNodeWithText("Format").fetchSemanticsNode().positionInRoot
        onNodeWithText("Export").performTouchInput {
            down(center)
            repeat(10) { moveBy(Offset(-8f, 6f)) }
            up()
        }
        val after = onNodeWithText("Format").fetchSemanticsNode().positionInRoot
        assertTrue(after.x < before.x - 40 && after.y > before.y + 30, "Moved from $before to $after")
    }

    @Test
    fun menusFitTheirItems() = runComposeUiTest {
        setStretchContent {
            Box {
                Menu(true, {}) {
                    MenuItem("Duplicate", {}, keys = "Ctrl D")
                    MenuDivider()
                    MenuItem("Lock", {})
                }
            }
        }
        val width = onNodeWithText("Duplicate").fetchSemanticsNode().layoutInfo.parentInfo!!.width
        assertTrue(width < 400, "Menu row width $width")
    }

    @Test
    fun popoverStaysOpenUntilEscape() = runComposeUiTest {
        var open by mutableStateOf(false)
        var clicks = 0
        setStretchContent {
            Box {
                Button("Brush", { open = true })
                Popover(open, { open = false }, title = "Brush settings") { Button("Apply", { clicks++ }) }
            }
        }
        onNodeWithText("Brush").performClick()
        onNodeWithText("Brush settings").assertExists()
        onNodeWithText("Apply").performClick()
        assertEquals(1, clicks)
        assertTrue(open)
        onNodeWithText("Apply").performKeyInput { pressKey(Key.Escape) }
        assertEquals(false, open)
    }
}
