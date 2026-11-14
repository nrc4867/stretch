package dev.chieppa.stretch.frame

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** What a window does after it crashed: build itself again with [hold] held back, or close. */
internal sealed interface CrashResponse {
    data class Rebuild(val hold: List<String>) : CrashResponse

    data object Close : CrashResponse
}

/**
 * What a window knows when it crashes. [lastShown] is the last tab whose content started showing in the window, which
 * may be missing from [showing] when the crash stopped the frame that showed it; [sinceShown] is how long ago.
 * [reloaded] is the last tab a reload released, [sinceReloaded] how long ago.
 */
internal data class CrashFacts(
    val showing: List<String>,
    val held: Set<String>,
    val lastShown: String?,
    val sinceShown: Duration?,
    val sinceLastCrash: Duration?,
    val reloaded: String? = null,
    val sinceReloaded: Duration? = null,
)

/**
 * How a window answers a crash. A tab that started showing just before is held and the window rebuilt, and so is a
 * showing tab reloaded within [REPEAT_WINDOW], so a reload that crashes again leaves that tab held rather than the
 * window rebuilt as it was. Otherwise a first crash rebuilds the window as it was, and a crash soon after another holds
 * every showing tab; when no tab is left to hold, the window closes. Each step holds more, so a crash that keeps coming
 * ends in a close.
 */
internal object CrashLadder {
    /** How soon after a tab starts showing a crash is put on that tab. */
    val SHOWN_WINDOW: Duration = 2.seconds

    /** How soon after a crash another counts as the same trouble coming back. */
    val REPEAT_WINDOW: Duration = 10.seconds

    fun respond(facts: CrashFacts): CrashResponse {
        val open = facts.showing.filter { it !in facts.held }
        val shown = facts.lastShown?.takeIf { it !in facts.held && facts.sinceShown != null && facts.sinceShown <= SHOWN_WINDOW }
        val reloaded = facts.reloaded?.takeIf { it in open && facts.sinceReloaded != null && facts.sinceReloaded <= REPEAT_WINDOW }
        return when {
            shown != null -> CrashResponse.Rebuild(listOf(shown))
            reloaded != null -> CrashResponse.Rebuild(listOf(reloaded))
            facts.sinceLastCrash == null || facts.sinceLastCrash > REPEAT_WINDOW -> CrashResponse.Rebuild(emptyList())
            open.isEmpty() -> CrashResponse.Close
            else -> CrashResponse.Rebuild(open)
        }
    }
}
