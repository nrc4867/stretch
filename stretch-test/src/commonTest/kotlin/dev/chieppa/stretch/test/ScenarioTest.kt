package dev.chieppa.stretch.test

import dev.chieppa.stretch.core.Applied
import dev.chieppa.stretch.core.Command
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private data class Count(val value: Int)

private data class Add(val amount: Int) : Command<Count> {
    override fun apply(document: Count) = Applied(Count(document.value + amount), Add(-amount))
}

private data class Set(val value: Int) : Command<Count> {
    override fun apply(document: Count) = Applied(Count(value), Set(document.value))
}

class ScenarioTest {
    @Test
    fun scenarioRunsAndReplays() {
        val result = scenario(Count(0)) {
            run(Add(2), Add(3))
            expect(Count(5))
            gesture(Set(7), Set(9))
            expect(Count(9))
            undo()
            expect(Count(5))
            redo()
        }
        assertEquals(Count(9), result)
    }

    @Test
    fun undoingPastTheStartFails() {
        assertFailsWith<AssertionError> { scenario(Count(0)) { undo() } }
    }

    @Test
    fun awaitSettledWaitsForTheCondition() = runTest {
        val settled = TestSettled(false)
        var done = false
        val job = launch {
            awaitSettled(settled)
            done = true
        }
        testScheduler.runCurrent()
        assertEquals(false, done)
        settled.set(true)
        job.join()
        assertTrue(done)
    }
}
