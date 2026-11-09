package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
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
 * way. An area inside another opens one menu: its own items, then a divider, then the outer area's, so a row's menu
 * keeps the menu of the card it sits in and the outer menu never opens on top of it.
 */
@Composable
fun ContextMenuArea(
    menu: @Composable ColumnScope.(close: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var at by remember { mutableStateOf<Offset?>(null) }
    var height by remember { mutableIntStateOf(0) }
    val outer = LocalContextMenu.current
    val merged: @Composable ColumnScope.(close: () -> Unit) -> Unit = if (outer == null) {
        menu
    } else {
        { close ->
            menu(close)
            MenuDivider()
            outer.menu(this, close)
        }
    }
    fun open(point: Offset) {
        at = point
        outer?.dismiss?.invoke()
    }
    Box(
        modifier
            .onSizeChanged { height = it.height }
            .onContextPress(::open)
            .onKeyEvent { event -> event.opensContextMenu().also { if (it) open(Offset(0f, height.toFloat())) } },
    ) {
        CompositionLocalProvider(LocalContextMenu provides ContextMenuParent(merged) { at = null }) { content() }
        ContextMenuPopup(at, { at = null }, merged)
    }
}

/** The context menu of the area a [ContextMenuArea] sits in: its items, and closing it before it shows. */
private class ContextMenuParent(val menu: @Composable ColumnScope.(close: () -> Unit) -> Unit, val dismiss: () -> Unit)

private val LocalContextMenu = compositionLocalOf<ContextMenuParent?> { null }
