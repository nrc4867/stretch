package dev.chieppa.stretch.document

import androidx.compose.runtime.Composable
import dev.chieppa.stretch.frame.AppIdentity
import kotlinx.coroutines.Dispatchers
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

/** [Recovery] in the files of [directory], with a file lock per slot, read and written on the IO dispatcher. */
class FileRecovery(val directory: Path) : Recovery {
    override fun slot(): RecoverySlot = FileRecoverySlot(directory, UUID.randomUUID().toString())

    override suspend fun pending(): List<RecoveredDocument> = withContext(Dispatchers.IO) {
        if (!directory.exists()) return@withContext emptyList()
        directory.listDirectoryEntries("*.properties")
            .filter { !lockedElsewhere(directory.resolve("${it.nameWithoutExtension}.lock")) }
            .sortedByDescending { it.getLastModifiedTime() }
            .mapNotNull { meta ->
                val props = runCatching { Files.newInputStream(meta).use { s -> Properties().apply { load(s) } } }.getOrNull() ?: return@mapNotNull null
                RecoveredDocument(meta.nameWithoutExtension, props.getProperty("path"), props.getProperty("name"))
            }
    }

    override suspend fun read(item: RecoveredDocument): ByteArray = withContext(Dispatchers.IO) { Files.readAllBytes(directory.resolve("${item.key}.data")) }

    override suspend fun discard(item: RecoveredDocument) = withContext(Dispatchers.IO) { remove(directory, item.key) }
}

/** Recovery in the app's own data folder. */
actual fun Recovery.Companion.forApp(id: String?): Recovery =
    FileRecovery(appDataDirectory(id ?: AppIdentity.current?.id ?: DEFAULT_APP_ID).resolve(RECOVERY_FOLDER))

private const val DEFAULT_APP_ID = "stretch-app"
private const val RECOVERY_FOLDER = "Recovery"

private class FileRecoverySlot(private val directory: Path, private val key: String) : RecoverySlot {
    private var channel: FileChannel? = null
    private var lock: FileLock? = null

    override suspend fun write(bytes: ByteArray, path: String?, name: String?) = withContext(Dispatchers.IO) {
        Files.createDirectories(directory)
        if (lock == null) {
            channel = FileChannel.open(directory.resolve("$key.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
            lock = channel?.tryLock()
        }
        FileStorage.writeFile(directory.resolve("$key.data").toString(), bytes)
        val props = Properties().apply {
            path?.let { setProperty("path", it) }
            name?.let { setProperty("name", it) }
        }
        val meta = directory.resolve("$key.properties")
        Files.newOutputStream(meta).use { props.store(it, null) }
    }

    override suspend fun clear() = withContext(Dispatchers.IO) { remove(directory, key, keepLock = true) }

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

/** A desktop app asks through [UnsavedChangesDialog] before its window closes. */
@Composable
actual fun LeaveGuard(edited: Boolean) = Unit
