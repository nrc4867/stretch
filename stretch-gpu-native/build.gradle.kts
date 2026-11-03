import dev.chieppa.stretch.build.DownloadWgpuNative
import dev.chieppa.stretch.build.GenerateWgpuBindings

plugins {
    `java-library`
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(libs.versions.jvmToolchain.get().toInt()))
}

/** One wgpu-native release archive for a platform, the library file in it, and where it goes in the jar. */
data class WgpuArchive(val asset: String, val sha256: String, val library: String, val platform: String)

val wgpuVersion = libs.versions.wgpu.native.get()
val archives = listOf(
    WgpuArchive("wgpu-windows-x86_64-msvc-release.zip", "7e67d7445c42aeb85e30f88930fd8d7d83ee769e3390aeb1ada75ebf3cf78132", "wgpu_native.dll", "windows-x64"),
    WgpuArchive("wgpu-linux-x86_64-release.zip", "95a4d90c071005a98d03eab348beaa6b07e16eb00d1dcdb9f8348f75eb97ec5a", "libwgpu_native.so", "linux-x64"),
    WgpuArchive("wgpu-linux-aarch64-release.zip", "015fcdf1dbae82e614a783cc38017e5399ae0927a889fe9b69c9b664bc61b47a", "libwgpu_native.so", "linux-arm64"),
    WgpuArchive("wgpu-macos-aarch64-release.zip", "a5797a37b1adf720bcd5dcffb291edbbd5b7b14be0a3874c28e6393a655a7a3e", "libwgpu_native.dylib", "macos-arm64"),
    WgpuArchive("wgpu-macos-x86_64-release.zip", "8e2f7378548ddd0e2cf21e7d864dda46e953f0af724855a33778b85ead206d41", "libwgpu_native.dylib", "macos-x64"),
)

/** The archive for the machine building, whose library goes in the jar. Published builds run on Windows; see publish.yml. */
val hostArchive: WgpuArchive = run {
    val os = System.getProperty("os.name").lowercase()
    val arm = System.getProperty("os.arch").lowercase().let { it == "aarch64" || it == "arm64" }
    val platform = when {
        os.startsWith("windows") -> "windows-x64"
        os.startsWith("mac") -> if (arm) "macos-arm64" else "macos-x64"
        else -> if (arm) "linux-arm64" else "linux-x64"
    }
    archives.first { it.platform == platform }
}

val downloadWgpuNative by tasks.registering(DownloadWgpuNative::class) {
    url = "https://github.com/gfx-rs/wgpu-native/releases/download/$wgpuVersion/${hostArchive.asset}"
    sha256 = hostArchive.sha256
    destination = layout.buildDirectory.dir("wgpu-native/${hostArchive.platform}")
}

tasks.processResources {
    from(downloadWgpuNative.map { it.destination.dir("lib") }) {
        include(hostArchive.library)
        into("dev/chieppa/stretch/gpu/native/${hostArchive.platform}")
    }
}

// Regenerates src/main/java from the downloaded headers: ./gradlew :stretch-gpu-native:generateBindings -Pjextract.home=<jextract>
tasks.register<GenerateWgpuBindings>("generateBindings") {
    jextractHome = providers.gradleProperty("jextract.home")
    headers = downloadWgpuNative.flatMap { it.destination.dir("include") }
    includes = layout.projectDirectory.file("bindings.includes")
    targetPackage = "dev.chieppa.stretch.gpu.ffi"
    output = layout.projectDirectory.dir("src/main/java")
}
