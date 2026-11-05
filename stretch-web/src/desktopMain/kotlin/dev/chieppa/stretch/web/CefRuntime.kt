package dev.chieppa.stretch.web

import com.jetbrains.cef.JCefAppConfig
import org.cef.CefApp
import org.cef.CefSettings
import java.io.File
import javax.swing.SwingUtilities

/** The one Chromium in the process, started on first use. This class cannot load without JCEF, so check [WebEngine.available] first. */
internal object CefRuntime {
    private const val CACHE_FOLDER = "stretch-web-cache"

    /** Started Chromium. Must be called on the event thread, as JCEF wants on every OS. */
    val app: CefApp by lazy {
        check(SwingUtilities.isEventDispatchThread()) { "Chromium starts on the event thread" }
        val config = JCefAppConfig.getInstance()
        val settings = config.cefSettings.apply {
            windowless_rendering_enabled = true
            no_sandbox = true
            log_severity = CefSettings.LogSeverity.LOGSEVERITY_DISABLE
            // Pages keep nothing between runs: the cache is scratch space, one per process since Chromium locks it.
            cache_path = File(System.getProperty("java.io.tmpdir"), "$CACHE_FOLDER-${ProcessHandle.current().pid()}").path
        }
        val arguments = config.appArgs
        CefApp.startup(arguments)
        val bundle = config.nativeBundlePath?.let(::File)
        CefApp.getInstance(arguments, settings, bundle).also {
            Runtime.getRuntime().addShutdownHook(Thread { File(settings.cache_path).deleteRecursively() })
        }
    }
}
