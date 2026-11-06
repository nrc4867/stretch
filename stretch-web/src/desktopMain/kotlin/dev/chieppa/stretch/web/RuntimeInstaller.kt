package dev.chieppa.stretch.web

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlinx.coroutines.future.await
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest
import java.time.Duration

private const val HTTP_OK = 200
private const val CONNECT_MILLIS = 30_000L
private const val MOVE_ATTEMPTS = 5
private const val MOVE_RETRY_MILLIS = 500L
private const val RUNTIME_SEARCH_DEPTH = 5

/** The Java home inside [folder], which holds `bin/java`: at its top on Windows and Linux, in `Contents/Home` on macOS. */
internal fun javaHomeIn(folder: File): File? = folder.walk().maxDepth(RUNTIME_SEARCH_DEPTH).firstOrNull { dir ->
    dir.isDirectory && (File(dir, "bin/java").isFile || File(dir, "bin/java.exe").isFile)
}

/**
 * Fetches [download] into [folder], shared by every Stretch app of this user, and unpacks it there. Each step stops
 * when its coroutine is cancelled, and whatever was fetched goes; only a whole, checked runtime is ever left in place.
 */
internal class RuntimeInstaller(val download: RuntimeDownload, private val folder: File) {
    private val target get() = File(folder, download.name)

    /** The Java home of the installed runtime, or null before an install. */
    fun installedHome(): File? = if (target.isDirectory) javaHomeIn(target) else null

    /** Fetches, checks and unpacks the runtime, reporting the bytes fetched so far to [onProgress]. */
    suspend fun install(onProgress: (Long) -> Unit) {
        if (installedHome() != null) return
        val work = File(folder, "${download.name}.${ProcessHandle.current().pid()}.partial")
        try {
            withContext(Dispatchers.IO) {
                work.deleteRecursively()
                work.mkdirs()
            }
            val archive = File(work, "runtime.tar.gz")
            fetch(archive, onProgress)
            val unpacked = File(work, "runtime")
            unpack(archive, unpacked)
            checkNotNull(javaHomeIn(unpacked)) { "The download holds no Java runtime" }
            moveIntoPlace(unpacked)
        } finally {
            withContext(NonCancellable + Dispatchers.IO) { work.deleteRecursively() }
        }
    }

    /** Streams the archive into [into] without blocking a thread, hashing as it goes; cancelling the caller stops it. */
    private suspend fun fetch(into: File, onProgress: (Long) -> Unit) {
        val digest = MessageDigest.getInstance("SHA-512")
        val sink = ArchiveSink(into, digest, onProgress)
        val client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofMillis(CONNECT_MILLIS)).build()
        val request = HttpRequest.newBuilder(URI(download.url)).build()
        val handler = HttpResponse.BodyHandler { info ->
            if (info.statusCode() == HTTP_OK) HttpResponse.BodySubscribers.fromSubscriber(sink) else HttpResponse.BodySubscribers.discarding()
        }
        try {
            val response = client.sendAsync(request, handler).await()
            check(response.statusCode() == HTTP_OK) { "The download failed: HTTP ${response.statusCode()}" }
            sink.done.await()
        } finally {
            sink.stop()
            client.shutdownNow()
        }
        val hash = digest.digest().joinToString("") { "%02x".format(it) }
        check(hash.equals(download.sha512, ignoreCase = true)) { "The download does not match its checksum" }
    }

    /** Unpacks with the OS's own `tar`, which keeps the links and permissions a macOS runtime needs. */
    private suspend fun unpack(archive: File, into: File) {
        withContext(Dispatchers.IO) { into.mkdirs() }
        val process = ProcessBuilder(tarProgram(), "-xzf", archive.absolutePath, "-C", into.absolutePath).redirectErrorStream(true).start()
        val output = stoppable(process::destroyForcibly) { process.inputStream.bufferedReader().readText().also { process.waitFor() } }
        check(process.exitValue() == 0) { "Unpacking failed: ${output.take(MAX_REASON_CHARS)}" }
    }

    /** Moves [unpacked] to its place, retrying while a virus scanner still holds a file. Another app may have got there first. */
    private suspend fun moveIntoPlace(unpacked: File) {
        repeat(MOVE_ATTEMPTS) {
            if (installedHome() != null || unpacked.renameTo(target)) return
            delay(MOVE_RETRY_MILLIS)
        }
        error("Could not move the runtime into $folder")
    }
}

private const val MAX_REASON_CHARS = 300

/** Windows' own `tar`, rather than any other on the path, which may read `C:` as a remote host. */
private fun tarProgram(): String =
    if (System.getProperty("os.name").orEmpty().startsWith("Windows")) File(System.getenv("SystemRoot") ?: "C:\\Windows", "System32\\tar.exe").path
    else "tar"

/** Runs the blocking [block] off the caller's thread, calling [stop] if the caller is cancelled, which ends what it blocks on. */
private suspend fun <T> stoppable(stop: () -> Unit, block: () -> T): T = coroutineScope {
    val watcher = launch(start = CoroutineStart.UNDISPATCHED) {
        try {
            awaitCancellation()
        } finally {
            stop()
        }
    }
    try {
        withContext(Dispatchers.IO) {
            ensureActive()
            block()
        }
    } finally {
        watcher.cancel()
    }
}
