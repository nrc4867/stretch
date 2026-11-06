package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.chieppa.stretch.frame.generated.Res
import dev.chieppa.stretch.frame.generated.resize_split
import org.jetbrains.compose.resources.stringResource

/** The split between two stacked drawers. Reports the layout at the start of the drag and the distance in pixels. */
@Composable
internal fun SplitHandle(layout: () -> FrameLayout, onResize: (start: FrameLayout, dy: Float) -> Unit) {
    val description = stringResource(Res.string.resize_split)
    val resize by rememberUpdatedState(onResize)
    var start by remember { mutableStateOf<FrameLayout?>(null) }
    Box(
        Modifier
            .fillMaxWidth()
            .pointerHoverIcon(resizeUpDown)
            .windowDrag(vertical = true, onStart = { start = layout() }) { dy -> start?.let { resize(it, dy) } }
            .semantics { contentDescription = description },
    ) { DrawerSplit() }
}
