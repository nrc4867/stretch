package dev.chieppa.stretch.frame

import dev.chieppa.stretch.ui.controlPointer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import dev.chieppa.stretch.theme.Icon
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.theme.StretchSizes
import dev.chieppa.stretch.ui.StretchPopup

/**
 * A menu item that holds more items: a chevron at its end, and its [items] in a panel beside it while it is open.
 * Hovering or clicking it, or Right while it has the focus, opens it; hovering another item of the same menu or a click
 * outside closes it. The panel leaves the keyboard with the menu, so Esc closes the whole menu. Items in the panel call
 * the menu's own `close`, so choosing one closes the whole menu.
 */
@Composable
fun SubMenuItem(
    text: String,
    icon: ImageVector? = null,
    items: @Composable ColumnScope.() -> Unit,
) {
    val c = Stretch.colors
    val key = remember { Any() }
    val own = remember { mutableStateOf<Any?>(null) }
    val openState: MutableState<Any?> = LocalOpenSubMenu.current ?: own
    val open = openState.value === key
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    LaunchedEffect(hovered) { if (hovered) openState.value = key }
    fun dismiss() {
        if (openState.value === key) openState.value = null
    }
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .height(StretchSizes.icon)
                .clip(Stretch.shapes.segShape)
                .background(if (hovered || open) c.surfaceControlHi else Color.Transparent)
                .hoverable(source)
                .controlPointer().clickable(source, indication = null, role = Role.Button) { openState.value = key }
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown || event.key != Key.DirectionRight) return@onKeyEvent false
                    openState.value = key
                    true
                }
                .padding(horizontal = ITEM_PADDING),
            horizontalArrangement = Arrangement.spacedBy(ITEM_PADDING),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) Icon(icon, c.inkControl)
            BasicText(text, Modifier.weight(1f), style = Stretch.type.body.merge(TextStyle(color = if (hovered || open) c.inkStrong else c.ink)), maxLines = 1)
            Icon(StretchIcons.ChevronRight, c.inkMuted)
        }
        if (open) {
            StretchPopup(
                popupPositionProvider = BesideItem,
                onDismissRequest = ::dismiss,
                properties = PopupProperties(focusable = false),
                shadow = Stretch.shadows.pop,
            ) { MenuPanel(content = items) }
        }
    }
}

private val ITEM_PADDING = 8.dp

/** Places a submenu's panel beside its item, on the left when the right side has no room, kept inside the window. */
private object BesideItem : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val right = anchorBounds.right
        val x = if (right + popupContentSize.width <= windowSize.width) right else anchorBounds.left - popupContentSize.width
        val y = anchorBounds.top.coerceAtMost(windowSize.height - popupContentSize.height)
        return IntOffset(x.coerceAtLeast(0), y.coerceAtLeast(0))
    }
}
