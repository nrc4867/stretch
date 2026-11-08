package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import java.awt.EventQueue
import java.awt.GraphicsEnvironment
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Real crashes in real windows, placed off every screen: Compose's own window exception handling reaching
 * [WindowRecovery]. It runs only with -Pscreen. [WindowRecoveryTest] covers the answers to crashes without windows.
 */
class WindowRecoveryScreenTest {
    private val crashes = LinkedBlockingQueue<WindowCrash>()
    private val frames = LinkedBlockingQueue<Unit>()
    private val closed = CountDownLatch(1)
    private var layout by mutableStateOf(PaneLayout().open("notes").open("bad").activate("notes"))
    private var open by mutableStateOf(true)
    private var tick by mutableIntStateOf(0)
    private var app: Thread? = null

    @AfterTest
    fun tearDown() {
        EventQueue.invokeAndWait { open = false }
        app?.join(TIMEOUT_MS)
    }

    private fun <T> BlockingQueue<T>.next(what: String): T =
        assertNotNull(poll(TIMEOUT_MS, TimeUnit.MILLISECONDS), "no $what; crashes: $crashes")

    /** Opens the window and returns once it has drawn its first frame. */
    private fun launch(chrome: @Composable () -> Unit = {}, bad: @Composable () -> Unit) {
        app = thread {
            application(exitProcessOnExit = false) {
                if (open) {
                    StretchWindow(
                        onCloseRequest = {
                            open = false
                            closed.countDown()
                        },
                        title = "Recovery probe",
                        state = rememberWindowState(position = WindowPosition(OFF_SCREEN.dp, OFF_SCREEN.dp), width = WINDOW_DP, height = WINDOW_DP),
                        onCrash = { crashes += it },
                    ) {
                        // Built again with the window, so each build reports its first frame, and again on each tick.
                        LaunchedEffect(tick) {
                            withFrameNanos { }
                            frames += Unit
                        }
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
        frames.next("first frame")
        EventQueue.invokeAndWait {
            val shown = java.awt.Window.getWindows().filter { it.isShowing }
            assertTrue(shown.all { it.x + it.width < 0 && it.y + it.height < 0 }, "on screen: ${shown.map { it.bounds }}")
        }
    }

    /** Waits for the main window to draw a frame begun after this call. */
    private fun nextFrame() {
        frames.clear()
        EventQueue.invokeAndWait { tick++ }
        frames.next("frame")
    }

    private fun showBad() = EventQueue.invokeAndWait { layout = layout.activate("bad") }

    @Test
    fun aTabThatThrowsWhileComposingIsHeldAndTheWindowStaysOpen() {
        if (GraphicsEnvironment.isHeadless()) return
        launch { error("composing") }
        showBad()
        val crash = crashes.next("crash")
        frames.next("frame of the window built again")
        assertEquals(listOf("bad"), crash.held)
        assertFalse(crash.closed)
        assertTrue(crashes.isEmpty(), "$crashes")
        assertTrue(open)
    }

    @Test
    fun aTabThatThrowsWhileMeasuringIsHeldAndTheWindowStaysOpen() {
        if (GraphicsEnvironment.isHeadless()) return
        launch { Box(Modifier.fillMaxSize().layout { _, _ -> error("measuring") }) }
        showBad()
        val crash = crashes.next("crash")
        frames.next("frame of the window built again")
        assertEquals(listOf("bad"), crash.held)
        assertTrue(crashes.isEmpty(), "$crashes")
        assertTrue(open)
    }

    @Test
    fun aWindowThatKeepsCrashingOutsideItsTabsCloses() {
        if (GraphicsEnvironment.isHeadless()) return
        var broken by mutableStateOf(false)
        launch(chrome = { if (broken) error("chrome") }) { RowText("Fine") }
        EventQueue.invokeAndWait { broken = true }
        assertTrue(closed.await(TIMEOUT_MS, TimeUnit.MILLISECONDS), "never closed; crashes: $crashes")
        val all = crashes.toList()
        assertTrue(all.last().closed, "$all")
        assertTrue(all.size >= 2, "$all")
    }

    @Test
    fun aPaneWindowWhoseTabKeepsCrashingHoldsItWithoutTouchingTheMainWindow() {
        if (GraphicsEnvironment.isHeadless()) return
        layout = layout.detach("bad", WindowBounds(OFF_SCREEN.toFloat(), OFF_SCREEN.toFloat(), PANE_SIZE, PANE_SIZE))
        launch { error("composing in a pane window") }
        var crash = crashes.next("crash")
        while (crash.held.isEmpty()) crash = crashes.next("crash that holds a tab")
        nextFrame()
        assertEquals(listOf("bad"), crash.held)
        assertTrue(crashes.none { it.closed }, "$crashes")
        assertEquals(1, layout.windows.size)
        assertTrue(open)
    }

    private companion object {
        /** Far left of and above any screen, so no one sees the windows. */
        const val OFF_SCREEN = -20_000
        const val PANE_SIZE = 360f
        const val TIMEOUT_MS = 15_000L
        val WINDOW_DP = 480.dp
    }
}
