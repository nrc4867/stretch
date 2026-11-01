package dev.chieppa.stretch.gpu.webgpu

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

/**
 * Loads wgpu-native from the jar. The library is copied once into the user's cache, in a folder named after its
 * content hash, so apps on different versions never overwrite each other's copy and nothing is written next to the
 * Java runtime, which an installed app cannot write to.
 */
internal object WgpuLibrary {
    private const val NAME = "wgpu_native"
    private const val HASH_CHARS = 16

    @Volatile private var loaded = false

    fun load() {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            val file = System.mapLibraryName(NAME)
            val resource = "/dev/chieppa/stretch/gpu/native/${platform()}/$file"
            val bytes = WgpuLibrary::class.java.getResourceAsStream(resource)?.use { it.readBytes() }
                ?: error("This build of Stretch has no wgpu-native for ${platform()}")
            val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }.take(HASH_CHARS)
            val target = cacheRoot().resolve("wgpu-native/$hash/$file")
            if (!target.isFile || target.length() != bytes.size.toLong()) {
                target.parentFile.mkdirs()
                val partial = File.createTempFile(NAME, ".part", target.parentFile)
                partial.writeBytes(bytes)
                Files.move(partial.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            }
            System.load(target.absolutePath)
            loaded = true
        }
    }

    /** The folder name the build packs this platform's library under (see stretch-gpu-native's build). */
    private fun platform(): String {
        val os = System.getProperty("os.name").lowercase()
        val arm = System.getProperty("os.arch").lowercase().let { it == "aarch64" || it == "arm64" }
        return when {
            os.startsWith("windows") -> "windows-x64"
            os.startsWith("mac") -> if (arm) "macos-arm64" else "macos-x64"
            else -> if (arm) "linux-arm64" else "linux-x64"
        }
    }

    private fun cacheRoot(): File {
        val os = System.getProperty("os.name").lowercase()
        val home = System.getProperty("user.home")
        return when {
            os.startsWith("windows") -> File(System.getenv("LOCALAPPDATA") ?: "$home/AppData/Local", "Stretch/Cache")
            os.startsWith("mac") -> File(home, "Library/Caches/Stretch")
            else -> File(System.getenv("XDG_CACHE_HOME") ?: "$home/.cache", "stretch")
        }
    }
}
