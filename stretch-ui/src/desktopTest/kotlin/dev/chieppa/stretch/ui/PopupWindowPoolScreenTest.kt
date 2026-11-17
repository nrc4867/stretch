package dev.chieppa.stretch.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.awt.LocalAwtWindow
import androidx.compose.ui.awt.SwingDialog
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import java.awt.EventQueue
import java.awt.Window
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import javax.swing.JFrame
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertSame

/**
 * Popups opened from a popup whose window goes back to [PopupWindowPool], in real windows placed off every screen. It runs
 * only with -Pscreen.
 */
class PopupWindowPoolScreenTest {
    private val crashes = LinkedBlockingQueue<Throwable>()
    private val shown = LinkedBlockingQueue<Window>()
    private val gone = LinkedBlockingQueue<Window>()
    private var formerHandler: Thread.UncaughtExceptionHandler? = null
    private var formerLayers: String? = null
    private val windows = mutableListOf<Window>()

    @BeforeTest
    fun setUp() {
        formerHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, e -> crashes += e }
        formerLayers = System.getProperty(LAYERS_PROPERTY)
        System.setProperty(LAYERS_PROPERTY, LAYERS_IN_WINDOWS)
    }

    @AfterTest
    fun tearDown() {
        EventQueue.invokeAndWait { windows.forEach(Window::dispose) }
        Thread.setDefaultUncaughtExceptionHandler(formerHandler)
        formerLayers?.let { System.setProperty(LAYERS_PROPERTY, it) } ?: System.clearProperty(LAYERS_PROPERTY)
    }

    private fun <T> BlockingQueue<T>.next(what: String): T =
        assertNotNull(poll(TIMEOUT_MS, TimeUnit.MILLISECONDS), "no $what; crashes: $crashes")

    /** Runs [block] on the UI thread once everything queued there before it has run. */
    private fun <T> onUi(block: () -> T): T {
        var result: Result<T>? = null
        EventQueue.invokeAndWait { result = runCatching(block) }
        return checkNotNull(result).getOrThrow()
    }

    private fun <W : Window> offScreen(window: W): W = window.apply {
        setBounds(OFF_SCREEN, OFF_SCREEN, WINDOW_PX, WINDOW_PX)
        focusableWindowState = false
        isAutoRequestFocus = false
    }

    /** Reports the window it is composed in once that window shows, and again when it leaves the window. */
    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    private fun ReportShown() {
        val window = checkNotNull(LocalAwtWindow.current)
        DisposableEffect(window) {
            var reported = false
            fun report() {
                if (!reported) shown += window
                reported = true
            }
            val listener = object : ComponentAdapter() {
                override fun componentShown(e: ComponentEvent) = report()
            }
            window.addComponentListener(listener)
            if (window.isShowing) report()
            onDispose {
                window.removeComponentListener(listener)
                gone += window
            }
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun aPopupClosedWithItsWindowStaysClosedWhenTheWindowIsReused() {
        val owner = onUi { offScreen(JFrame()).also { windows += it; it.isVisible = true } }
        val corner = onUi {
            newPopupWindow(owner, focusable = false).also { corner ->
                offScreen(corner)
                corner.setContent {
                    SwingDialog(create = { offScreen(newPopupWindow(corner, focusable = false)) }, dispose = ComposeDialog::dispose) {
                        ReportShown()
                    }
                }
                corner.isVisible = true
            }
        }
        val nested = shown.next("nested popup")
        onUi { PopupWindowPool.release(corner) }
        assertFalse(nested.isDisplayable)
        onUi {
            val reused = PopupWindowPool.take(owner)
            assertSame(corner, reused)
            corner.isVisible = true
        }
        assertFalse(nested.isVisible)
        onUi { PopupWindowPool.release(corner) }
    }

    @Test
    fun aPopupInAPopupThatOpensAndClosesOverAndOverNeverShowsAClosedWindow() {
        var cornerOpen by mutableStateOf(true)
        onUi {
            ComposeWindow().also { window ->
                windows += window
                offScreen(window)
                window.setContent {
                    Box(Modifier.size(SIDE_DP.dp)) {
                        if (cornerOpen) {
                            StretchPopup(AtTopLeft, properties = HoldsPlace) {
                                Box(Modifier.size(SIDE_DP.dp)) {
                                    StretchPopup(AtTopLeft, properties = PopupProperties(focusable = false)) {
                                        Box(Modifier.size(SIDE_DP.dp)) { ReportShown() }
                                    }
                                }
                            }
                        }
                    }
                }
                window.isVisible = true
            }
        }
        repeat(ROUNDS) { round ->
            shown.next("nested popup in round $round")
            onUi { cornerOpen = false }
            gone.next("nested popup closing in round $round")
            onUi { cornerOpen = true }
        }
        shown.next("nested popup after the last round")
        onUi { }
        assertEquals(emptyList(), crashes.toList())
    }

    private object AtTopLeft : PopupPositionProvider {
        override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize) =
            IntOffset.Zero
    }

    private companion object {
        val HoldsPlace = PopupProperties(focusable = false, dismissOnBackPress = false, dismissOnClickOutside = false)
        const val OFF_SCREEN = -20_000
        const val WINDOW_PX = 120
        const val SIDE_DP = 40
        const val ROUNDS = 12
        const val TIMEOUT_MS = 20_000L
    }
}
