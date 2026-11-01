package dev.chieppa.stretch.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The document, its undo and redo stacks and the session log. Commands are applied in order by one writer.
 */
class History<D>(initial: D, private val limit: Int = 500) {
    private class Entry<D>(val command: Command<D>, val undo: Command<D>)

    private val state = MutableStateFlow(initial)
    private val undoStack = ArrayDeque<Entry<D>>()
    private val redoStack = ArrayDeque<Entry<D>>()
    private var open: Transaction? = null

    val document: StateFlow<D> = state.asStateFlow()
    val log = SessionLog<D>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun execute(command: Command<D>) {
        checkNoTransaction()
        val applied = command.apply(state.value)
        state.value = applied.document
        push(Entry(command, applied.undo))
        log.add(LogEntry.Executed(command))
    }

    fun undo(): Boolean {
        checkNoTransaction()
        val entry = undoStack.removeLastOrNull() ?: return false
        state.value = entry.undo.apply(state.value).document
        redoStack.addLast(entry)
        log.add(LogEntry.Undo)
        return true
    }

    fun redo(): Boolean {
        checkNoTransaction()
        val entry = redoStack.removeLastOrNull() ?: return false
        val applied = entry.command.apply(state.value)
        state.value = applied.document
        undoStack.addLast(Entry(entry.command, applied.undo))
        log.add(LogEntry.Redo)
        return true
    }

    /**
     * Starts a gesture. Each [Transaction.update] replaces the previous preview and applies to the document as it
     * was when the gesture began; [Transaction.commit] makes one undo step and [Transaction.cancel] restores the start.
     */
    fun begin(): Transaction {
        checkNoTransaction()
        return Transaction(state.value).also { open = it }
    }

    private fun checkNoTransaction() = check(open == null) { "A transaction is open" }

    private fun push(entry: Entry<D>) {
        undoStack.addLast(entry)
        if (undoStack.size > limit) undoStack.removeFirst()
        redoStack.clear()
    }

    inner class Transaction internal constructor(private val base: D) {
        private var current: Applied<D>? = null
        private var command: Command<D>? = null
        private var ended = false

        fun update(command: Command<D>) {
            check(!ended) { "The transaction has ended" }
            val applied = command.apply(base)
            state.value = applied.document
            this.command = command
            current = applied
        }

        fun commit() {
            end()
            val command = command ?: return
            push(Entry(command, checkNotNull(current).undo))
            log.add(LogEntry.Executed(command))
        }

        fun cancel() {
            end()
            state.value = base
        }

        private fun end() {
            check(!ended) { "The transaction has ended" }
            ended = true
            open = null
        }
    }
}
