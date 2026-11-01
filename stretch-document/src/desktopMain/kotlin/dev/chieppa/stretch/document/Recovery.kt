package dev.chieppa.stretch.document

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import dev.chieppa.stretch.core.Steps
import dev.chieppa.stretch.frame.AppIdentity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.Properties
import java.util.UUID
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension

/**
 * Copies of open documents kept in [directory] while they have unsaved changes, so work survives a crash or power
 * cut. Each running document holds a [RecoverySlot]; a slot whose app is gone shows up in [pending] next time.
 * Other copies of the app that are still running keep their slots locked, so they are never offered.
 */
class Recovery(val directory: Path) {
    /** A slot for one open document. Close it when the document closes cleanly. */
    fun slot(): RecoverySlot = RecoverySlot(directory, UUID.randomUUID().toString())

    /** Documents left behind by a run that ended without saving them, newest first. */
    fun pending(): List<RecoveredDocument> {
        if (!directory.exists()) return emptyList()
        return directory.listDirectoryEntries("*.properties")
            .filter { !lockedElsewhere(directory.resolve("${it.nameWithoutExtension}.lock")) }
            .sortedByDescending { it.getLastModifiedTime() }
            .mapNotNull { meta ->
                val props = runCatching { Files.newInputStream(meta).use { s -> Properties().apply { load(s) } } }.getOrNull() ?: return@mapNotNull null
                RecoveredDocument(meta.nameWithoutExtension, props.getProperty("path"), props.getProperty("name"))
            }
    }

    /** The recovered document's contents, for [DocumentSession.restore] after [DocumentFormat.decode]. */
    fun read(item: RecoveredDocument): ByteArray = Files.readAllBytes(directory.resolve("${item.key}.data"))

    /** Forgets [item], as after restoring it into a session (which keeps its own slot) or discarding it. */
    fun discard(item: RecoveredDocument) = remove(directory, item.key)

    companion object {
        /** Recovery in the app's own data folder, named by its id from the `dev.chieppa.stretch.app` plugin. */
        fun forApp(id: String = AppIdentity.current?.id ?: "stretch-app"): Recovery = Recovery(appDataDirectory(id).resolve("Recovery"))
    }
}

/** One document's place in [Recovery]. It is locked while this run holds it. */
class RecoverySlot internal constructor(private val directory: Path, private val key: String) : AutoCloseable {
    private var channel: FileChannel? = null
    private var lock: FileLock? = null

    /** Stores [bytes] as the document's latest state, with its file [path] and [name] for the recovery list. */
    fun write(bytes: ByteArray, path: String?, name: String?) {
        Files.createDirectories(directory)
        if (lock == null) {
            channel = FileChannel.open(directory.resolve("$key.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
            lock = channel?.tryLock()
        }
        FileStorage.write(directory.resolve("$key.data").toString(), bytes)
        val props = Properties().apply {
            path?.let { setProperty("path", it) }
            name?.let { setProperty("name", it) }
        }
        val meta = directory.resolve("$key.properties")
        Files.newOutputStream(meta).use { props.store(it, null) }
    }

    /** Removes the stored copy, once the document is saved or its changes are thrown away. */
    fun clear() {
        remove(directory, key, keepLock = true)
    }

    override fun close() {
        remove(directory, key, keepLock = true)
        runCatching { lock?.release() }
        runCatching { channel?.close() }
        lock = null
        channel = null
        runCatching { directory.resolve("$key.lock").deleteIfExists() }
    }
}

private fun remove(directory: Path, key: String, keepLock: Boolean = false) {
    runCatching { directory.resolve("$key.properties").deleteIfExists() }
    runCatching { directory.resolve("$key.data").deleteIfExists() }
    if (!keepLock) runCatching { directory.resolve("$key.lock").deleteIfExists() }
}

/** Whether a running process, this one included, holds the lock at [path]. */
private fun lockedElsewhere(path: Path): Boolean {
    if (!path.exists()) return false
    return try {
        FileChannel.open(path, StandardOpenOption.WRITE).use { channel ->
            val lock = channel.tryLock() ?: return true
            lock.release()
            false
        }
    } catch (_: OverlappingFileLockException) {
        true
    } catch (_: java.io.IOException) {
        true
    }
}

/** Where the app keeps its own files: the local app data folder on Windows, Application Support on macOS, the XDG state folder on Linux. */
fun appDataDirectory(id: String): Path {
    val os = System.getProperty("os.name").orEmpty()
    val home = Path.of(System.getProperty("user.home"))
    return when {
        os.startsWith("Windows") -> Path.of(System.getenv("LOCALAPPDATA") ?: home.resolve("AppData/Local").toString()).resolve(id)
        os.startsWith("Mac") -> home.resolve("Library/Application Support").resolve(id)
        else -> (System.getenv("XDG_STATE_HOME")?.let(Path::of) ?: home.resolve(".local/state")).resolve(id)
    }
}

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
                if (copied != null) withContext(Dispatchers.IO) { slot.clear() }
                copied = null
                continue
            }
            if (copied === steps && copiedHistory === s.history) continue
            val bytes = s.format.encode(s.history.document.value)
            withContext(Dispatchers.IO) { runCatching { slot.write(bytes, s.path, s.fileName) } }
            copied = steps
            copiedHistory = s.history
        }
    }
}
