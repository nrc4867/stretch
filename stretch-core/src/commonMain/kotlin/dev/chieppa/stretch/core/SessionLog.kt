package dev.chieppa.stretch.core

sealed interface LogEntry<out D> {
    data class Executed<D>(val command: Command<D>) : LogEntry<D>
    data object Undo : LogEntry<Nothing>
    data object Redo : LogEntry<Nothing>
}

/** Every command applied in a session, in order, so the session can be replayed. */
class SessionLog<D> {
    private val items = mutableListOf<LogEntry<D>>()

    val entries: List<LogEntry<D>> get() = items

    internal fun add(entry: LogEntry<D>) {
        items += entry
    }

    fun replay(initial: D): D {
        val history = History(initial, limit = Int.MAX_VALUE)
        for (entry in items) {
            when (entry) {
                is LogEntry.Executed -> history.execute(entry.command)
                LogEntry.Undo -> history.undo()
                LogEntry.Redo -> history.redo()
            }
        }
        return history.document.value
    }
}
