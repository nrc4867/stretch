package dev.chieppa.stretch.gallery

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.chieppa.stretch.devtools.DevTools
import dev.chieppa.stretch.frame.StretchWindow
import dev.chieppa.stretch.frame.WindowTopBar

fun main() {
    DevTools.startIfRequested()
    application {
        val state = rememberWindowState(width = 1200.dp, height = 820.dp)
        StretchWindow(onCloseRequest = ::exitApplication, title = "Stretch gallery", state = state) {
            Gallery { document, modes, actions ->
                WindowTopBar("Stretch", document, modes = modes, actions = actions)
            }
        }
    }
}
