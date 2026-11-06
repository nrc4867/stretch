package dev.chieppa.stretch.web

/** A JetBrains Runtime with JCEF to fetch: its [name], where from, the SHA-512 it must have, and its size. */
internal data class RuntimeDownload(val name: String, val url: String, val sha512: String, val bytes: Long)

private const val VERSION = "25.0.4.1"
private const val BUILD = "b329.136"
private const val SOURCE = "https://cache-redirector.jetbrains.com/intellij-jbr/"

/** Each platform's archive: its SHA-512, from JetBrains' checksum file beside it, and its size in bytes. */
private val archives = mapOf(
    "windows-x64" to ("a3f1292947174fb09203cf7f2e172d2b0212efddc5493854b46e4663ba0a1168c930b94069c2e5cc92130d5108a23ff09bbf4798b29c666ff27d7b46a13c0ade" to 266_045_608L),
    "windows-aarch64" to ("efd9086a26e51ebc0198a5c47b1aec0730676e8d5a727ed218e211a02bc2d05be4ede25961b9698c0f583bcd4f93a15b93643c31336c430aeae0a6bf152726fc" to 235_571_619L),
    "osx-x64" to ("fedd176da911f00602f365f423d6297f9691c1aaf0897340455504cb2a8f84d297500539cb63bea9010da06237c09ae6bd4ec8e6b3e91ba406732c64cc0f133d" to 242_355_271L),
    "osx-aarch64" to ("905ebd31c95ac3f5ce086e8e7b91a439bec375f5eda0aeed442d5a09e56b9cd4754ee9622fb1a58efbed6cd85c28d1f82d5d4307f439db1c7243b2b1eab0a67d" to 232_423_705L),
    "linux-x64" to ("ae7c5cb296e9d6209baf04c6dc5ba476cebb45cfb95fe8126db2f396e8aca547cfaba8c2a006c53d00642527817698b32be316302cbab75aef990d2100b5e599" to 246_307_185L),
    "linux-aarch64" to ("f35f07267da89c3aaaacde950c3833a9a2bad87a8de1afb08dc98840f6b9334801d6346447e4e3d3211aca4f418631988c4a64a6c2719a469f8829dd88ecd993" to 245_789_978L),
)

/** JetBrains' name for the platform of [os] and [arch], as the JVM reports them, like `windows-x64`; null for others. */
internal fun platformName(os: String, arch: String): String? {
    val system = when {
        os.startsWith("Windows") -> "windows"
        os.startsWith("Mac") -> "osx"
        os.startsWith("Linux") -> "linux"
        else -> return null
    }
    val cpu = when (arch.lowercase()) {
        "amd64", "x86_64" -> "x64"
        "aarch64", "arm64" -> "aarch64"
        else -> return null
    }
    return "$system-$cpu"
}

/** The runtime to fetch for [platform], or null where JetBrains publishes none. */
internal fun runtimeFor(platform: String?): RuntimeDownload? {
    val (sha512, bytes) = archives[platform] ?: return null
    val name = "jbr_jcef-$VERSION-$platform-$BUILD"
    return RuntimeDownload(name, "$SOURCE$name.tar.gz", sha512, bytes)
}

/** The runtime to fetch for this computer. */
internal fun runtimeForThisComputer(): RuntimeDownload? =
    runtimeFor(platformName(System.getProperty("os.name").orEmpty(), System.getProperty("os.arch").orEmpty()))
