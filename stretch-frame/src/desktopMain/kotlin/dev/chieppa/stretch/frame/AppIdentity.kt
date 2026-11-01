package dev.chieppa.stretch.frame

import com.sun.jna.WString
import com.sun.jna.platform.win32.Shell32
import java.awt.Image
import java.awt.Taskbar
import java.awt.Toolkit
import java.util.Properties
import javax.imageio.ImageIO

private val os = System.getProperty("os.name").orEmpty()

/**
 * The app's name, id and icons, written by the `dev.chieppa.stretch.app` Gradle plugin from `stretchApp { }`. Null
 * when the app does not use the plugin; windows then keep whatever icon they are given.
 */
class AppIdentity(val name: String, val id: String, val icons: List<Image>) {
    companion object {
        private const val DIR = "stretch-app"

        /** The identity on the classpath, read once. */
        val current: AppIdentity? by lazy { load() }

        private fun load(): AppIdentity? {
            val props = javaClass.getResourceAsStream("/$DIR/app.properties")?.use { Properties().apply { load(it) } } ?: return null
            val icons = props.getProperty("icons").orEmpty().split(',').mapNotNull { size ->
                javaClass.getResourceAsStream("/$DIR/icon-${size.trim()}.png")?.use { ImageIO.read(it) }
            }
            return AppIdentity(props.getProperty("name"), props.getProperty("id"), icons)
        }

        private var applied = false

        /**
         * Tells the OS who this process is, before the first window opens: on Windows, run as plain `java`, a taskbar
         * id of its own so the app groups and pins apart from other Java apps (a packaged launcher already has one);
         * the dock icon, app name and screen menu bar on macOS; the window class on Linux, so the desktop matches the
         * app's icon and name.
         */
        fun applyToProcess() {
            if (applied) return
            applied = true
            if (os.startsWith("Mac")) System.setProperty("apple.laf.useScreenMenuBar", "true")
            val identity = current ?: return
            if (os.startsWith("Mac")) System.setProperty("apple.awt.application.name", identity.name)
            runCatching {
                when {
                    os.startsWith("Windows") -> if (System.getProperty("jpackage.app-path") == null) {
                        Shell32.INSTANCE.SetCurrentProcessExplicitAppUserModelID(WString(identity.id))
                    }
                    os.startsWith("Mac") -> if (Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE)) {
                        identity.icons.lastOrNull()?.let { Taskbar.getTaskbar().iconImage = it }
                    }
                    else -> Toolkit.getDefaultToolkit().javaClass.getDeclaredField("awtAppClassName").apply { isAccessible = true }
                        .set(null, identity.name)
                }
            }
        }
    }
}
