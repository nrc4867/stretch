package dev.chieppa.stretch.frame

import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.time.TimeMark

/**
 * The tabs of one window, for putting a crash of that window on them: the tabs whose content is in the window's
 * composition, noted as soon as it starts composing so a crash in that very composition still finds it, and the last
 * tab to start. A window is [settling] from when it is built until its first frame, when every showing tab starts at
 * once, and those starts count for nothing.
 */
internal class WindowTabs(val holds: TabHolds) {
    private val composing = mutableMapOf<String, Any>()

    var settling = true

    /** The last tab whose content started showing outside [settling], and when. */
    var lastShown: Pair<String, TimeMark>? = null
        private set

    /** The tabs whose content is in the window. */
    fun showing(): List<String> = composing.keys.toList()

    /** Notes that tab [id]'s content starts showing, returning the showing to pass to [hidden]. */
    fun shown(id: String): Any {
        val showing = Any()
        composing[id] = showing
        if (!settling) lastShown = id to holds.time.markNow()
        return showing
    }

    /** Notes that tab [id]'s content has left the window, unless it has started showing again since [showing]. */
    fun hidden(id: String, showing: Any) {
        if (composing[id] === showing) composing.remove(id)
    }

    /** Forgets every showing tab, for a window about to be built again. */
    fun rebuilding() {
        composing.clear()
        settling = true
    }
}

/** The [WindowTabs] of the window being composed, where it recovers from crashes. */
internal val LocalWindowTabs = staticCompositionLocalOf<WindowTabs?> { null }
