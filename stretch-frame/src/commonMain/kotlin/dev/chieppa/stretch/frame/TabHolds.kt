package dev.chieppa.stretch.frame

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.time.TimeSource

/**
 * The tabs held back after a window crashed with them showing, shared by every window of an app. A [PaneGrid] shows a
 * held tab as [HeldTab], with a reload in place of its content, until the reload releases it.
 */
internal class TabHolds(val time: TimeSource = TimeSource.Monotonic) {
    private val held = mutableStateMapOf<String, Boolean>()

    /** The held tabs. */
    val ids: Set<String> get() = held.keys

    fun isHeld(id: String): Boolean = held[id] == true

    fun hold(id: String) {
        held[id] = true
    }

    fun release(id: String) {
        held.remove(id)
    }
}

/** The app's [TabHolds], where its windows recover from crashes; without one, no tab is held. */
internal val LocalTabHolds = staticCompositionLocalOf<TabHolds?> { null }
