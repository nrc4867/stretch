package dev.chieppa.stretch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Icon
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.theme.StretchSizes

enum class ChipKind { Default, On, Warn }

/** Whether a [Chip] draws a dashed edge, a value not yet confirmed. Tests read it. */
val ChipDashed = SemanticsPropertyKey<Boolean>("ChipDashed")

private val DASH = 3.dp
private val DASH_GAP = 2.dp

/**
 * A small label for a count, kind or state. A warning chip always shows the warning icon. With [onClick] it presses, as
 * a chip that opens a list of choices does, and ends in a chevron. [dashed] draws the edge dashed for a tentative value,
 * such as a guess. [description] names the chip for assistive tools when its text alone does not.
 */
@Composable
fun Chip(
    text: String,
    modifier: Modifier = Modifier,
    kind: ChipKind = ChipKind.Default,
    mono: Boolean = false,
    icon: ImageVector? = null,
    dashed: Boolean = false,
    description: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val c = Stretch.colors
    val state = rememberControlState()
    val shape = Stretch.shapes.pillShape
    val (rest, edge, ink) = when (kind) {
        ChipKind.Default -> Triple(Color.Transparent, c.chipEdge, c.inkMuted)
        ChipKind.On -> Triple(c.onFill, c.chipOnEdge, c.onInk)
        ChipKind.Warn -> Triple(c.warnWash, c.warnEdge, c.warnInk)
    }
    val pressable = onClick != null
    val fill = if (pressable && kind == ChipKind.Default) Fills.borderless(state) else rest
    val shown = icon ?: if (kind == ChipKind.Warn) StretchIcons.Warn else null
    Row(
        modifier = modifier
            .let { if (pressable) it.focusRing(state, shape, fill) else it }
            .height(StretchSizes.chip)
            .clip(shape)
            .background(fill)
            .let { if (dashed) it.dashedBorder(edge, Stretch.stroke, StretchSizes.chip / 2, dashed = true, dash = DASH, gap = DASH_GAP) else it.border(Stretch.stroke, edge, shape) }
            .let {
                if (onClick == null) it
                else it.hoverable(state.source).controlPointer().clickable(state.source, indication = null, role = Role.DropdownList, onClick = onClick)
            }
            .semantics {
                if (description != null) contentDescription = description
                set(ChipDashed, dashed)
            }
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (shown != null) Icon(shown, ink, size = 12.dp)
        ControlLabel(text, if (mono) Stretch.type.valueSm else Stretch.type.chip, ink)
        if (pressable) Icon(StretchIcons.ChevronDown, ink, size = 12.dp)
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
            .controlPointer().toggleable(selected, state.source, indication = null, role = Role.Checkbox, onValueChange = onSelectedChange)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ControlLabel(text, Stretch.type.bodySm.copy(fontWeight = Stretch.type.control.fontWeight), if (selected) c.onInk else c.inkControl)
    }
}
