package dev.chieppa.stretch.frame

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
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

    private val drawers = listOf(
        DrawerSpec("layers", "Layers", StretchIcons.Layers) { RowText("Body") },
        DrawerSpec("library", "Library", StretchIcons.Grid) { RowText("Shapes") },
    )

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
}
