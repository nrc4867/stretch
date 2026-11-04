package dev.chieppa.stretch.frame

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.core.Action
import dev.chieppa.stretch.core.ActionId
import dev.chieppa.stretch.core.ActionRegistry
import dev.chieppa.stretch.core.KeyChord
import dev.chieppa.stretch.core.Keymap
import dev.chieppa.stretch.test.setStretchContent
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.ui.Button
import dev.chieppa.stretch.ui.TextField
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class FrameUiTest {
    @Test
    fun viewportCornersStackInsteadOfOverlapping() = runComposeUiTest {
        var width by mutableStateOf(800.dp)
        setStretchContent {
            Box(Modifier.size(width, 400.dp)) {
                Viewport(
                    topStart = { Box(Modifier.size(200.dp, 40.dp).testTag("tools")) },
                    topEnd = { Box(Modifier.size(200.dp, 40.dp).testTag("view")) },
                    bottomStart = { Box(Modifier.size(200.dp, 20.dp).testTag("readout")) },
                    bottomEnd = { Box(Modifier.size(200.dp, 20.dp).testTag("status")) },
                ) {}
            }
        }
        fun top(tag: String) = onNodeWithTag(tag).getUnclippedBoundsInRoot().top
        assertEquals(top("tools"), top("view"), "side by side when there is room")
        assertEquals(top("readout"), top("status"))

        width = 380.dp
        waitForIdle()
        assertEquals(top("tools") + 48.dp, top("view"), "under the toolbar, 8dp apart")
        assertEquals(top("readout") - 28.dp, top("status"), "above the readout, 8dp apart")
    }

    @Test
    fun barMenusSitAfterTheAppNameAndRunTheirItems() = runComposeUiTest {
        var ran = 0
        setStretchContent {
            TopBar(
                "App",
                "Untitled",
                menus = {
                    BarMenus(
                        listOf(
                            BarMenu("File") { close -> MenuItem("Save", { ran++; close() }) },
                            BarMenu("Edit") { close -> MenuItem("Undo", close) },
                        ),
                    )
                },
            )
        }
        fun left(text: String) = onNodeWithText(text).getUnclippedBoundsInRoot().left
        assertTrue(left("App") < left("File") && left("File") < left("Edit") && left("Edit") < left("Untitled"))
        onNodeWithText("Save").assertDoesNotExist()
        onNodeWithText("File").performClick()
        onNodeWithText("Undo").assertDoesNotExist()
        onNodeWithText("Save").performClick()
        assertEquals(1, ran)
        onNodeWithText("Save").assertDoesNotExist()
    }

    private val drawers = listOf(
        DrawerSpec("layers", "Layers", StretchIcons.Layers) { RowText("Body") },
        DrawerSpec("library", "Library", StretchIcons.Grid) { RowText("Shapes") },
    )

    @Test
    fun aDrawersKeysShowInItsRailTooltip() = runComposeUiTest {
        val keyed = listOf(DrawerSpec("layers", "Layers", StretchIcons.Layers, keys = listOf("L")) { RowText("Body") })
        setStretchContent {
            StretchApp(FrameLayout(listOf(DrawerPlacement("layers", Side.Left))), {}, keyed, onResetLayout = {}, topBar = { TopBar("App", "Untitled") }) {
                Viewport {}
            }
        }
        onNodeWithText("L").assertDoesNotExist()
        onNodeWithContentDescription("Layers").performMouseInput { enter(center) }
        mainClock.advanceTimeBy(1000)
        onNodeWithText("L").assertExists()
    }

    @Test
    fun railOpensAndClosesDrawers() = runComposeUiTest {
        var layout by mutableStateOf(FrameLayout(listOf(DrawerPlacement("layers", Side.Left), DrawerPlacement("library", Side.Right))))
        setStretchContent {
            StretchApp(layout, { layout = it }, drawers, onResetLayout = {}, topBar = { TopBar("App", "Untitled") }) {
                Viewport {}
            }
        }
        onNodeWithText("Shapes").assertDoesNotExist()
        onNodeWithContentDescription("Library").performClick()
        onNodeWithText("Shapes").assertExists()
        assertTrue(layout["library"]!!.open)
        onNodeWithContentDescription("Library").performClick()
        onNodeWithText("Shapes").assertDoesNotExist()
    }

    @Test
    fun paletteFindsAndRunsAnAction() = runComposeUiTest {
        var ran = ""
        val registry = ActionRegistry().apply {
            register(Action(ActionId("mirror"), "Mirror to other side", keys = listOf(KeyChord.parse("M"))) { ran = "mirror" })
            register(Action(ActionId("smooth"), "Smooth") { ran = "smooth" })
        }
        var open by mutableStateOf(true)
        setStretchContent {
            CommandPalette(open, registry.available(null), Keymap(registry), onDismiss = { open = false })
        }
        onNodeWithContentDescription("Search commands").performTextInput("mir")
        onNodeWithText("Smooth").assertDoesNotExist()
        onNodeWithContentDescription("Search commands").performKeyInput { pressKey(Key.Enter) }
        assertEquals("mirror", ran)
        assertEquals(false, open)
    }

    @Test
    fun boundKeysRunActions() = runComposeUiTest {
        var commands = 0
        val registry = ActionRegistry().apply {
            register(Action(ActionId("commands"), "Commands", keys = listOf(KeyChord.parse("Ctrl K"))) { commands++ })
        }
        val focus = FocusRequester()
        setStretchContent {
            Box(Modifier.size(100.dp).actionKeys(Keymap(registry), null).focusRequester(focus).focusable().testTag("root"))
        }
        runOnIdle { focus.requestFocus() }
        onNodeWithTag("root").performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(Key.K) } }
        assertEquals(1, commands)
    }

    @Test
    fun typingInAFieldLeavesPlainKeyShortcutsAlone() = runComposeUiTest {
        var types = 0
        var commands = 0
        val registry = ActionRegistry().apply {
            register(Action(ActionId("types"), "Types", keys = listOf(KeyChord.parse("T"))) { types++ })
            register(Action(ActionId("commands"), "Commands", keys = listOf(KeyChord.parse("Ctrl K"))) { commands++ })
        }
        var query by mutableStateOf("")
        setStretchContent {
            Box(Modifier.actionKeys(Keymap(registry), null)) { TextField(query, { query = it }, placeholder = "Search") }
        }
        val field = onNodeWithContentDescription("Search")
        field.performClick()
        field.performKeyInput { pressKey(Key.T) }
        field.performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.T) } }
        assertEquals(0, types)
        field.performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(Key.K) } }
        assertEquals(1, commands)
    }

    @Test
    fun dropdownPicksFromItsMenu() = runComposeUiTest {
        var blend by mutableStateOf("Normal")
        setStretchContent { Dropdown(listOf("Normal", "Multiply"), blend, { blend = it }, { it }, "Blend") }
        onNodeWithContentDescription("Blend").performClick()
        onNodeWithText("Multiply").performClick()
        assertEquals("Multiply", blend)
        onNodeWithText("Normal").assertDoesNotExist()
    }

    @Test
    fun dialogClosesFromItsHeader() = runComposeUiTest {
        var open by mutableStateOf(true)
        setStretchContent {
            if (open) StretchDialog("Export", onDismiss = { open = false }) { RowText("Format") }
        }
        onNodeWithText("Format").assertExists()
        onNodeWithContentDescription("Close").performClick()
        onNodeWithText("Format").assertDoesNotExist()
    }

    @Test
    fun toastOffersUndo() = runComposeUiTest {
        val toasts = ToastState()
        var undone = false
        setStretchContent {
            Box {
                Button("Delete", { toasts.show("Deleted · Fur edge", "Undo") { undone = true } })
                ToastHost(toasts)
            }
        }
        onNodeWithText("Delete").performClick()
        onNodeWithText("Deleted · Fur edge").assertExists()
        onNodeWithText("Undo").performClick()
        assertTrue(undone)
        onNodeWithText("Deleted · Fur edge").assertDoesNotExist()
    }

    @Test
    fun aDrawerNotEnabledKeepsItsRailButtonAndDoesNotOpen() = runComposeUiTest {
        var layout by mutableStateOf(FrameLayout(listOf(DrawerPlacement("rig", Side.Right), DrawerPlacement("part", Side.Right))))
        setStretchContent {
            StretchApp(
                layout,
                { layout = it },
                listOf(DrawerSpec("rig", "Rig", StretchIcons.Layers, enabled = false) {}, DrawerSpec("part", "Outline", StretchIcons.Layers, headerIcon = StretchIcons.Pen, railLabel = "Part") {}),
                onResetLayout = {},
                topBar = { TopBar("App", "Untitled") },
            ) { Viewport {} }
        }
        onNodeWithContentDescription("Rig").assertExists().assertIsNotEnabled().performClick()
        assertTrue(layout["rig"]!!.open.not())
        onNodeWithContentDescription("Part").performClick()
        assertTrue(layout["part"]!!.open)
    }

    @Test
    fun undoAndRedoStayInTheBarWhileThereIsNothingToDo() = runComposeUiTest {
        var undone = 0
        setStretchContent {
            TopBar("App", "Untitled", actions = { TopBarActions({ undone++ }, {}, {}, undoEnabled = false, redoEnabled = true) })
        }
        onNodeWithContentDescription("Undo").assertIsNotEnabled().performClick()
        assertEquals(0, undone)
        onNodeWithContentDescription("Redo").assertIsEnabled()
    }

    @Test
    fun anActionMenuItemIsFaintWhileItsActionIsNotAvailable() = runComposeUiTest {
        var ran = 0
        val registry = ActionRegistry()
        val action = Action(ActionId("x"), "x", isAvailable = { false }, run = { ran++ }).also(registry::register)
        setStretchContent { Column { ActionMenuItem(action, Keymap(registry)) } }
        onNodeWithText("x").assertIsNotEnabled().performClick()
        assertEquals(0, ran)
    }
}
