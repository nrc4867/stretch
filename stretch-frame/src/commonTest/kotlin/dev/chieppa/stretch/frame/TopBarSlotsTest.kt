package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import dev.chieppa.stretch.core.KeyChord
import dev.chieppa.stretch.test.setStretchContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class TopBarSlotsTest {
    private var barWidth by mutableStateOf(1000.dp)
    private var centerWidth by mutableStateOf(320.dp)

    private fun ComposeUiTest.showBar(leadingWidth: Dp = 120.dp) {
        setStretchContent {
            Box(Modifier.width(barWidth).testTag("bar")) {
                TopBar(
                    "App",
                    "Untitled",
                    menus = { Box(Modifier.size(100.dp, 24.dp).testTag("menus")) },
                    leading = { Box(Modifier.size(leadingWidth, 24.dp).testTag("leading")) },
                    center = { Box(Modifier.width(centerWidth).height(28.dp).testTag("center")) },
                    actions = { TopBarActions(onUndo = {}, onRedo = {}, onCommands = {}, onSettings = {}) },
                )
            }
        }
    }

    private fun ComposeUiTest.bounds(tag: String): DpRect = onNodeWithTag(tag).getUnclippedBoundsInRoot()

    private fun ComposeUiTest.actionsLeft() = onNodeWithContentDescription("Undo").getUnclippedBoundsInRoot().left

    @Test
    fun theCenterItemIsCentredOnTheWholeBar() = runComposeUiTest {
        showBar()
        val bar = bounds("bar")
        val center = bounds("center")
        assertEquals(((bar.left + bar.right) / 2).value, ((center.left + center.right) / 2).value, 0.5f)
        assertEquals(320f, center.width.value, 0.5f)
    }

    @Test
    fun theLeadingItemSitsAfterTheMenusAndBeforeTheDocument() = runComposeUiTest {
        showBar(leadingWidth = 40.dp)
        val menus = bounds("menus")
        val leading = bounds("leading")
        val document = onNodeWithText("Untitled").getUnclippedBoundsInRoot()
        assertTrue(leading.left > menus.right, "after the menus")
        assertTrue(document.left > leading.right, "before the document")
    }

    @Test
    fun inANarrowBarTheCenterItemMovesClearOfBothSidesThenNarrowsThenHides() = runComposeUiTest {
        barWidth = 1000.dp
        showBar(leadingWidth = 300.dp)
        val leading = bounds("leading")
        var center = bounds("center")
        assertTrue(center.left > leading.right, "clear of the left group")
        assertTrue(center.right < actionsLeft(), "clear of the actions")
        assertEquals(320f, center.width.value, 0.5f, "still its own width")
        assertTrue((center.left + center.right) / 2 > (bounds("bar").right / 2), "moved right of centre")

        barWidth = 860.dp
        waitForIdle()
        center = bounds("center")
        assertTrue(center.width < 320.dp && center.width >= 160.dp, "narrowed to ${center.width}")
        assertTrue(center.left > bounds("leading").right && center.right < actionsLeft())

        barWidth = 700.dp
        waitForIdle()
        onNodeWithTag("center").assertDoesNotExist()
        onNodeWithTag("leading").assertExists()
    }

    @Test
    fun theDocumentNameGivesWayBeforeTheCenterItemMoves() = runComposeUiTest {
        showBar()
        onNodeWithText("Untitled").assertDoesNotExist()
        val bar = bounds("bar")
        val center = bounds("center")
        assertEquals(((bar.left + bar.right) / 2).value, ((center.left + center.right) / 2).value, 0.5f)
    }

    @Test
    fun theCommandsTooltipShowsTheAppsBinding() = runComposeUiTest {
        setStretchContent {
            TopBar("App", "Untitled") {
                TopBarActions(onUndo = null, onRedo = null, onCommands = {}, commandsKeys = KeyChord.parse("Ctrl Shift P").caps())
            }
        }
        onNodeWithContentDescription("Commands").performMouseInput { enter(center) }
        mainClock.advanceTimeBy(1000)
        onNodeWithText("P").assertExists()
        onNodeWithText("K").assertDoesNotExist()
    }

    @Test
    fun theCommandsTooltipShowsCtrlKUnlessTold() = runComposeUiTest {
        setStretchContent {
            TopBar("App", "Untitled") { TopBarActions(onUndo = null, onRedo = null, onCommands = {}) }
        }
        onNodeWithContentDescription("Commands").performMouseInput { enter(center) }
        mainClock.advanceTimeBy(1000)
        onNodeWithText("K").assertExists()
    }
}
