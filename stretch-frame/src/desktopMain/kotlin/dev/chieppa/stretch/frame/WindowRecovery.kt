package dev.chieppa.stretch.frame

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.WindowExceptionHandler
import androidx.compose.ui.window.WindowExceptionHandlerFactory
import java.util.WeakHashMap
import javax.swing.SwingUtilities
import kotlin.time.TimeMark

/**
 * Keeps one window of the app alive through crashes: the window's exception handler answers a crash by the
 * [CrashLadder], holding back tabs on [tabs] and building the window again under a new [generation], or calling
 * [close]. Each crash goes to [report]. A crashed window keeps failing until it is gone, and only its first failure
 * counts.
 */
@OptIn(ExperimentalComposeUiApi::class)
internal class WindowRecovery(
    val tabs: WindowTabs,
    private val report: (WindowCrash) -> Unit,
    private val close: () -> Unit,
) {
    /** Counts the builds of the window; the window is keyed by it. */
    var generation by mutableIntStateOf(0)
        private set

    private val builtIn = WeakHashMap<java.awt.Window, Int>()
    private var crashed = -1
    private var lastCrash: TimeMark? = null

    val factory = WindowExceptionHandlerFactory { window ->
        val built = builtIn.getOrPut(window) { generation }
        WindowExceptionHandler { cause -> crash(built, cause) }
    }

    /** Answers [cause], thrown in a window of build [built]. */
    fun crash(built: Int, cause: Throwable) {
        if (built != generation || built == crashed) return
        crashed = built
        val shown = tabs.lastShown
        val facts = CrashFacts(
            showing = tabs.showing(),
            held = tabs.holds.ids,
            lastShown = shown?.first,
            sinceShown = shown?.second?.elapsedNow(),
            sinceLastCrash = lastCrash?.elapsedNow(),
        )
        lastCrash = tabs.holds.time.markNow()
        when (val response = CrashLadder.respond(facts)) {
            is CrashResponse.Rebuild -> {
                response.hold.forEach(tabs.holds::hold)
                tabs.rebuilding()
                generation++
                report(WindowCrash(cause, response.hold, closed = false))
            }
            CrashResponse.Close -> {
                report(WindowCrash(cause, emptyList(), closed = true))
                SwingUtilities.invokeLater(close)
            }
        }
    }
}
