package dev.chieppa.stretch.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private data class Doc(val size: Int = 0, val name: String = "")

private data class SetSize(val size: Int) : Command<Doc> {
    override fun apply(document: Doc) = Applied(document.copy(size = size), SetSize(document.size))
}

private data class Rename(val name: String) : Command<Doc> {
    override fun apply(document: Doc) = Applied(document.copy(name = name), Rename(document.name))
}

class HistoryTest {
    @Test
    fun executeUndoRedo() {
        val history = History(Doc())
        history.execute(SetSize(3))
        history.execute(Rename("ear"))
        assertEquals(Doc(3, "ear"), history.document.value)

        assertTrue(history.undo())
        assertEquals(Doc(3, ""), history.document.value)
        assertTrue(history.undo())
        assertEquals(Doc(), history.document.value)
        assertFalse(history.undo())

        assertTrue(history.redo())
        assertTrue(history.redo())
        assertEquals(Doc(3, "ear"), history.document.value)
        assertFalse(history.redo())
    }

    @Test
    fun newCommandClearsRedo() {
        val history = History(Doc())
        history.execute(SetSize(1))
        history.undo()
        history.execute(SetSize(2))
        assertFalse(history.canRedo)
        history.undo()
        assertEquals(Doc(), history.document.value)
    }

    @Test
    fun gestureIsOneUndoStep() {
        val history = History(Doc(size = 1))
        val gesture = history.begin()
        gesture.update(SetSize(4))
        assertEquals(4, history.document.value.size)
        gesture.update(SetSize(9))
        assertEquals(9, history.document.value.size)
        gesture.commit()

        history.undo()
        assertEquals(1, history.document.value.size)
        history.redo()
        assertEquals(9, history.document.value.size)
    }

    @Test
    fun cancelledGestureRestoresStartAndLeavesNoStep() {
        val history = History(Doc(size = 1))
        val gesture = history.begin()
        gesture.update(SetSize(5))
        gesture.cancel()
        assertEquals(1, history.document.value.size)
        assertFalse(history.canUndo)
    }

    @Test
    fun commandsAreRefusedWhileAGestureIsOpen() {
        val history = History(Doc())
        val gesture = history.begin()
        assertFailsWith<IllegalStateException> { history.execute(SetSize(1)) }
        gesture.commit()
        history.execute(SetSize(1))
    }

    @Test
    fun limitDropsOldestSteps() {
        val history = History(Doc(), limit = 2)
        (1..3).forEach { history.execute(SetSize(it)) }
        assertTrue(history.undo())
        assertTrue(history.undo())
        assertFalse(history.undo())
        assertEquals(1, history.document.value.size)
    }

    @Test
    fun logReplaysToTheSameDocument() {
        val history = History(Doc())
        history.execute(SetSize(2))
        history.execute(Rename("tail"))
        history.undo()
        val gesture = history.begin()
        gesture.update(SetSize(7))
        gesture.commit()
        history.undo()
        history.redo()

        assertEquals(history.document.value, history.log.replay(Doc()))
    }
}
