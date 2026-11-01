package dev.chieppa.stretch.document

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.FrameWindowScope
import java.io.File

/**
 * Tells the OS about the window's document: on macOS the close button shows the unsaved dot and the title bar the
 * file's icon. Windows and Linux show this in the top bar instead (see [dev.chieppa.stretch.frame.TopBar]'s edited).
 */
@Composable
fun FrameWindowScope.DocumentWindowState(path: String?, edited: Boolean) {
    LaunchedEffect(window, path, edited) {
        window.rootPane.putClientProperty("Window.documentModified", edited)
        window.rootPane.putClientProperty("Window.documentFile", path?.let(::File))
    }
}
