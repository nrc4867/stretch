package dev.chieppa.stretch.core

import kotlinx.coroutines.flow.StateFlow

/** Where a [Requirement] stands. */
sealed interface RequirementState {
    /** In place and in use. */
    data object Ready : RequirementState

    /** Not on this machine; [Requirement.install] fetches it. */
    data object Missing : RequirementState

    /** Fetching: [fetchedBytes] so far, of [totalBytes] when known. */
    data class Installing(val fetchedBytes: Long, val totalBytes: Long?) : RequirementState {
        /** How far along, 0 to 1, or null when the total is unknown. */
        val fraction: Float? get() = totalBytes?.takeIf { it > 0 }?.let { (fetchedBytes.toDouble() / it).toFloat().coerceIn(0f, 1f) }
    }

    /** On this machine, and in use once the app starts again through [Requirement.restart]. */
    data object NeedsRestart : RequirementState

    /** The last install stopped, for [reason]; installing again retries. */
    data class Failed(val reason: String) : RequirementState
}

/**
 * Something a Stretch module needs beyond the app's own files, which the app can fetch while it runs, such as the
 * browser engine `stretch-web` draws pages with. The module lists it in [Requirements]; the app decides whether to
 * offer it, when to install it and when to restart.
 */
interface Requirement {
    /** A stable id, like `stretch-web.engine`, for the app to name it in its own words. */
    val id: String

    /** About how many bytes [install] downloads, when known. */
    val downloadBytes: Long?

    val state: StateFlow<RequirementState>

    /**
     * Starts fetching and installing in the background and returns at once; [state] reports progress. The work is
     * not tied to any screen, so it carries on until it ends or [cancel]. Does nothing unless [state] is
     * [RequirementState.Missing] or [RequirementState.Failed].
     */
    fun install()

    /** Stops an install and removes what it fetched, back to [RequirementState.Missing]. */
    fun cancel()

    /** Starts the app again so an installed requirement takes effect. The app saves its work first; this process exits. */
    fun restart()
}

/** The requirements of the Stretch modules in this app, found on its classpath. */
object Requirements {
    /** Every module's requirements, ready or not. */
    val all: List<Requirement> by lazy { platformRequirements() }

    /** The one with [id], if a module in the app has it. */
    fun find(id: String): Requirement? = all.firstOrNull { it.id == id }
}

internal expect fun platformRequirements(): List<Requirement>
