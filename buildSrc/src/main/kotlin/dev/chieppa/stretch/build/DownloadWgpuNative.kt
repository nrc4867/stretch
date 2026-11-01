package dev.chieppa.stretch.build

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.net.URI
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/**
 * Downloads one wgpu-native release archive, fails unless its SHA-256 is [sha256], and unpacks its `lib` and
 * `include` folders into [destination]. Up to date while the URL, checksum and unpacked files are unchanged.
 */
abstract class DownloadWgpuNative : DefaultTask() {
    @get:Input
    abstract val url: Property<String>

    @get:Input
    abstract val sha256: Property<String>

    @get:OutputDirectory
    abstract val destination: DirectoryProperty

    @TaskAction
    fun run() {
        val bytes = URI(url.get()).toURL().openStream().use { it.readBytes() }
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        if (digest != sha256.get().lowercase()) {
            throw GradleException("${url.get()} has SHA-256 $digest, expected ${sha256.get()}")
        }
        val root = destination.get().asFile
        root.deleteRecursively()
        ZipInputStream(bytes.inputStream()).use { zip ->
            generateSequence { zip.nextEntry }.filter { !it.isDirectory }.forEach { entry ->
                val name = entry.name.substringAfter("wgpu-native-meta/", entry.name)
                if (name.startsWith("lib/") || name.startsWith("include/")) {
                    val file = root.resolve(name)
                    file.parentFile.mkdirs()
                    file.outputStream().use { zip.copyTo(it) }
                }
            }
        }
    }
}
