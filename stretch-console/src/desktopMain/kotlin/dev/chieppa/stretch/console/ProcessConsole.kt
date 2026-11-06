package dev.chieppa.stretch.console

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/** How many characters a read takes from a process's output at most. */
private const val READ_CHARS = 8192

/** How long a process has to end after being asked before it is made to. */
private const val GRACE_MILLIS = 2000L

/**
 * A process to start: [command] is the program and its arguments, each passed as it is, with no shell between. It
 * runs in [workingDirectory], or this process's own, with [environment] over this process's environment, or in place
 * of it when not [inheritEnvironment].
 */
data class ConsoleCommand(
    val command: List<String>,
    val workingDirectory: File? = null,
    val environment: Map<String, String> = emptyMap(),
    val inheritEnvironment: Boolean = true,
) {
    init {
        require(command.isNotEmpty()) { "A command needs a program" }
    }
}

/**
 * Starts [command] and streams its standard output and error into this console as they come, returning its exit code.
 * Output arrives on the coroutine context this is called from, so call it from the UI's, as a scope from
 * `rememberCoroutineScope` gives. Cancelling ends the process and every process it started. Throws [java.io.IOException]
 * when the program cannot start.
 */
suspend fun ConsoleState.runProcess(command: ConsoleCommand): Int = coroutineScope {
    val builder = ProcessBuilder(command.command).apply {
        command.workingDirectory?.let(::directory)
        if (!command.inheritEnvironment) environment().clear()
        environment().putAll(command.environment)
    }
    val process = runInterruptible(Dispatchers.IO) { builder.start() }
    try {
        process.outputStream.close()
        val chunks = Channel<ConsoleChunk>(Channel.UNLIMITED)
        val readers = listOf(
            launch(Dispatchers.IO) { pump(process.inputStream, ConsoleStream.Out, chunks) },
            launch(Dispatchers.IO) { pump(process.errorStream, ConsoleStream.Err, chunks) },
        )
        launch {
            readers.joinAll()
            chunks.close()
        }
        for (chunk in chunks) append(chunk.text, chunk.stream)
        runInterruptible(Dispatchers.IO) { process.waitFor() }
    } finally {
        if (process.isAlive) withContext(NonCancellable + Dispatchers.IO) { end(process) }
    }
}

/** Sends what [stream] gives as [kind] chunks until it ends. UTF-8 characters split across reads stay whole. */
private fun pump(stream: InputStream, kind: ConsoleStream, into: SendChannel<ConsoleChunk>) {
    InputStreamReader(stream, Charsets.UTF_8).use { reader ->
        val buffer = CharArray(READ_CHARS)
        while (true) {
            val count = runCatching { reader.read(buffer) }.getOrDefault(-1)
            if (count < 0) break
            if (count > 0) into.trySend(ConsoleChunk(String(buffer, 0, count), kind))
        }
    }
}

/** Asks [process] and everything it started to end, then makes them after a grace period. */
private fun end(process: Process) {
    val family = process.descendants().toList()
    family.forEach { it.destroy() }
    process.destroy()
    if (!process.waitFor(GRACE_MILLIS, TimeUnit.MILLISECONDS)) {
        family.filter { it.isAlive }.forEach { it.destroyForcibly() }
        process.destroyForcibly()
    }
}
