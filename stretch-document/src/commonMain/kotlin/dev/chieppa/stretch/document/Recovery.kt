package dev.chieppa.stretch.document

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import dev.chieppa.stretch.core.Steps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Copies of open documents kept while they have unsaved changes, so work survives a crash or power cut. Each running
 * document holds a [RecoverySlot]; a slot whose app is gone shows up in [pending] next time. Other copies of the app
 * that are still running keep their slots locked, so they are never offered.
 */
interface Recovery {
    /** A slot for one open document. Close it when the document closes cleanly. */
    fun slot(): RecoverySlot

    /** Documents left behind by a run that ended without saving them, newest first. */
    suspend fun pending(): List<RecoveredDocument>

    /** The recovered document's contents, for [DocumentSession.restore] after [DocumentFormat.decode]. */
    suspend fun read(item: RecoveredDocument): ByteArray

    /** Forgets [item], as after restoring it into a session (which keeps its own slot) or discarding it. */
    suspend fun discard(item: RecoveredDocument)

    companion object
}

/** One document's place in [Recovery]. It is locked while this run holds it. */
interface RecoverySlot : AutoCloseable {
    /** Stores [bytes] as the document's latest state, with its file [path] and [name] for the recovery list. */
    suspend fun write(bytes: ByteArray, path: String?, name: String?)

    /** Removes the stored copy, once the document is saved or its changes are thrown away. */
    suspend fun clear()
}

/**
 * Recovery in the app's own storage, named by [id]. Without one it uses the app's id from the
 * `dev.chieppa.stretch.app` plugin where the platform has it.
 */
expect fun Recovery.Companion.forApp(id: String? = null): Recovery

/**
 * Keeps [session]'s recovery copy current: every [intervalMillis] while it has changes not yet copied, and cleared
 * as soon as it is saved. The slot closes, and its copy goes, when this leaves the composition, which is when the
 * document closes cleanly.
 */
@Composable
fun <D> Autosave(session: DocumentSession<D>, recovery: Recovery, intervalMillis: Long = 30_000) {
    val slot = remember(recovery) { recovery.slot() }
    DisposableEffect(slot) { onDispose { slot.close() } }
    val current by rememberUpdatedState(session)
    LaunchedEffect(slot, intervalMillis) {
        var copied: Steps<D>? = null
        var copiedHistory: Any? = null
        while (true) {
            delay(intervalMillis)
            val s = current
            val steps = s.history.steps.value
            if (!s.isEdited(steps)) {
                if (copied != null) withContext(Dispatchers.Default) { slot.clear() }
                copied = null
                continue
            }
            if (copied === steps && copiedHistory === s.history) continue
            val bytes = s.format.encode(s.history.document.value)
            withContext(Dispatchers.Default) { runCatching { slot.write(bytes, s.path, s.fileName) } }
            copied = steps
            copiedHistory = s.history
        }
    }
}

/** Warns before the app is left with [edited] changes where the platform can't ask through [UnsavedChangesDialog]. */
@Composable
expect fun LeaveGuard(edited: Boolean)
