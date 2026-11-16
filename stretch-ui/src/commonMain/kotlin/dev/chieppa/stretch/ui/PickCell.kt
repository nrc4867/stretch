package dev.chieppa.stretch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import dev.chieppa.stretch.theme.Stretch

/**
 * One choice in a picker's grid or list. It takes no focus itself: the picker holds focus and moves a keyboard cursor
 * over its cells, so Tab passes over the whole picker in one stop. [cursor] draws that cursor; [marked] edges a cell
 * worth finding, like today.
 */
@Composable
internal fun PickCell(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    muted: Boolean = false,
    cursor: Boolean = false,
    marked: Boolean = false,
    description: String? = null,
) {
    val c = Stretch.colors
    val state = rememberControlState()
    val shape = Stretch.shapes.segShape
    val fill = if (selected) Fills.on(state) else if (enabled) Fills.borderless(state) else Color.Transparent
    val ink = when {
        selected -> c.onInk
        !enabled -> c.inkSubtle.copy(alpha = 0.5f)
        muted -> c.inkSubtle
        else -> c.ink
    }
    val (edgeWidth, edge) = pickEdge(fill, cursor, selected, marked)
    Box(
        modifier
            .clip(shape)
            .background(fill)
            .border(edgeWidth, edge, shape)
            .focusProperties { canFocus = false }
            .hoverable(state.source, enabled)
            .controlPointer().clickable(state.source, null, enabled, role = Role.Button, onClick = onClick)
            .semantics {
                this.selected = selected
                if (description != null) contentDescription = description
            },
        contentAlignment = Alignment.Center,
    ) {
        Label(text, Stretch.type.value, ink)
    }
}

/**
 * The edge of a picker's cell on [fill]: the keyboard [cursor] in the focus color, a stroke thicker, or ink-strong
 * where the focus color would vanish against [fill]; then [selected] and [marked]; otherwise none.
 */
@Composable
internal fun pickEdge(fill: Color, cursor: Boolean, selected: Boolean = false, marked: Boolean = false): Pair<Dp, Color> {
    val c = Stretch.colors
    return when {
        cursor -> (Stretch.stroke + focusGrowth) to (if (contrast(c.focus, fill) < 3f) c.inkStrong else c.focus)
        selected -> Stretch.stroke to c.onEdge
        marked -> Stretch.stroke to c.chipEdge
        else -> Stretch.stroke to Color.Transparent
    }
}
