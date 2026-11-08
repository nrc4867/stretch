package dev.chieppa.stretch.web

import dev.chieppa.stretch.core.Settled
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The browser engine's work under way in this app: on the desktop, an [HtmlView] waiting for its first picture of the
 * page it shows, a [ScriptPage] running a script, or a block in [during]. [settled] while there is none, so a test
 * waits for pages and diagrams by waiting for it, as stretch-test's `registerSettled` does, never for time to pass.
 */
object EngineWork : Settled {
    private val running = MutableStateFlow(0)
    private val idle = MutableStateFlow(true)

    override val settled: StateFlow<Boolean> = idle.asStateFlow()

    /** Counts [block] as engine work, for a library that reaches a page through steps of its own around a [ScriptPage]. */
    suspend fun <T> during(block: suspend () -> T): T {
        begin()
        try {
            return block()
        } finally {
            end()
        }
    }

    internal fun begin() = change(1)

    internal fun end() = change(-1)

    private fun change(by: Int) {
        running.update { it + by }
        // Another thread may change the count between reading and publishing it; whoever publishes last sees the final count.
        do {
            val now = running.value
            idle.value = now == 0
        } while (running.value != now)
    }
}
