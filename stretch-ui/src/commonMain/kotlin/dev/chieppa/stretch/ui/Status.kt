package dev.chieppa.stretch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chieppa.stretch.theme.Icon
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons

/**
 * A badge on the top-right corner of [content]: a count ("3", "99+") or, with an empty [text], a dot. It marks
 * something new or waiting, never a state that needs a color to read. [description] names it for screen readers.
 */
@Composable
fun BadgedBox(text: String?, description: String, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(modifier) {
        content()
        if (text != null) {
            val dot = text.isEmpty()
            BadgeMark(text, description, Modifier.align(Alignment.TopEnd).offset(x = if (dot) 1.dp else 5.dp, y = if (dot) (-1).dp else (-5).dp))
        }
    }
}

/** The badge itself: a count or, with an empty [text], a dot [dot] across. */
@Composable
internal fun BadgeMark(text: String, description: String, modifier: Modifier = Modifier, dot: Dp = 9.dp) {
    val c = Stretch.colors
    val isDot = text.isEmpty()
    Box(
        modifier
            .then(if (isDot) Modifier.size(dot) else Modifier.heightIn(min = 16.dp).widthIn(min = 16.dp))
            .clip(Stretch.shapes.pillShape)
            .background(c.accent)
            .border(1.5.dp, c.surfaceRail, Stretch.shapes.pillShape)
            .semantics { contentDescription = description }
            .padding(horizontal = if (isDot) 0.dp else 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (!isDot) Label(text, Stretch.type.valueSm.copy(fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.SemiBold), c.onAccent)
    }
}

/** A count as a badge shows it: nothing for zero, the number up to 99, then "99+". */
fun badgeCount(count: Int): String? = when {
    count <= 0 -> null
    count > 99 -> "99+"
    else -> count.toString()
}

/**
 * What a drawer, list or viewport shows with nothing in it: an icon, a short label and at most one action that fixes
 * it ("No layers" · "Add layer"). No paragraphs: the label names the state, the action the way out.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    val c = Stretch.colors
    Column(
        modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(c.surfaceControl), contentAlignment = Alignment.Center) {
            Icon(icon, c.inkMuted, size = 20.dp)
        }
        Label(label, Stretch.type.body, c.inkMuted)
        if (actionLabel != null) Button(actionLabel, onAction, icon = StretchIcons.Plus)
    }
}

/**
 * Tabs for open documents or views inside one drawer. The current tab is `ink-strong` on the panel with an accent
 * bar under it; the rest are `ink-muted`. With [onClose] each tab has a close button and middle-click is left to
 * the app. Use [Segmented] to switch a setting, tabs to switch what you look at.
 */
@Composable
fun <T> Tabs(
    tabs: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    onClose: ((T) -> Unit)? = null,
    closeDescription: (T) -> String = { label(it) },
) {
    val c = Stretch.colors
    Column(modifier) {
        Row(Modifier.height(36.dp), verticalAlignment = Alignment.Bottom) {
            for (tab in tabs) {
                val state = rememberControlState()
                val current = tab == selected
                Column(
                    Modifier
                        .height(36.dp)
                        .width(IntrinsicSize.Max)
                        .focusRing(state, Stretch.shapes.segShape)
                        .background(if (current) c.surfacePanel else Fills.borderless(state))
                        .hoverable(state.source)
                        .clickable(state.source, null, role = Role.Tab) { onSelect(tab) },
                ) {
                    Row(
                        Modifier.weight(1f).padding(start = 12.dp, end = if (onClose != null) 4.dp else 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Label(label(tab), Stretch.type.control, if (current) c.inkStrong else c.inkMuted)
                        if (onClose != null) FieldIcon(StretchIcons.Close, contentDescription = closeDescription(tab), onClick = { onClose(tab) })
                    }
                    Box(Modifier.fillMaxWidth().height(2.dp).background(if (current) c.accent else Color.Transparent))
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(Stretch.stroke).background(c.linePanel))
    }
}
