package dev.chieppa.stretch.gallery

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Stretch gallery",
        state = rememberWindowState(width = 1200.dp, height = 820.dp),
    ) {
        Gallery()
    }
}
