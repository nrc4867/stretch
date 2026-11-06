package dev.chieppa.stretch.frame

import dev.chieppa.stretch.core.javaIn
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppLauncherTest {
    private val work = createTempDirectory("app-launcher").toFile()

    @AfterTest
    fun clean() {
        work.deleteRecursively()
    }

    @Test
    fun anAppFromJarsCanMakeALauncher() {
        assertTrue(AppLauncher.available, "the test app has an identity and no package")
    }

    @Test
    fun javaReadsBackAnArgumentFileExactly() {
        val tricky = "C:\\My Apps\\a \"quoted\" b"
        val file = File(work, "launch.args").apply { writeText(LauncherFiles.argumentFile(listOf("-Dstretch.tricky=$tricky", "-XshowSettings:properties", "-version"))) }
        val java = javaIn(File(System.getProperty("java.home")), windowed = false).path
        val process = ProcessBuilder(java, "@${file.path}").redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()
        assertContains(output, "stretch.tricky = $tricky")
    }

    @Test
    fun aMacBundleRunsTheCommandAsItsOwnProgram() {
        val script = LauncherFiles.shellScript(listOf("/jbr/bin/java", "-jar", "/Users/me/It's here.jar"))
        assertEquals("#!/bin/sh\nexec '/jbr/bin/java' '-jar' '/Users/me/It'\\''s here.jar'\n", script)
        val plist = LauncherFiles.infoPlist("Notes & Co", "dev.example.notes")
        assertContains(plist, "<string>Notes &amp; Co</string>")
        assertContains(plist, "<key>CFBundleExecutable</key><string>${LauncherFiles.MAC_PROGRAM}</string>")
    }

    @Test
    fun aLinuxEntryQuotesItsCommand() {
        val entry = LauncherFiles.desktopEntry("Notes", listOf("/jbr/bin/java", "-jar", "/home/me/my notes.jar", "100%"), "/icons/notes.png")
        assertContains(entry, "Exec=\"/jbr/bin/java\" \"-jar\" \"/home/me/my notes.jar\" \"100%%\"")
        assertContains(entry, "StartupWMClass=Notes")
    }

    @Test
    fun windowsArgumentsQuoteOnlyWhatNeedsIt() {
        assertEquals("plain C:\\dir\\ \"C:\\My dir\\\\\"", LauncherFiles.windowsArguments(listOf("plain", "C:\\dir\\", "C:\\My dir\\")))
        assertEquals("\"@C:\\My Apps\\launch.args\"", LauncherFiles.windowsArguments(listOf("@C:\\My Apps\\launch.args")))
    }

    @Test
    fun aWindowsShortcutPointsWhereItWasTold() {
        if (!System.getProperty("os.name").startsWith("Windows")) return
        val shortcut = File(work, "Test App.lnk")
        val icon = File(work, "icon.ico").apply { writeBytes(ByteArray(0)) }
        writeWindowsShortcut(shortcut, "C:\\Java\\bin\\javaw.exe", "@C:\\My Apps\\launch.args", icon, "Test App")
        val read = ProcessBuilder(
            "powershell.exe", "-NoProfile", "-Command",
            "\$s = (New-Object -ComObject WScript.Shell).CreateShortcut('${shortcut.path}'); \$s.TargetPath + '|' + \$s.Arguments",
        ).redirectErrorStream(true).start()
        val output = read.inputStream.bufferedReader().readText().trim()
        read.waitFor()
        assertEquals("C:\\Java\\bin\\javaw.exe|\"@C:\\My Apps\\launch.args\"", output)
    }
}
