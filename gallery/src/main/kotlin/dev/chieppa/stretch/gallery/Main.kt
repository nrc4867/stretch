package dev.chieppa.stretch.gallery

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.chieppa.stretch.devtools.DevTools
import dev.chieppa.stretch.document.Recovery
import dev.chieppa.stretch.frame.StretchWindow
import dev.chieppa.stretch.frame.WindowTopBar

fun main() {
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
                Gallery { document, edited, modes, actions ->
                    WindowTopBar("Stretch", document, edited = edited, modes = modes, actions = actions)
                }
            }
        }
    }
}
