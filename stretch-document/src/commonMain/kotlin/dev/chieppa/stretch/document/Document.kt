package dev.chieppa.stretch.document

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.chieppa.stretch.core.Command
import dev.chieppa.stretch.core.History
import dev.chieppa.stretch.core.Steps

/** How an app's documents turn into bytes and back, and the file type they are saved as. */
interface DocumentFormat<D> {
    /** The file extension documents are saved with, without the dot. */
    val extension: String

    /**
     * Every extension [decode] reads, without the dots: [extension] and any others the format can take in but does not save.
     * The open dialog lists these.
     */
    val openExtensions: Set<String> get() = setOf(extension)

    /** The file type as the open and save dialogs name it, such as "Squash character". */
    val description: String

    fun encode(document: D): ByteArray

    fun decode(bytes: ByteArray): D

    /** Whether [path] is a file of this format's own type, which saving may write over; a file it only reads is not. */
    fun saves(path: String): Boolean =
        path.substringAfterLast('/').substringAfterLast('\\').substringAfterLast('.', "").equals(extension, ignoreCase = true)
}

/**
 * Where documents are read and written: the platform's [documentStorage], something in memory in tests. Its calls may
 * come from the UI thread; an implementation that blocks moves that work off it.
 */
interface DocumentStorage {
    suspend fun read(path: String): ByteArray

    /** Replaces the file at [path] as a whole: a failed write leaves the old file as it was. */
    suspend fun write(path: String, bytes: ByteArray)

    suspend fun exists(path: String): Boolean

    /** The file's name as people see it, without its folder or extension. */
    fun displayName(path: String): String
}

/** Asks where to open or save, with the platform's own dialogs. Null when the person cancels. */
interface FileChooser {
    suspend fun chooseOpen(format: DocumentFormat<*>): String?

    suspend fun chooseSave(format: DocumentFormat<*>, suggestedName: String): String?
}

/** Documents where this platform keeps them. */
expect val documentStorage: DocumentStorage

/** Open and save dialogs for the app's window or page, with titles in the app's language. */
@Composable
expect fun rememberFileChooser(): FileChooser

/**
 * One open document: its undo [history], the file it lives in, and whether it has changes that are not saved. The
 * app edits through [history] as before; the session reads and writes files through [storage] and keeps track of
 * what was saved, so undoing back to the saved state makes it clean again. Opening or starting a new document swaps
 * the history for a fresh one. Pass [history] when something else already owns the document's undo.
 */
@Stable
class DocumentSession<D>(
    val format: DocumentFormat<D>,
    initial: D,
    private val storage: DocumentStorage,
    path: String? = null,
    history: History<D> = History(initial),
) {
    var history by mutableStateOf(history)
        private set

    /** The file this document was opened from or last saved to; null until it is first saved. */
    var path by mutableStateOf(path)
        private set

    private var saved by mutableStateOf<Command<D>?>(null)
    private var savedSize by mutableStateOf(0)
    private var restored by mutableStateOf(false)

    /** A close or switch waiting on the person's answer to the unsaved-changes prompt. */
    internal var pending by mutableStateOf<(() -> Unit)?>(null)

    /** The file's name, or null for a document never saved, which the app shows as "Untitled". */
    val fileName: String? get() = path?.let(storage::displayName)

    fun isEdited(steps: Steps<D>): Boolean = restored || steps.done.size != savedSize || steps.done.lastOrNull() !== saved

    /** Whether there are changes since the last save, at this moment. Composables use [rememberEdited]. */
    val edited: Boolean get() = isEdited(history.steps.value)

    /**
     * Saves to the document's file, asking for one with [chooser] the first time, and when the document came from a file of a type
     * the format only reads, which is left as it is. False when the person cancels.
     */
    suspend fun save(chooser: FileChooser): Boolean {
        val target = path?.takeIf(format::saves) ?: chooser.chooseSave(format, fileName ?: "") ?: return false
        saveTo(target)
        return true
    }

    /** Saves to a file the person picks with [chooser], which becomes the document's file. */
    suspend fun saveAs(chooser: FileChooser): Boolean {
        val target = chooser.chooseSave(format, fileName ?: "") ?: return false
        saveTo(target)
        return true
    }

    /** Writes the document to [target], which becomes its file. Errors from the format or storage are thrown. */
    suspend fun saveTo(target: String) {
        val document = history.document.value
        val steps = history.steps.value
        val bytes = format.encode(document)
        storage.write(target, bytes)
        path = target
        saved = steps.done.lastOrNull()
        savedSize = steps.done.size
        restored = false
    }

    /** Reads [target] and makes it the open document, with a fresh history. */
    suspend fun open(target: String) {
        val bytes = storage.read(target)
        replace(format.decode(bytes), target, edited = false)
    }

    /** Starts a new, never-saved document. */
    fun new(document: D) = replace(document, null, edited = false)

    /** Takes back a document recovered after a crash: it opens with its old file, marked as having unsaved changes. */
    fun restore(document: D, path: String?) = replace(document, path, edited = true)

    private fun replace(document: D, file: String?, edited: Boolean) {
        history = History(document)
        path = file
        saved = null
        savedSize = 0
        restored = edited
        pending = null
    }

    /**
     * Runs [then] once the document's changes are safe: at once when there are none, otherwise after the person
     * answers the [UnsavedChangesDialog] by saving or by choosing not to. Use it before closing the window,
     * opening another file or starting a new one.
     */
    fun whenSaved(then: () -> Unit) {
        if (edited) pending = then else then()
    }
}

/** Whether [session] has unsaved changes, recomposing when that changes. */
@Composable
fun <D> rememberEdited(session: DocumentSession<D>): Boolean {
    val steps by session.history.steps.collectAsState()
    return session.isEdited(steps)
}
