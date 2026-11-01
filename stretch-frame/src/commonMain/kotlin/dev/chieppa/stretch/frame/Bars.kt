package dev.chieppa.stretch.frame

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.frame.generated.Res
import dev.chieppa.stretch.frame.generated.commands
import dev.chieppa.stretch.frame.generated.redo
import dev.chieppa.stretch.frame.generated.settings
import dev.chieppa.stretch.frame.generated.undo
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.theme.StretchSizes
import dev.chieppa.stretch.ui.IconButton
import dev.chieppa.stretch.ui.ToolButton
import org.jetbrains.compose.resources.stringResource

/**
 * The 44px bar across the top of the window: app name, document name and state, the mode switch, then window-level
 * actions. Nothing else goes here.
 */
@Composable
fun TopBar(
    appName: String,
    document: String,
    modifier: Modifier = Modifier,
    modes: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val c = Stretch.colors
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().height(StretchSizes.header).background(c.surfaceRail).padding(start = 16.dp, end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(appName, style = Stretch.type.heading.merge(TextStyle(color = c.inkStrong)))
            BasicText(document, style = Stretch.type.body.merge(TextStyle(color = c.inkSubtle)), maxLines = 1)
            modes()
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) { actions() }
        }
        Spacer(Modifier.fillMaxWidth().height(Stretch.stroke).background(c.linePanel))
    }
}

/** The standard top bar actions: undo, redo, the command palette and settings. Pass null to leave one out. */
@Composable
fun RowScope.TopBarActions(
    onUndo: (() -> Unit)?,
    onRedo: (() -> Unit)?,
    onCommands: (() -> Unit)?,
    onSettings: (() -> Unit)? = null,
) {
    onUndo?.let { Tooltip(stringResource(Res.string.undo), listOf("Ctrl", "Z")) { IconButton(StretchIcons.Undo, stringResource(Res.string.undo), it) } }
    onRedo?.let { Tooltip(stringResource(Res.string.redo), listOf("Ctrl", "Shift", "Z")) { IconButton(StretchIcons.Redo, stringResource(Res.string.redo), it) } }
    onCommands?.let { Tooltip(stringResource(Res.string.commands), listOf("Ctrl", "K")) { IconButton(StretchIcons.Command, stringResource(Res.string.commands), it) } }
    onSettings?.let { Tooltip(stringResource(Res.string.settings)) { IconButton(StretchIcons.Settings, stringResource(Res.string.settings), it) } }
}

data class RailItem(val id: String, val icon: ImageVector, val label: String)

/** The 48px icon rail on one edge. Each button opens or closes one drawer; open drawers show as on. */
@Composable
fun Rail(
    side: Side,
    items: List<RailItem>,
    open: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Stretch.colors
    Row(modifier.fillMaxHeight()) {
        if (side == Side.Right) Spacer(Modifier.fillMaxHeight().width(Stretch.stroke).background(c.linePanel))
        Column(
            Modifier.fillMaxHeight().width(StretchSizes.rail).background(c.surfaceRail).padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            for (item in items) {
                Tooltip(item.label) { ToolButton(item.icon, item.label, item.id in open, { onToggle(item.id) }) }
            }
        }
        if (side == Side.Left) Spacer(Modifier.fillMaxHeight().width(Stretch.stroke).background(c.linePanel))
    }
}

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

/** A small readout over the viewport: angle, size, zoom. */
@Composable
fun Hud(text: String, modifier: Modifier = Modifier) {
    val c = Stretch.colors
    Box(
        modifier.floating(Stretch.shapes.pillShape, shadow = null).padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        BasicText(text, style = Stretch.type.value.merge(TextStyle(color = c.inkMuted)))
    }
}
