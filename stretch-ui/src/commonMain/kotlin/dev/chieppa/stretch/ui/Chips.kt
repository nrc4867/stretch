package dev.chieppa.stretch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Icon
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.theme.StretchSizes

enum class ChipKind { Default, On, Warn }

/** A small label for a count, kind or state. A warning chip always shows the warning icon. */
@Composable
fun Chip(
    text: String,
    modifier: Modifier = Modifier,
    kind: ChipKind = ChipKind.Default,
    mono: Boolean = false,
    icon: ImageVector? = null,
) {
    val c = Stretch.colors
    val shape = Stretch.shapes.pillShape
    val (fill, edge, ink) = when (kind) {
        ChipKind.Default -> Triple(Color.Transparent, c.chipEdge, c.inkMuted)
        ChipKind.On -> Triple(c.onFill, c.chipOnEdge, c.onInk)
        ChipKind.Warn -> Triple(c.warnWash, c.warnEdge, c.warnInk)
    }
    val shown = icon ?: if (kind == ChipKind.Warn) StretchIcons.Warn else null
    Row(
        modifier = modifier
            .height(StretchSizes.chip)
            .clip(shape)
            .background(fill)
            .border(Stretch.stroke, edge, shape)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (shown != null) Icon(shown, ink, size = 12.dp)
        Label(text, if (mono) Stretch.type.valueSm else Stretch.type.chip, ink)
    }
}

/** An option chip people pick from; several may be on at once. */
@Composable
fun OptionChip(
    text: String,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Stretch.colors
    val state = rememberControlState()
    val shape = Stretch.shapes.pillShape
    val fill = if (selected) Fills.on(state) else Fills.filled(state)
    Row(
        modifier = modifier
            .focusRing(state, shape, fill)
            .height(StretchSizes.seg)
            .clip(shape)
            .background(fill)
            .border(Stretch.stroke, if (selected) c.chipOnEdge else c.chipEdge, shape)
            .hoverable(state.source)
            .toggleable(selected, state.source, indication = null, role = Role.Checkbox, onValueChange = onSelectedChange)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Label(text, Stretch.type.bodySm.copy(fontWeight = Stretch.type.control.fontWeight), if (selected) c.onInk else c.inkControl)
    }
}
