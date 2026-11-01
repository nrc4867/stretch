package dev.chieppa.stretch.test

import dev.chieppa.stretch.core.Command
import dev.chieppa.stretch.core.History
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A command scenario: commands, gestures, undo and redo against a document, with assertions on the state between
 * steps. Every scenario finishes by checking that the session log replays to the same document.
 */
class Scenario<D> internal constructor(private val initial: D) {
    val history = History(initial, limit = Int.MAX_VALUE)
    val document: D get() = history.document.value

    fun run(vararg commands: Command<D>) {
        commands.forEach(history::execute)
    }

    /** One gesture: each step is a live preview from the gesture's start; only the last one becomes the undo step. */
    fun gesture(vararg steps: Command<D>) {
        val gesture = history.begin()
        steps.forEach(gesture::update)
        gesture.commit()
    }

    fun undo(times: Int = 1) = repeat(times) { assertTrue(history.undo(), "Nothing to undo") }

    fun redo(times: Int = 1) = repeat(times) { assertTrue(history.redo(), "Nothing to redo") }

    fun expect(expected: D) = assertEquals(expected, document)

    fun expect(check: (D) -> Unit) = check(document)

    internal fun finish() {
        assertEquals(document, history.log.replay(initial), "The session log does not replay to the same document")
    }
}

fun <D> scenario(initial: D, block: Scenario<D>.() -> Unit): D {
    val scenario = Scenario(initial)
    scenario.block()
    scenario.finish()
    return scenario.document
}
