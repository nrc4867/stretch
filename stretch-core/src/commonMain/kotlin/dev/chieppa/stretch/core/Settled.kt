package dev.chieppa.stretch.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

/** True once every piece of derived data has caught up with the current document. */
interface Settled {
    val settled: StateFlow<Boolean>

    /** For apps with no derived data. */
    object Always : Settled {
        override val settled: StateFlow<Boolean> = MutableStateFlow(true)
    }
}

suspend fun Settled.awaitSettled() {
    settled.first { it }
}
