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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
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
import dev.chieppa.stretch.ui.Tooltip
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
) = TopBarLayout(appName, document, modifier, modes = modes, actions = actions)

/**
 * The [TopBar] with room kept at its [start] and [end] for buttons the OS draws over it, and [barModifier] on the bar
 * itself (not the line under it).
 */
@Composable
internal fun TopBarLayout(
    appName: String,
    document: String,
    modifier: Modifier = Modifier,
    barModifier: Modifier = Modifier,
    start: Dp = 0.dp,
    end: Dp = 0.dp,
    modes: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val c = Stretch.colors
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(StretchSizes.header)
                .background(c.surfaceRail)
                .then(barModifier)
                .padding(start = start + 16.dp, end = end + 8.dp),
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
