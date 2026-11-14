package dev.chieppa.stretch.frame

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * The tabs held back after a window crashed with them showing, each with the crash's one-line message, shared by every
 * window of an app. A [PaneGrid] shows a held tab as [HeldTab], with the message and a reload in place of its content,
 * until the reload releases it; the last release is kept, so a crash soon after it is put on that tab.
 */
internal class TabHolds(val time: TimeSource = TimeSource.Monotonic) {
    private val held = mutableStateMapOf<String, String>()

    /** The held tabs. */
    val ids: Set<String> get() = held.keys

    /** The last tab released by a reload, and when. */
    var lastReload: Pair<String, TimeMark>? = null
        private set

    fun isHeld(id: String): Boolean = id in held

    /** The message of the crash tab [id] is held for, or null when it is not held. */
    fun message(id: String): String? = held[id]

    fun hold(id: String, message: String = "") {
        held[id] = message
    }

    /** Lets tab [id] show again, as its reload does. */
    fun release(id: String) {
        if (held.remove(id) != null) lastReload = id to time.markNow()
    }
}

/** The app's [TabHolds], where its windows recover from crashes; without one, no tab is held. */
internal val LocalTabHolds = staticCompositionLocalOf<TabHolds?> { null }
