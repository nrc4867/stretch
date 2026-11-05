package dev.chieppa.stretch.gradle

import org.gradle.api.Project
import org.jetbrains.compose.desktop.application.tasks.AbstractJLinkTask
import java.io.File

private const val JLINK_TASK = "createRuntimeImage"
private const val DISTRIBUTABLE_TASK = "createDistributable"

/** What Chromium needs beside its libraries in a JetBrains Runtime's `bin` folder. */
private val engineFiles = listOf(
    Regex("""jcef_helper.*"""),
    Regex("""cef_server.*"""),
    Regex(""".*\.pak"""),
    Regex("""icudtl\.dat"""),
    Regex(""".*snapshot.*\.bin"""),
    Regex("""locales"""),
)

/** Copies what [engineFiles] names from [from] to [to], keeping what is already there. */
internal fun copyEngineFiles(from: File, to: File) {
    val names = from.list().orEmpty().filter { name -> engineFiles.any { it.matches(name) } }
    for (name in names) File(from, name).copyRecursively(File(to, name), overwrite = true)
}

/**
 * Compose builds its runtime without a JDK's commands. That also takes Chromium's helpers and data, which sit among
 * them in `bin`, so they go back in from [javaHome]. A macOS JetBrains Runtime keeps them all in a `Frameworks` folder
 * beside its home instead, which `jlink` knows nothing of, so that goes into the finished app at the same place.
 */
internal fun restoreEngineFiles(project: Project, javaHome: String) {
    project.tasks.withType(AbstractJLinkTask::class.java).matching { it.name == JLINK_TASK }.configureEach {
        val runtime = destinationDir.asFile
        doLast { copyEngineFiles(File(javaHome, "bin"), File(runtime.get(), "bin")) }
    }
    val distributions = project.layout.buildDirectory.dir("compose/binaries/main/app").map { it.asFile }
    project.tasks.matching { it.name == DISTRIBUTABLE_TASK }.configureEach {
        doLast { copyMacFrameworks(File(javaHome), distributions.get()) }
    }
}

/** Copies a macOS runtime's `Frameworks` beside the home of each app under [apps]. Other runtimes have none, so this does nothing. */
internal fun copyMacFrameworks(javaHome: File, apps: File) {
    val frameworks = javaHome.parentFile?.resolve("Frameworks")?.takeIf { it.isDirectory } ?: return
    for (app in apps.listFiles().orEmpty().filter { it.name.endsWith(".app") }) {
        frameworks.copyRecursively(app.resolve("Contents/runtime/Contents/Frameworks"), overwrite = true)
    }
}
