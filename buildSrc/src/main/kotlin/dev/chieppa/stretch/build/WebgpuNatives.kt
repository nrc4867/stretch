package dev.chieppa.stretch.build

import org.gradle.api.provider.ProviderFactory

/**
 * Which webgpu-native-ffm natives jar a build takes: this machine's, or the platform named by `-Pwebgpu.natives`
 * (`windows-x64`, `linux-x64`, `linux-arm64`, `macos-x64` or `macos-arm64`) when packaging for another.
 */
object WebgpuNatives {
    const val PROPERTY = "webgpu.natives"

    /** The classifier of the natives jar to take, such as `natives-windows-x64`. */
    fun classifier(providers: ProviderFactory): String =
        "natives-${providers.gradleProperty(PROPERTY).orNull ?: host()}"

    /** The platform of the machine running the build. */
    fun host(): String {
        val os = System.getProperty("os.name").lowercase()
        val arm = System.getProperty("os.arch").lowercase().let { it == "aarch64" || it == "arm64" }
        return when {
            os.startsWith("windows") -> "windows-x64"
            os.startsWith("mac") -> if (arm) "macos-arm64" else "macos-x64"
            else -> if (arm) "linux-arm64" else "linux-x64"
        }
    }
}
