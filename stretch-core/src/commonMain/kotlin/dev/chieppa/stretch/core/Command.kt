package dev.chieppa.stretch.core

/**
 * A change to a document of type [D]. Applying it returns the new document and the command that undoes it.
 */
fun interface Command<D> {
    fun apply(document: D): Applied<D>
}

data class Applied<D>(val document: D, val undo: Command<D>)
