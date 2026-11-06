package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.onSizeChanged
import dev.chieppa.stretch.ui.onContextPress
import dev.chieppa.stretch.ui.opensContextMenu

/**
 * Gives [content] a context menu: a right-click, or a long press with a pen or finger, opens [menu] at the pointer,
 * and Shift F10 or the Menu key opens it under the area while the focus is inside. Put [MenuItem]s inside and call
 * `close` from each. Every item should also exist in the command palette, so the menu is a shortcut, never the only
 * way.
 */
@Composable
fun ContextMenuArea(
    menu: @Composable ColumnScope.(close: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var at by remember { mutableStateOf<Offset?>(null) }
    var height by remember { mutableIntStateOf(0) }
    Box(
        modifier
            .onSizeChanged { height = it.height }
            .onContextPress { at = it }
            .onKeyEvent { event -> event.opensContextMenu().also { if (it) at = Offset(0f, height.toFloat()) } },
    ) {
        content()
        ContextMenuPopup(at, { at = null }, menu)
    }
}
