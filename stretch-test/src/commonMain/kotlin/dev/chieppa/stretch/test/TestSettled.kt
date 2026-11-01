package dev.chieppa.stretch.test

import dev.chieppa.stretch.core.Settled
import dev.chieppa.stretch.core.awaitSettled
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** A [Settled] a test controls directly. */
class TestSettled(settled: Boolean = true) : Settled {
    override val settled = MutableStateFlow(settled)

    fun set(value: Boolean) {
        settled.value = value
    }
}

/** Waits for [settled], failing after [timeout]. A condition with a timeout, never a fixed delay. */
suspend fun awaitSettled(settled: Settled, timeout: Duration = 10.seconds) {
    withTimeout(timeout) { settled.awaitSettled() }
}
