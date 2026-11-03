package dev.chieppa.stretch.frame

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Icon
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchSizes
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.frame.generated.Res
import dev.chieppa.stretch.frame.generated.collapsed
import dev.chieppa.stretch.frame.generated.expanded
import dev.chieppa.stretch.ui.Divider
import dev.chieppa.stretch.ui.Chip
import org.jetbrains.compose.resources.stringResource
import dev.chieppa.stretch.ui.ScrollColumn

enum class DrawerStyle { Docked, Card }

/**
 * A drawer's 44px header: icon, title, then small icon buttons for its actions, menu and close. At the top of a
 * popped-out drawer's window it is the title bar: its empty space drags the window and its buttons stay buttons.
 */
@Composable
fun DrawerHeader(title: String, icon: ImageVector?, count: String? = null, actions: @Composable RowScope.() -> Unit = {}) {
    val c = Stretch.colors
    val bar = LocalTitleBar.current
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .height(StretchSizes.header)
                .then(if (bar != null) Modifier.titleBarHitTest(bar.regions, bar.onHit) else Modifier)
                .padding(start = 14.dp + (bar?.start ?: 0.dp), end = 6.dp + (bar?.end ?: 0.dp)),
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
            if (bar != null) {
                Row(
                    Modifier.titleBarControl(bar.regions, "actions"),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) { actions() }
            } else {
                actions()
            }
        }
        Divider()
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
    }
    Column(base) {
        DrawerHeader(title, icon, count, actions)
        ScrollColumn(Modifier.weight(1f, fill = false), content = content)
    }
}

/**
 * A group of rows in a drawer, with an optional eyebrow label and a divider under it. With [collapsed] set the label
 * folds the section: a chevron leads it and clicking it reports [onCollapsedChange]. The drawer's app keeps which
 * sections are folded, like the rest of its layout.
 */
@Composable
fun DrawerSection(
    label: String? = null,
    divider: Boolean = true,
    collapsed: Boolean? = null,
    onCollapsedChange: (Boolean) -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Stretch.colors
    val folds = collapsed != null && label != null
    Column {
        Column(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = if (collapsed == true) 10.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (label != null) {
                if (folds) {
                    val source = remember { MutableInteractionSource() }
                    val hovered by source.collectIsHoveredAsState()
                    val state = stringResource(if (collapsed == true) Res.string.collapsed else Res.string.expanded)
                    Row(
                        Modifier
                            .hoverable(source)
                            .clickable(source, null, role = Role.Button) { onCollapsedChange(collapsed != true) }
                            .semantics { stateDescription = state },
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(if (collapsed == true) StretchIcons.ChevronRight else StretchIcons.ChevronDown, if (hovered) c.inkStrong else c.inkMuted, size = 12.dp)
                        BasicText(label.uppercase(), style = Stretch.type.eyebrow.merge(TextStyle(color = if (hovered) c.inkStrong else c.inkMuted)))
                    }
                } else {
                    BasicText(label.uppercase(), style = Stretch.type.eyebrow.merge(TextStyle(color = c.inkMuted)))
                }
            }
            if (collapsed != true) content()
        }
        if (divider) Divider()
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
    val pressed by source.collectIsPressedAsState()
    val shape = Stretch.shapes.rowShape
    Row(
        modifier
            .padding(start = indent)
            .fillMaxWidth()
            .height(StretchSizes.row)
            .clip(shape)
            .background(
                when {
                    selected && pressed -> c.onFillPress
                    selected -> c.onFill
                    pressed -> c.surfaceControlHi
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
        Divider()
        Box(Modifier.fillMaxWidth().height(8.dp).background(c.surfaceRail), contentAlignment = Alignment.Center) {
            Box(Modifier.size(width = 28.dp, height = 2.dp).clip(Stretch.shapes.pillShape).background(c.inkSubtle))
        }
        Divider()
    }
}
