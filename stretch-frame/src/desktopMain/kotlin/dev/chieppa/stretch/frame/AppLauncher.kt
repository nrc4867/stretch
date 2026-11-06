package dev.chieppa.stretch.frame

import dev.chieppa.stretch.core.AppLaunch
import dev.chieppa.stretch.core.stretchUserFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val APPS_FOLDER = "apps"
private const val RESOURCE_DIR = "/stretch-app/"

/**
 * A launcher an app run from jars makes for itself, with its own name and icon from `stretchApp { }`: a Start menu
 * shortcut on Windows, an app in `~/Applications` on macOS, a desktop entry on Linux. People then start it like any
 * other app. The app makes these files itself, so no OS treats them as downloaded and nothing needs signing.
 * A packaged app has a launcher already, so there it is not [available].
 */
object AppLauncher {
    private val os = System.getProperty("os.name").orEmpty()
    private val home = File(System.getProperty("user.home"))

    /** Whether this app can make a launcher: it runs from jars, not a package, with the plugin's identity. */
    val available: Boolean get() = !AppLaunch.packaged && AppIdentity.current != null

    /** Whether this app's launcher is in place. */
    fun installed(): Boolean = launcherFile()?.exists() == true

    /**
     * Makes the launcher, or brings it up to date: it starts the app as this process was started, on the Java runtime
     * running now. Call it again after a move to another runtime.
     */
    suspend fun install() = withContext(Dispatchers.IO) {
        val identity = checkNotNull(AppIdentity.current) { "The app has no identity from the stretchApp plugin" }
        check(!AppLaunch.packaged) { "A packaged app has its own launcher" }
        val command = AppLaunch.command(markRestarted = false)
        val files = File(stretchUserFolder(), "$APPS_FOLDER/${identity.id}").apply { mkdirs() }
        when {
            os.startsWith("Windows") -> windowsShortcut(identity, command, files)
            os.startsWith("Mac") -> macBundle(identity, command)
            else -> linuxEntry(identity, command, files)
        }
    }

    /** Takes the launcher away. */
    suspend fun remove() = withContext(Dispatchers.IO) {
        launcherFile()?.let { if (it.isDirectory) it.deleteRecursively() else it.delete() }
    }

    private fun launcherFile(): File? {
        val identity = AppIdentity.current ?: return null
        return when {
            os.startsWith("Windows") -> File(startMenu(), "${fileName(identity.name)}.lnk")
            os.startsWith("Mac") -> File(home, "Applications/${fileName(identity.name)}.app")
            else -> File(applicationsFolder(), "${identity.id}.desktop")
        }
    }

    private fun startMenu() = File(System.getenv("APPDATA") ?: "$home\\AppData\\Roaming", "Microsoft\\Windows\\Start Menu\\Programs")

    private fun applicationsFolder() = File(System.getenv("XDG_DATA_HOME") ?: "$home/.local/share", "applications")

    /** [name] without the characters file systems refuse. */
    private fun fileName(name: String) = name.replace(Regex("""[\\/:*?"<>|]"""), "").trim()

    private fun copyResource(name: String, to: File): File {
        val stream = checkNotNull(javaClass.getResourceAsStream(RESOURCE_DIR + name)) { "The app's $name is missing; rebuild with the stretchApp plugin" }
        stream.use { input -> to.outputStream().use { input.copyTo(it) } }
        return to
    }

    private fun windowsShortcut(identity: AppIdentity, command: List<String>, files: File) {
        val icon = copyResource("icon.ico", File(files, "icon.ico"))
        // A shortcut's arguments are short, so they go through a Java argument file.
        val arguments = File(files, "launch.args").apply { writeText(LauncherFiles.argumentFile(command.drop(1))) }
        writeWindowsShortcut(checkNotNull(launcherFile()), command.first(), "@" + arguments.path, icon, identity.name)
    }

    private fun macBundle(identity: AppIdentity, command: List<String>) {
        val bundle = checkNotNull(launcherFile())
        bundle.deleteRecursively()
        val contents = File(bundle, "Contents")
        File(contents, "MacOS").mkdirs()
        File(contents, "Resources").mkdirs()
        File(contents, "Info.plist").writeText(LauncherFiles.infoPlist(identity.name, identity.id))
        copyResource("icon.icns", File(contents, "Resources/${LauncherFiles.MAC_ICON}"))
        File(contents, "MacOS/${LauncherFiles.MAC_PROGRAM}").apply {
            writeText(LauncherFiles.shellScript(command))
            setExecutable(true)
        }
    }

    private fun linuxEntry(identity: AppIdentity, command: List<String>, files: File) {
        val icon = copyResource("icon.png", File(files, "icon.png"))
        val entry = checkNotNull(launcherFile())
        entry.parentFile.mkdirs()
        entry.writeText(LauncherFiles.desktopEntry(identity.name, command, icon.path))
    }
}

/**
 * Writes a Windows shortcut to [target] with [argument], through Windows' own shell. The values go in the
 * environment, so nothing needs quoting for PowerShell.
 */
internal fun writeWindowsShortcut(shortcut: File, target: String, argument: String, icon: File, description: String) {
    shortcut.parentFile.mkdirs()
    val script = "\$s = (New-Object -ComObject WScript.Shell).CreateShortcut(\$env:STRETCH_LINK); " +
        "\$s.TargetPath = \$env:STRETCH_TARGET; \$s.Arguments = \$env:STRETCH_ARGUMENTS; \$s.IconLocation = \$env:STRETCH_ICON; " +
        "\$s.WorkingDirectory = \$env:STRETCH_FOLDER; \$s.Description = \$env:STRETCH_NAME; \$s.Save()"
    val process = ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-Command", script).redirectErrorStream(true).apply {
        environment() += mapOf(
            "STRETCH_LINK" to shortcut.path,
            "STRETCH_TARGET" to target,
            "STRETCH_ARGUMENTS" to LauncherFiles.windowsArguments(listOf(argument)),
            "STRETCH_ICON" to icon.path,
            "STRETCH_FOLDER" to System.getProperty("user.dir"),
            "STRETCH_NAME" to description,
        )
    }.start()
    val output = process.inputStream.bufferedReader().readText()
    check(process.waitFor() == 0 && shortcut.isFile) { "Could not make the shortcut: $output" }
}
