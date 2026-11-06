package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.chieppa.stretch.frame.generated.Res
import dev.chieppa.stretch.frame.generated.dock_left
import dev.chieppa.stretch.frame.generated.dock_right
import dev.chieppa.stretch.frame.generated.drawer_menu
import dev.chieppa.stretch.frame.generated.pop_out
import dev.chieppa.stretch.frame.generated.rail_bottom
import dev.chieppa.stretch.frame.generated.rail_top
import dev.chieppa.stretch.frame.generated.reset_layout
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.theme.StretchSizes
import dev.chieppa.stretch.ui.IconButton
import dev.chieppa.stretch.ui.opensOnPress
import org.jetbrains.compose.resources.stringResource

/** A drawer's menu: pop out, dock left or right, move to the other group of its rail, and reset the layout. */
@Composable
internal fun DrawerMenuButton(placement: DrawerPlacement, canPopOut: Boolean, rtl: Boolean, change: ((FrameLayout) -> FrameLayout) -> Unit, onReset: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(StretchIcons.More, stringResource(Res.string.drawer_menu), { open = true }, Modifier.opensOnPress(open) { open = true }, size = StretchSizes.seg)
        Menu(open, { open = false }) {
            if (canPopOut && !placement.detached) {
                MenuItem(stringResource(Res.string.pop_out), { change { it.popOut(placement.id) }; open = false })
                MenuDivider()
            }
            // Side.Left is the start side, on the right in a right-to-left layout; the menu names where it shows.
            val leftSide = if (rtl) Side.Right else Side.Left
            MenuItem(stringResource(Res.string.dock_left), { change { it.dock(placement.id, leftSide) }; open = false }, icon = StretchIcons.DockLeft)
            MenuItem(stringResource(Res.string.dock_right), { change { it.dock(placement.id, if (rtl) Side.Left else Side.Right) }; open = false }, icon = StretchIcons.DockRight)
            val toBottom = !placement.bottom
            MenuItem(
                stringResource(if (toBottom) Res.string.rail_bottom else Res.string.rail_top),
                { change { it.move(placement.id, placement.side, toBottom, Int.MAX_VALUE) }; open = false },
                icon = if (toBottom) StretchIcons.DockBottom else if ((placement.side == Side.Left) != rtl) StretchIcons.DockLeft else StretchIcons.DockRight,
            )
            MenuDivider()
            MenuItem(stringResource(Res.string.reset_layout), { onReset(); open = false })
        }
    }
}
