package dev.chieppa.stretch.build

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Regenerates the Java bindings for wgpu-native's headers with jextract, for the symbols listed in [includes]. The
 * output is checked in, so this only runs by hand after the wgpu-native version or the symbol list changes, with
 * jextract's install folder in [jextractHome]. The bindings load no library themselves: they find wgpu-native's
 * symbols among the libraries their class loader has loaded, so the app loads it first, from wherever it keeps it.
 */
abstract class GenerateWgpuBindings : DefaultTask() {
    @get:Input
    abstract val jextractHome: Property<String>

    @get:InputDirectory
    abstract val headers: DirectoryProperty

    @get:InputFile
    abstract val includes: RegularFileProperty

    @get:Input
    abstract val targetPackage: Property<String>

    /** The source root the package's folder is written under; the package's folder is replaced. */
    @get:Internal
    abstract val output: DirectoryProperty

    @TaskAction
    fun run() {
        val windows = System.getProperty("os.name").startsWith("Windows")
        val launcher = File(jextractHome.get(), if (windows) "bin/jextract.bat" else "bin/jextract")
        if (!launcher.exists()) throw GradleException("No jextract at $launcher; pass -Pjextract.home=<jextract folder>")
        val root = output.get().asFile
        root.resolve(targetPackage.get().replace('.', '/')).deleteRecursively()
        val header = headers.get().asFile.resolve("webgpu/wgpu.h")
        val command = listOf(launcher.path) + listOf(
            "--output", root.path,
            "-t", targetPackage.get(),
            "-I", headers.get().asFile.path,
            "@${includes.get().asFile.path}",
            header.path,
        )
        val process = ProcessBuilder(if (windows) listOf("cmd", "/c") + command else command)
            .redirectErrorStream(true)
            .start()
        val log = process.inputStream.bufferedReader().readText()
        if (process.waitFor() != 0) throw GradleException("jextract failed:\n$log")
    }
}
