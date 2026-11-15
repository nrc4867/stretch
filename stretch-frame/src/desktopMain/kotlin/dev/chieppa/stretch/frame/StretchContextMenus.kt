package dev.chieppa.stretch.frame

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ContextMenuRepresentation
import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Compose's own context menus, such as Copy on selected text and in text fields, drawn as Stretch's context menu:
 * compact, as wide as its items, and alike on Windows, macOS and Linux.
 */
object StretchContextMenuRepresentation : ContextMenuRepresentation {
    @Composable
    override fun Representation(state: ContextMenuState, items: () -> List<ContextMenuItem>) {
        val status = state.status as? ContextMenuState.Status.Open ?: return
        val close = { state.status = ContextMenuState.Status.Closed }
        ContextMenuPopup(status.rect.topLeft, close) {
            for (item in items()) {
                MenuItem(item.label, { close(); item.onClick() }, enabled = item.enabled)
            }
        }
    }
}

/** Gives every context menu Compose draws in [content] Stretch's look; [StretchWindow] puts it around its content. */
@Composable
fun StretchContextMenus(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalContextMenuRepresentation provides StretchContextMenuRepresentation, content = content)
}
