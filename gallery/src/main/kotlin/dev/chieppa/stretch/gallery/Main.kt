package dev.chieppa.stretch.gallery

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.chieppa.stretch.devtools.DevTools
import dev.chieppa.stretch.document.Recovery
import dev.chieppa.stretch.frame.NativeSurfaces
import dev.chieppa.stretch.frame.StretchWindow
import dev.chieppa.stretch.frame.WindowTopBar

fun main() {
    // -Pgpu on the run task: show the WebGPU page, which needs popups in windows of their own.
    val gpu = System.getProperty("stretch.gpu").toBoolean()
    if (gpu) NativeSurfaces.enable()
    DevTools.startIfRequested()
    application {
        val state = rememberWindowState(width = 1200.dp, height = 820.dp)
        val host = remember { GalleryHost().apply { recovery = Recovery.forApp() } }
        StretchWindow(
            onCloseRequest = { host.closeGuard?.invoke(::exitApplication) ?: exitApplication() },
            title = "Stretch gallery",
            state = state,
        ) {
            host.window = window
            CompositionLocalProvider(LocalGalleryHost provides host) {
                Gallery(gpu = gpu) { document, edited, menus, modes, actions ->
                    WindowTopBar("Stretch", document, edited = edited, menus = menus, modes = modes, actions = actions)
                }
            }
        }
    }
}
