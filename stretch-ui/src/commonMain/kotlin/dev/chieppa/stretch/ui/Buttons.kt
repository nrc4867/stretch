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

/** A text button: 32px, sentence-case label of one to three words. At most one [ButtonKind.Primary] per panel. */
@Composable
fun Button(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Default,
    icon: ImageVector? = null,
) {
    val c = Stretch.colors
    val state = rememberControlState()
    val shape = Stretch.shapes.ctlShape
    val (fill, edge, ink) = when (kind) {
        ButtonKind.Default -> Triple(if (state.hovered) c.surfaceControlHi else c.surfaceControl, c.lineStrong, c.inkStrong)
        ButtonKind.Primary -> Triple(c.onFill, c.onEdge, c.onInk)
        ButtonKind.Quiet -> Triple(if (state.hovered) c.surfaceControl else Color.Transparent, Color.Transparent, c.inkControl)
    }
    Row(
        modifier = modifier
            .focusRing(state, shape)
            .height(StretchSizes.ctl)
            .clip(shape)
            .background(fill)
            .border(Stretch.stroke, edge, shape)
            .hoverable(state.source)
            .clickable(state.source, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(iconGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, ink)
        val style = Stretch.type.control
        Label(text, if (kind == ButtonKind.Primary) style.copy(fontWeight = FontWeight.SemiBold) else style, ink)
    }
}

/**
 * A square icon-only button. [contentDescription] names the action; it is required because there is no label.
 * [on] marks a toggled-on state with the on colors.
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
) {
    val c = Stretch.colors
    val state = rememberControlState()
    val shape = Stretch.shapes.ctlShape
    val fill = when {
        on -> c.onFill
        quiet -> if (state.hovered) c.surfaceControl else Color.Transparent
        else -> if (state.hovered) c.surfaceControlHi else c.surfaceControl
    }
    val edge = when {
        on -> c.onEdge
        quiet -> Color.Transparent
        else -> c.lineStrong
    }
    Box(
        modifier = modifier
            .focusRing(state, shape)
            .size(size)
            .clip(shape)
            .background(fill)
            .border(Stretch.stroke, edge, shape)
            .hoverable(state.source)
            .clickable(state.source, indication = null, role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, if (on) c.onInk else c.inkControl)
    }
}

/** A tool button for rails and toolbars: 40px, or 34px inside a toolbar. [selected] marks the armed tool. */
@Composable
fun ToolButton(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = StretchSizes.tool,
    label: String? = null,
) {
    val c = Stretch.colors
    val state = rememberControlState()
    val shape = Stretch.shapes.rowShape
    val fill = when {
        selected -> c.onFill
        state.hovered -> c.surfaceControl
        else -> Color.Transparent
    }
    val ink = if (selected) c.onInk else c.inkControl
    val base = modifier
        .focusRing(state, shape)
        .height(size)
        .clip(shape)
        .background(fill)
        .border(Stretch.stroke, if (selected) c.onEdge else Color.Transparent, shape)
        .hoverable(state.source)
        .clickable(state.source, indication = null, role = Role.Button, onClick = onClick)
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
