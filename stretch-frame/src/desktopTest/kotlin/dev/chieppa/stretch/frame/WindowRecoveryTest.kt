package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import java.awt.EventQueue
import java.awt.GraphicsEnvironment
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Opens real windows, so it runs only where there is a screen. */
class WindowRecoveryTest {
    private val crashes = CopyOnWriteArrayList<WindowCrash>()
    private var layout by mutableStateOf(PaneLayout().open("notes").open("bad").activate("notes"))
    private var open by mutableStateOf(true)
    @Volatile private var builds = 0
    private var app: Thread? = null

    @AfterTest
    fun tearDown() {
        EventQueue.invokeAndWait { open = false }
        app?.join(TIMEOUT_MS)
    }

    private fun launch(chrome: @Composable () -> Unit = {}, bad: @Composable () -> Unit) {
        app = thread {
            application(exitProcessOnExit = false) {
                if (open) {
                    StretchWindow(
                        onCloseRequest = { open = false },
                        title = "Recovery probe",
                        state = rememberWindowState(width = WINDOW_DP, height = WINDOW_DP),
                        onCrash = { crashes += it },
                    ) {
                        SideEffect { builds++ }
                        StretchTheme(StretchThemes.Ink) {
                            chrome()
                            PaneGrid(layout, { layout = it }, { TabItem(it, it) }) { id ->
                                if (id == "bad") bad() else RowText("Showing $id")
                            }
                        }
                    }
                }
            }
        }
        waitFor { builds > 0 }
        Thread.sleep(SETTLE_MS)
    }

    private fun showBad() = EventQueue.invokeAndWait { layout = layout.activate("bad") }

    private fun waitFor(condition: () -> Boolean) {
        val until = System.currentTimeMillis() + TIMEOUT_MS
        while (!condition()) {
            check(System.currentTimeMillis() < until) { "timed out; crashes: $crashes" }
            Thread.sleep(POLL_MS)
        }
    }

    @Test
    fun aTabThatThrowsWhileComposingIsHeldAndTheWindowStaysOpen() {
        if (GraphicsEnvironment.isHeadless()) return
        launch { error("composing") }
        val before = builds
        showBad()
        waitFor { crashes.isNotEmpty() && builds > before }
        Thread.sleep(SETTLE_MS)
        assertEquals(1, crashes.size, "$crashes")
        assertEquals(listOf("bad"), crashes.single().held)
        assertFalse(crashes.single().closed)
        assertTrue(open)
    }

    @Test
    fun aTabThatThrowsWhileMeasuringIsHeldAndTheWindowStaysOpen() {
        if (GraphicsEnvironment.isHeadless()) return
        launch { Box(Modifier.fillMaxSize().layout { _, _ -> error("measuring") }) }
        showBad()
        waitFor { crashes.isNotEmpty() }
        Thread.sleep(SETTLE_MS)
        assertEquals(listOf("bad"), crashes.single().held)
        assertTrue(open)
    }

    @Test
    fun aWindowThatKeepsCrashingOutsideItsTabsCloses() {
        if (GraphicsEnvironment.isHeadless()) return
        var broken by mutableStateOf(false)
        launch(chrome = { if (broken) error("chrome") }) { RowText("Fine") }
        EventQueue.invokeAndWait { broken = true }
        waitFor { !open }
        assertTrue(crashes.last().closed, "$crashes")
        assertTrue(crashes.size >= 2, "$crashes")
    }

    @Test
    fun aPaneWindowWhoseTabKeepsCrashingHoldsItWithoutTouchingTheMainWindow() {
        if (GraphicsEnvironment.isHeadless()) return
        layout = layout.detach("bad", WindowBounds(PANE_AT, PANE_AT, PANE_SIZE, PANE_SIZE))
        launch { error("composing in a pane window") }
        waitFor { crashes.any { it.held.isNotEmpty() } }
        Thread.sleep(SETTLE_MS)
        assertEquals(listOf("bad"), crashes.last().held, "$crashes")
        assertTrue(crashes.none { it.closed }, "$crashes")
        assertEquals(1, layout.windows.size)
        assertTrue(open)
    }

    private companion object {
        const val PANE_AT = 600f
        const val PANE_SIZE = 360f
        const val TIMEOUT_MS = 15_000L
        const val POLL_MS = 50L
        const val SETTLE_MS = 600L
        val WINDOW_DP = 480.dp
    }
}
