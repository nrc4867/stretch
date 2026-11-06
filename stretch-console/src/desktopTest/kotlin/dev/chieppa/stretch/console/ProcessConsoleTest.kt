package dev.chieppa.stretch.console

import kotlinx.coroutines.async
import kotlinx.coroutines.yield
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Runs small Java programs through the test's own runtime, so it works the same on every OS. */
class ProcessConsoleTest {
    private val java = File(System.getProperty("java.home"), "bin/java").path

    private fun program(name: String, body: String): File {
        val folder = File(System.getProperty("java.io.tmpdir"), "stretch-console-test").apply { mkdirs() }
        return File(folder, "$name.java").apply { writeText("public class $name { public static void main(String[] a) throws Exception { $body } }") }
    }

    @Test
    fun streamsBothOutputsAndReturnsTheExitCode() = runBlocking {
        val source = program(
            "Greets",
            """System.out.println("\u001B[32mhello\u001B[0m " + System.getenv("GREETING") + " in " + new java.io.File(".").getCanonicalFile().getName());
               System.out.flush(); System.err.println("problem"); System.exit(3);""",
        )
        val folder = File(System.getProperty("java.io.tmpdir"), "stretch-console-cwd").apply { mkdirs() }
        val state = ConsoleState()
        val code = state.runProcess(ConsoleCommand(listOf(java, source.path), folder, mapOf("GREETING" to "there")))
        assertEquals(3, code)
        val out = state.lines.first { it.text.startsWith("hello") }
        assertEquals("hello there in stretch-console-cwd", out.text)
        assertEquals(AnsiColor.Indexed(2), out.spans.first().style.foreground)
        val err = state.lines.first { it.text == "problem" }
        assertEquals(ConsoleStream.Err, err.spans.single().stream)
    }

    @Test
    fun cancellingEndsTheProcess() = runBlocking {
        val source = program("Waits", """System.out.println("started"); System.out.flush(); Thread.sleep(600_000);""")
        val state = ConsoleState()
        val run = async { state.runProcess(ConsoleCommand(listOf(java, source.path))) }
        // Output arrives on this thread, so yielding lets it in until the line is there.
        while (state.lines.none { it.text == "started" }) yield()
        run.cancel()
        // The run only finishes once the process's output closes, which happens when it has ended.
        run.join()
        assertTrue(run.isCancelled)
    }
}
