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

/** The chosen files' paths, through [SystemFileDialogs]. */
actual suspend fun pickFiles(title: String, extensions: Set<String>, multiple: Boolean, kindName: String): List<String> {
    val kinds = if (extensions.isEmpty()) emptyList() else listOf(FileKind(kindName.ifBlank { extensionsName(extensions) }, extensions))
    return SystemFileDialogs.open(title, kinds, multiple)
}
