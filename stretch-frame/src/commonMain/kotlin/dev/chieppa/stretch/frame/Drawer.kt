package dev.chieppa.stretch.frame

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Icon
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchSizes
import dev.chieppa.stretch.ui.Chip

enum class DrawerStyle { Docked, Card, Floating }

/** A drawer's 44px header: icon, title, then small icon buttons for its actions, menu and close. */
@Composable
fun DrawerHeader(title: String, icon: ImageVector?, count: String? = null, actions: @Composable RowScope.() -> Unit = {}) {
    val c = Stretch.colors
    Column {
        Row(
            Modifier.fillMaxWidth().height(StretchSizes.header).padding(start = 14.dp, end = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) Icon(icon, c.inkControl)
            BasicText(
                title,
                Modifier.weight(1f),
                style = Stretch.type.drawerTitle.merge(TextStyle(color = c.inkStrong)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (count != null) Chip(count, mono = true)
            actions()
        }
        Box(Modifier.fillMaxWidth().height(Stretch.stroke).background(c.linePanel))
    }
}

/**
 * Every panel is a drawer: a header, then sections of settings rows or list rows. Contents are labels, values,
 * controls and icons; explanations go in tooltips.
 */
@Composable
fun Drawer(
    title: String,
    icon: ImageVector?,
    modifier: Modifier = Modifier,
    style: DrawerStyle = DrawerStyle.Card,
    count: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Stretch.colors
    val shape = if (style == DrawerStyle.Docked) RectangleShape else Stretch.shapes.cardShape
    val base = when (style) {
        DrawerStyle.Docked -> modifier.background(c.surfacePanel)
        DrawerStyle.Card -> modifier.clip(shape).background(c.surfacePanel).border(Stretch.stroke, c.linePanel, shape)
        DrawerStyle.Floating -> modifier.floating(shape, Stretch.shadows.float).background(c.surfacePanel)
    }
    Column(base) {
        DrawerHeader(title, icon, count, actions)
        Column(Modifier.verticalScroll(rememberScrollState()), content = content)
    }
}

/** A group of rows in a drawer, with an optional eyebrow label and a divider under it. */
@Composable
fun DrawerSection(label: String? = null, divider: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    val c = Stretch.colors
    Column {
        Column(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (label != null) BasicText(label.uppercase(), style = Stretch.type.eyebrow.merge(TextStyle(color = c.inkMuted)))
            content()
        }
        if (divider) Box(Modifier.fillMaxWidth().height(Stretch.stroke).background(c.linePanel))
    }
}

/** A settings row: a fixed-width label, then the control and its readout. */
@Composable
fun SettingRow(label: String, content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = StretchSizes.ctl),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(label, Modifier.width(76.dp), style = Stretch.type.body.merge(TextStyle(color = Stretch.colors.inkControl)), maxLines = 1)
        content()
    }
}

/** A 36px list row. [selected] uses the on colors; [indent] nests children 12 to 18px in. */
@Composable
fun ListRow(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    indent: Dp = 0.dp,
    content: @Composable RowScope.() -> Unit,
) {
    val c = Stretch.colors
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val shape = Stretch.shapes.rowShape
    Row(
        modifier
            .padding(start = indent)
            .fillMaxWidth()
            .height(StretchSizes.row)
            .clip(shape)
            .background(
                when {
                    selected -> c.onFill
                    hovered -> c.surfaceControl
                    else -> Color.Transparent
                },
            )
            .border(Stretch.stroke, if (selected) c.onEdge else Color.Transparent, shape)
            .hoverable(source)
            .clickable(source, indication = null, role = Role.Button, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalRowInk provides if (selected) c.onInk else c.ink) { content() }
    }
}

/** The ink color for text and icons inside the current [ListRow]. */
val LocalRowInk = compositionLocalOf { Color.Unspecified }

/** Text inside a [ListRow], in the row's ink. */
@Composable
fun RowText(text: String, modifier: Modifier = Modifier, subtle: Boolean = false) {
    val ink = LocalRowInk.current.takeIf { it != Color.Unspecified } ?: Stretch.colors.ink
    val color = if (subtle && ink == Stretch.colors.ink) Stretch.colors.inkSubtle else ink
    BasicText(text, modifier, style = Stretch.type.body.merge(TextStyle(color = color)), maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/** An icon inside a [ListRow], in the row's ink. */
@Composable
fun RowIcon(icon: ImageVector) {
    val ink = LocalRowInk.current.takeIf { it != Color.Unspecified } ?: Stretch.colors.inkControl
    Icon(icon, ink)
}

/** The handle between two stacked drawers. */
@Composable
fun DrawerSplit() {
    val c = Stretch.colors
    Column {
        Box(Modifier.fillMaxWidth().height(Stretch.stroke).background(c.linePanel))
        Box(Modifier.fillMaxWidth().height(8.dp).background(c.surfaceRail), contentAlignment = Alignment.Center) {
            Box(Modifier.size(width = 28.dp, height = 2.dp).clip(Stretch.shapes.pillShape).background(c.inkSubtle))
        }
        Box(Modifier.fillMaxWidth().height(Stretch.stroke).background(c.linePanel))
    }
}
