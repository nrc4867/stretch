package dev.chieppa.stretch.ui

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.domDataTransferOrNull
import org.w3c.files.get

@OptIn(ExperimentalComposeUiApi::class)
private fun DragAndDropEvent.droppedPaths(): List<String> {
    val files = transferData?.domDataTransferOrNull?.files ?: return emptyList()
    return (0 until files.length).mapNotNull { files[it]?.let(BrowserFiles::add) }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun Modifier.fileDropTarget(onHover: (HoveredFiles?) -> Unit, onDrop: (List<String>) -> Boolean): Modifier {
    val hover = rememberUpdatedState(onHover)
    val drop = rememberUpdatedState(onDrop)
    val target = remember {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) = hover.value(HoveredFiles(null))
            override fun onExited(event: DragAndDropEvent) = hover.value(null)
            override fun onEnded(event: DragAndDropEvent) = hover.value(null)
            override fun onDrop(event: DragAndDropEvent): Boolean = drop.value(event.droppedPaths())
        }
    }
    return dragAndDropTarget(shouldStartDragAndDrop = { true }, target = target)
}
