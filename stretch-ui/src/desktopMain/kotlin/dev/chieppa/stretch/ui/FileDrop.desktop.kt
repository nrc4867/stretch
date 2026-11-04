package dev.chieppa.stretch.ui

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.net.URI

@OptIn(ExperimentalComposeUiApi::class)
private fun DragAndDropEvent.paths(): List<String>? = runCatching {
    (dragData() as? DragData.FilesList)?.readFiles()?.map { File(URI(it)).path }
}.getOrNull()

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun Modifier.fileDropTarget(onHover: (HoveredFiles?) -> Unit, onDrop: (List<String>) -> Boolean): Modifier {
    val hover = rememberUpdatedState(onHover)
    val drop = rememberUpdatedState(onDrop)
    val target = remember {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) = hover.value(HoveredFiles(event.paths().orEmpty()))
            override fun onExited(event: DragAndDropEvent) = hover.value(null)
            override fun onEnded(event: DragAndDropEvent) = hover.value(null)
            override fun onDrop(event: DragAndDropEvent): Boolean = drop.value(event.paths().orEmpty())
        }
    }
    return dragAndDropTarget(shouldStartDragAndDrop = { true }, target = target)
}

/** Opens the system file picker and returns the chosen paths, empty when cancelled. */
fun pickFiles(title: String, extensions: Set<String>, multiple: Boolean = true): List<String> {
    val dialog = FileDialog(null as Frame?, title, FileDialog.LOAD)
    dialog.isMultipleMode = multiple
    if (extensions.isNotEmpty()) dialog.setFilenameFilter { _, name -> fileExtension(name) in extensions }
    dialog.isVisible = true
    return dialog.files.map { it.path }
}
