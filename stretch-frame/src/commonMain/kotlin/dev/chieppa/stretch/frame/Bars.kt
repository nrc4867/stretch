package dev.chieppa.stretch.frame

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch

/**
 * The 44px bar across the top of the window: app name, the [menus] of a [BarMenus], then [leading] (a control that
 * belongs beside them, such as a picker for what the window shows), document name and state, the mode switch, then
 * window-level actions. [center] is centred on the whole bar, such as a search field. Nothing else goes here. [edited]
 * puts a dot after the document name while it has unsaved changes. When the window narrows, the document name
 * shortens first; then the centre item moves aside to stay clear of what is on its left and right, narrows down to
 * 160dp (or its own width, if less) and then hides; then actions from [TopBarActions] fold into a More menu. The app
 * name, the menus, [leading], the mode switch and that menu always stay.
 */
@Composable
fun TopBar(
    appName: String,
    document: String,
    modifier: Modifier = Modifier,
    edited: Boolean = false,
    menus: @Composable () -> Unit = {},
    modes: @Composable () -> Unit = {},
    leading: @Composable () -> Unit = {},
    center: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) = TopBarLayout(appName, document, modifier, edited = edited, menus = menus, modes = modes, leading = leading, center = center, actions = actions)

/** The floating bar at the top-left of the viewport: tools, then the armed tool's options. */
@Composable
fun Toolbar(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val shape = Stretch.shapes.cardShape
    Row(
        modifier
            .floating(shape, Stretch.shadows.pop)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** A divider between groups in a [Toolbar]. */
@Composable
fun ToolbarSeparator() {
    Box(Modifier.padding(horizontal = 4.dp).width(Stretch.stroke).height(22.dp).background(Stretch.colors.linePanel))
}

/** A small readout over the viewport: angle, size, zoom. [leading] goes before the text, such as a progress bar. */
@Composable
fun Hud(text: String, modifier: Modifier = Modifier, leading: (@Composable () -> Unit)? = null) {
    val c = Stretch.colors
    Row(
        modifier.floating(Stretch.shapes.pillShape, shadow = null).padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        BasicText(text, style = Stretch.type.value.merge(TextStyle(color = c.inkMuted)))
    }
}
