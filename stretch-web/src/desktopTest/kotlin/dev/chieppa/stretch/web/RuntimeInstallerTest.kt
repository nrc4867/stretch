package dev.chieppa.stretch.web

import com.sun.net.httpserver.HttpServer
import dev.chieppa.stretch.core.Requirements
import dev.chieppa.stretch.core.RequirementState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import java.net.InetSocketAddress
import java.security.MessageDigest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Installs a stand-in runtime from a server on this machine, so it needs no network. */
class RuntimeInstallerTest {
    private val work = createTempDirectory("runtime-installer").toFile()
    private val folder = File(work, "runtimes")
    private val archive = fakeRuntimeArchive()
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    private val release = CountDownLatch(1)

    init {
        server.createContext("/runtime.tar.gz") { exchange ->
            val bytes = archive.readBytes()
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        // Sends the first half, then holds the rest until the test lets go: a download in progress.
        server.createContext("/slow.tar.gz") { exchange ->
            val bytes = archive.readBytes()
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            runCatching {
                exchange.responseBody.use { out ->
                    out.write(bytes, 0, bytes.size / 2)
                    out.flush()
                    release.await(30, TimeUnit.SECONDS)
                    out.write(bytes, bytes.size / 2, bytes.size - bytes.size / 2)
                }
            }
        }
        server.start()
    }

    @AfterTest
    fun stop() {
        release.countDown()
        server.stop(0)
        work.deleteRecursively()
    }

    private fun download(path: String = "runtime.tar.gz", sha512: String = sha512(archive)) =
        RuntimeDownload("test-runtime", "http://127.0.0.1:${server.address.port}/$path", sha512, archive.length())

    @Test
    fun installsACheckedRuntimeAndReportsProgress() = runBlocking {
        val installer = RuntimeInstaller(download(), folder)
        assertNull(installer.installedHome())
        var last = 0L
        installer.install { last = it }
        assertEquals(archive.length(), last)
        val home = assertNotNull(installer.installedHome())
        assertTrue(File(home, "bin").isDirectory)
        assertEquals(listOf("test-runtime"), folder.list()!!.toList(), "nothing else stays behind")
    }

    @Test
    fun aRuntimeThatDoesNotMatchItsChecksumIsNotInstalled() = runBlocking {
        val requirement = EngineRequirement({ false }, RuntimeInstaller(download(sha512 = "00"), folder))
        assertEquals(RequirementState.Missing, requirement.state.value)
        requirement.install()
        val ended = withTimeout(TIMEOUT_MILLIS) { requirement.state.first { it is RequirementState.Failed } }
        assertIs<RequirementState.Failed>(ended)
        assertEquals(emptyList(), folder.list()!!.toList())
    }

    @Test
    fun anInstallRunsInTheBackgroundUntilItNeedsARestart() = runBlocking {
        val requirement = EngineRequirement({ false }, RuntimeInstaller(download(), folder))
        requirement.install()
        withTimeout(TIMEOUT_MILLIS) { requirement.state.first { it == RequirementState.NeedsRestart } }
        assertNotNull(requirement.installedHome())
        val again = EngineRequirement({ false }, RuntimeInstaller(download(), folder))
        assertEquals(RequirementState.NeedsRestart, again.state.value, "a later start finds it installed")
    }

    @Test
    fun cancellingStopsADownloadAndClearsIt() = runBlocking {
        val requirement = EngineRequirement({ false }, RuntimeInstaller(download("slow.tar.gz"), folder))
        requirement.install()
        withTimeout(TIMEOUT_MILLIS) { requirement.state.first { it is RequirementState.Installing && it.fetchedBytes > 0 } }
        requirement.cancel()
        withTimeout(TIMEOUT_MILLIS) { requirement.state.first { it == RequirementState.Missing } }
        val deadline = System.currentTimeMillis() + TIMEOUT_MILLIS
        while (folder.list()!!.isNotEmpty() && System.currentTimeMillis() < deadline) Thread.sleep(50)
        assertEquals(emptyList(), folder.list()!!.toList())
    }

    @Test
    fun anEngineHereIsReady() {
        assertEquals(RequirementState.Ready, EngineRequirement({ true }, null).state.value)
        assertIs<RequirementState.Failed>(EngineRequirement({ false }, null).state.value, "no runtime is published for this computer")
    }

    @Test
    fun theEngineIsListedAmongTheAppsRequirements() {
        assertNotNull(Requirements.find(ENGINE_REQUIREMENT))
    }

    @Test
    fun eachPlatformHasItsRuntime() {
        assertEquals("windows-x64", platformName("Windows 11", "amd64"))
        assertEquals("osx-aarch64", platformName("Mac OS X", "aarch64"))
        assertEquals("linux-x64", platformName("Linux", "x86_64"))
        assertNull(platformName("SunOS", "sparc"))
        val windows = assertNotNull(runtimeFor("windows-x64"))
        assertTrue(windows.url.endsWith("jbr_jcef-25.0.4.1-windows-x64-b329.136.tar.gz"))
        assertEquals(128, windows.sha512.length)
    }

    /** A gzipped tar of `runtime/bin/java` and `java.exe`, made with the OS's own tar. */
    private fun fakeRuntimeArchive(): File {
        val source = File(work, "source/runtime/bin").apply { mkdirs() }
        File(source, "java").writeText("stand-in")
        File(source, "java.exe").writeText("stand-in")
        val archive = File(work, "runtime.tar.gz")
        val tar = if (File.separatorChar == '\\') File(System.getenv("SystemRoot"), "System32\\tar.exe").path else "tar"
        val process = ProcessBuilder(tar, "-czf", archive.absolutePath, "-C", File(work, "source").absolutePath, "runtime").inheritIO().start()
        check(process.waitFor() == 0)
        return archive
    }

    private fun sha512(file: File) = MessageDigest.getInstance("SHA-512").digest(file.readBytes()).joinToString("") { "%02x".format(it) }

    private companion object {
        const val TIMEOUT_MILLIS = 20_000L
    }
}
