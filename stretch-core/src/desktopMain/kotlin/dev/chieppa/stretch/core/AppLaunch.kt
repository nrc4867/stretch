package dev.chieppa.stretch.core

import java.io.File
import java.lang.management.ManagementFactory
import kotlin.system.exitProcess

/** JVM options that tie a process to one debugger port, so a new process leaves them out. */
private val singleProcessOptions = listOf("-agentlib:jdwp", "-Xrunjdwp", "-Xdebug")

/** Marks a process Stretch started again, so it never starts another for the same reason. */
private const val RESTARTED_PROPERTY = "stretch.restarted"

/**
 * How this app was started, so Stretch can start it again: on another Java runtime, or from a launcher the app makes
 * for itself. Call [remember] first in `main` with its arguments. It covers apps on the classpath, started with `-jar`
 * or a main class: as plain `java`, from an IDE or from a packaged launcher.
 */
object AppLaunch {
    @Volatile
    private var arguments: List<String> = emptyList()

    /** Keeps [args], the app's own arguments, for starting it again. */
    fun remember(args: Array<String>) {
        arguments = args.toList()
    }

    /** Whether Stretch started this process again, from another one. */
    val restarted: Boolean get() = System.getProperty(RESTARTED_PROPERTY) != null

    /** Whether this is a packaged app, which starts through its own launcher. */
    val packaged: Boolean get() = System.getProperty("jpackage.app-path") != null

    /** The Java runtime running this app. */
    val javaHome: File get() = File(System.getProperty("java.home"))

    /**
     * The command that starts this app on the runtime at [home], with this process's options and arguments. With
     * [markRestarted], the new process knows Stretch started it, and never starts another for the same reason.
     */
    fun command(home: File = javaHome, windowed: Boolean = true, markRestarted: Boolean = true): List<String> = launchCommand(
        java = javaIn(home, windowed).path,
        options = ManagementFactory.getRuntimeMXBean().inputArguments.filterNot { it.startsWith("-D$RESTARTED_PROPERTY") } +
            listOfNotNull("-D$RESTARTED_PROPERTY=true".takeIf { markRestarted }),
        classPath = System.getProperty("java.class.path").orEmpty(),
        command = System.getProperty("sun.java.command").orEmpty(),
        arguments = arguments,
    )

    /**
     * Starts the app again on the runtime at [home]. With [waitAndExit], this process waits for the new one and exits
     * with its code, for startup, where a terminal or IDE may be watching this one; otherwise it exits at once.
     */
    fun startAgain(home: File, waitAndExit: Boolean): Nothing {
        val child = ProcessBuilder(command(home, windowed = !onConsoleJava())).inheritIO().start()
        if (!waitAndExit) exitProcess(0)
        Runtime.getRuntime().addShutdownHook(Thread { child.destroy() })
        exitProcess(child.waitFor())
    }

    /** Whether this process is a console `java.exe` on Windows, rather than `javaw.exe` or a launcher. */
    private fun onConsoleJava(): Boolean =
        ProcessHandle.current().info().command().map { File(it).name.equals("java.exe", ignoreCase = true) }.orElse(false)
}

/** The Java program in the runtime at [home]: `javaw.exe` for a windowed app on Windows, which opens no console. */
fun javaIn(home: File, windowed: Boolean = true): File {
    val bin = File(home, "bin")
    val windows = File(bin, "java.exe").isFile
    return when {
        windows && windowed && File(bin, "javaw.exe").isFile -> File(bin, "javaw.exe")
        windows -> File(bin, "java.exe")
        else -> File(bin, "java")
    }
}

/**
 * The command that starts an app: [java], its JVM [options] without debugger hooks, then `-jar` when [command] (the
 * JVM's record of what it ran) starts with a lone jar on [classPath], or the class path and main class otherwise,
 * then the app's [arguments].
 */
internal fun launchCommand(java: String, options: List<String>, classPath: String, command: String, arguments: List<String>): List<String> {
    val kept = options.filterNot { option -> singleProcessOptions.any { option.startsWith(it) } }
    val jar = classPath.takeIf { File.pathSeparator !in it && it.endsWith(".jar", ignoreCase = true) && command.startsWith(it) }
    val target = if (jar != null) listOf("-jar", jar) else listOf("-cp", classPath, command.substringBefore(' '))
    return listOf(java) + kept + target + arguments
}
