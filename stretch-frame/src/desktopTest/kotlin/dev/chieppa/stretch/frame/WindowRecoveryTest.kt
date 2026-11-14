package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.test.setStretchContent
import java.awt.EventQueue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

/**
 * A window's answers to its crashes, with the window's content composed in the test and crashes handed to its
 * [WindowRecovery] as the window's exception handler would. [WindowRecoveryScreenTest] throws real ones in real windows.
 */
@OptIn(ExperimentalTestApi::class)
class WindowRecoveryTest {
    private val time = TestTimeSource()
    private val holds = TabHolds(time)
    private val reports = mutableListOf<WindowCrash>()
    private var closed = false
    private val recovery = WindowRecovery(WindowTabs(holds), { reports += it }, { closed = true })
    private var layout by mutableStateOf(PaneLayout().open("notes").open("bad").activate("notes"))
    private var builds = 0

    private fun ComposeUiTest.showWindow() {
        setStretchContent {
            CompositionLocalProvider(LocalTabHolds provides holds) {
                RecoveringWindow(recovery) {
                    remember { builds++ }
                    WindowSettles()
                    Box(Modifier.size(800.dp, 500.dp)) {
                        PaneGrid(layout, { layout = it }, { TabItem(it, it.uppercase()) }) { id -> RowText("Showing $id") }
                    }
                }
            }
        }
        waitForIdle()
    }

    private fun ComposeUiTest.crash(build: Int = recovery.generation) {
        runOnIdle { recovery.crash(build, IllegalStateException("build $build")) }
        waitForIdle()
        // Closing goes through the event queue, as it must not happen inside the failing frame.
        EventQueue.invokeAndWait {}
    }

    @Test
    fun aTabThatJustStartedShowingIsHeldAndTheWindowBuiltAgain() = runComposeUiTest {
        showWindow()
        layout = layout.activate("bad")
        waitForIdle()
        crash()
        assertEquals(listOf(listOf("bad")), reports.map { it.held })
        assertFalse(reports.single().closed)
        assertEquals(2, builds)
        onNodeWithText("Showing bad").assertDoesNotExist()
        onNodeWithText("IllegalStateException: build 0").assertExists()
        onNodeWithContentDescription("Reload").assertExists()
        assertFalse(closed)
    }

    @Test
    fun aTabThatCrashesAgainSoonAfterItsReloadIsHeldAgainWithTheNewCrash() = runComposeUiTest {
        showWindow()
        layout = layout.activate("bad")
        waitForIdle()
        crash()
        time += 30.seconds
        onNodeWithContentDescription("Reload").performClick()
        waitForIdle()
        onNodeWithText("Showing bad").assertExists()
        time += 5.seconds
        crash()
        assertEquals(listOf("bad"), reports.last().held, "past the shown window, the reload still puts it on the tab")
        onNodeWithText("IllegalStateException: build 1").assertExists()
        assertFalse(closed)
    }

    @Test
    fun onlyTheFirstFailureOfABuildCounts() = runComposeUiTest {
        showWindow()
        crash()
        crash(build = 0)
        crash(build = 1)
        crash(build = 1)
        assertEquals(2, reports.size, "$reports")
        assertEquals(3, builds)
    }

    @Test
    fun crashesThatKeepComingHoldEveryShowingTabAndThenClose() = runComposeUiTest {
        showWindow()
        crash()
        assertEquals(emptyList(), reports.last().held, "a first crash builds the window as it was")
        time += 1.seconds
        crash()
        assertEquals(listOf("notes"), reports.last().held)
        onNodeWithText("Showing notes").assertDoesNotExist()
        time += 1.seconds
        crash()
        assertTrue(reports.last().closed, "$reports")
        assertTrue(closed)
    }

    @Test
    fun aCrashLongAfterTheLastBuildsTheWindowAsItWas() = runComposeUiTest {
        showWindow()
        crash()
        time += 60.seconds
        crash()
        assertEquals(listOf(emptyList<String>(), emptyList()), reports.map { it.held })
        onNodeWithText("Showing notes").assertExists()
        assertFalse(closed)
    }
}
