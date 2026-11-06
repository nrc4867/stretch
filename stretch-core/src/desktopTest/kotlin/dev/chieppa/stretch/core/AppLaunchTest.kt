package dev.chieppa.stretch.core

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class AppLaunchTest {
    private val sep = File.pathSeparator

    @Test
    fun aJarStartsWithJarAndKeepsItsArgumentsButNotTheDebugger() {
        val command = launchCommand(
            "java", listOf("-Xmx1g", "-agentlib:jdwp=transport=dt_socket"), "/My Apps/app.jar", "/My Apps/app.jar --open a b", listOf("--open", "a b"),
        )
        assertEquals(listOf("java", "-Xmx1g", "-jar", "/My Apps/app.jar", "--open", "a b"), command)
    }

    @Test
    fun aClassPathStartsWithItsMainClass() {
        val command = launchCommand("java", emptyList(), "a.jar${sep}b.jar", "dev.app.MainKt --x", listOf("--x"))
        assertEquals(listOf("java", "-cp", "a.jar${sep}b.jar", "dev.app.MainKt", "--x"), command)
    }

    @Test
    fun windowsGetsTheWindowedJavaWhenThereIsOne() {
        val home = createTempDirectory("app-launch").toFile()
        File(home, "bin").mkdirs()
        File(home, "bin/java.exe").writeText("")
        File(home, "bin/javaw.exe").writeText("")
        assertEquals("javaw.exe", javaIn(home).name)
        assertEquals("java.exe", javaIn(home, windowed = false).name)
        home.deleteRecursively()
    }
}
