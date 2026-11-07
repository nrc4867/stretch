package dev.chieppa.stretch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Icon
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchSizes

enum class ButtonKind { Default, Primary, Quiet }

/** Fill, edge and ink for a button of [kind] in [state]. */
@Composable
internal fun buttonLook(kind: ButtonKind, state: ControlState): Triple<Color, Color, Color> {
    val c = Stretch.colors
    return when (kind) {
        ButtonKind.Default -> Triple(Fills.filled(state), c.lineStrong, c.inkStrong)
        ButtonKind.Primary -> Triple(Fills.on(state), c.onEdge, c.onInk)
        ButtonKind.Quiet -> Triple(Fills.borderless(state), Color.Transparent, c.inkControl)
    }
}

/**
 * A text button: 32px, sentence-case label of one to three words. At most one [ButtonKind.Primary] per panel. Made
 * wider than its label, as across a panel, it keeps the label in its middle.
 */
@Composable
fun Button(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Default,
    icon: ImageVector? = null,
) {
    val state = rememberControlState()
    val shape = Stretch.shapes.ctlShape
    val (fill, edge, ink) = buttonLook(kind, state)
    Row(
        modifier = modifier
            .focusRing(state, shape, fill)
            .height(StretchSizes.ctl)
            .clip(shape)
            .background(fill)
            .border(Stretch.stroke, edge, shape)
            .hoverable(state.source)
            .clickable(state.source, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(iconGap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, ink)
        val style = Stretch.type.control
        Label(text, if (kind == ButtonKind.Primary) style.copy(fontWeight = FontWeight.SemiBold) else style, ink)
    }
}

/**
 * A square icon-only button. [contentDescription] names the action; it is required because there is no label.
 * [on] marks a toggled-on state with the on colors. A button not [enabled] shows faint and takes no clicks.
 */
@Composable
fun IconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = StretchSizes.icon,
    quiet: Boolean = true,
    on: Boolean = false,
    enabled: Boolean = true,
) {
    val c = Stretch.colors
    val state = rememberControlState()
    val shape = Stretch.shapes.ctlShape
    val fill = when {
        on -> Fills.on(state)
        quiet -> Fills.borderless(state)
        else -> Fills.filled(state)
    }
    val edge = when {
        on -> c.onEdge
        quiet -> Color.Transparent
        else -> c.lineStrong
    }
    Box(
        modifier = modifier
            .focusRing(state, shape, fill)
            .size(size)
            .clip(shape)
            .background(fill)
            .border(Stretch.stroke, edge, shape)
            .hoverable(state.source)
            .clickable(state.source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, (if (on) c.onInk else c.inkControl).let { if (enabled) it else it.copy(alpha = disabledAlpha) })
    }
}

/**
 * A tool button for rails and toolbars: 40px, or 34px inside a toolbar. [selected] marks the armed tool. A button not
 * [enabled] shows faint and takes no clicks.
 */
@Composable
fun ToolButton(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = StretchSizes.tool,
    label: String? = null,
    enabled: Boolean = true,
) {
    val c = Stretch.colors
    val state = rememberControlState()
    val shape = Stretch.shapes.rowShape
    val fill = if (selected) Fills.on(state) else Fills.borderless(state)
    val ink = (if (selected) c.onInk else c.inkControl).let { if (enabled) it else it.copy(alpha = disabledAlpha) }
    val base = modifier
        .focusRing(state, shape, fill)
        .height(size)
        .clip(shape)
        .background(fill)
        .border(Stretch.stroke, if (selected) c.onEdge else Color.Transparent, shape)
        .hoverable(state.source)
        .clickable(state.source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
        .semantics {
            this.contentDescription = contentDescription
            this.selected = selected
        }
    if (label == null) {
        Box(base.size(size), contentAlignment = Alignment.Center) { Icon(icon, ink, size = 18.dp) }
    } else {
        Row(
            base.padding(start = 10.dp, end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(iconGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, ink, size = 18.dp)
            Label(label, Stretch.type.control, ink)
        }
    }
}
