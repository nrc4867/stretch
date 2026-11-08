package dev.chieppa.stretch.frame

import dev.chieppa.stretch.ui.controlPointer
import dev.chieppa.stretch.ui.opensOnPress
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchSizes

/** One menu of a [BarMenus]: its title and its items. [content] gets a call that closes the menu. */
class BarMenu(val title: String, val content: @Composable ColumnScope.(close: () -> Unit) -> Unit)

/**
 * The menus inside the top bar on Windows and Linux: a title per menu that opens its items under it. Goes in the
 * [TopBar]'s menus slot. While one is open, pointing at another title opens that one instead. A click outside or Esc
 * closes the open menu.
 */
@Composable
fun BarMenus(menus: List<BarMenu>, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf<String?>(null) }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        for (menu in menus) {
            val close = { open = null }
            Box {
                BarMenuTitle(menu.title, open == menu.title, open != null) { open = menu.title }
                Menu(open == menu.title, close) { menu.content(this, close) }
            }
        }
    }
}

@Composable
private fun BarMenuTitle(title: String, open: Boolean, anyOpen: Boolean, onClick: () -> Unit) {
    val c = Stretch.colors
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val pressed by source.collectIsPressedAsState()
    if (hovered && anyOpen && !open) SideEffect(onClick)
    Box(
        Modifier
            .opensOnPress(open, onClick)
            .height(StretchSizes.icon)
            .clip(Stretch.shapes.segShape)
            .background(
                when {
                    pressed -> c.surfaceControlPress
                    open -> c.surfaceControlHi
                    hovered -> c.surfaceControl
                    else -> Color.Transparent
                },
            )
            .hoverable(source)
            .controlPointer().clickable(source, indication = null, role = Role.DropdownList, onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(title, style = Stretch.type.body.merge(TextStyle(color = c.inkControl)), maxLines = 1)
    }
}
