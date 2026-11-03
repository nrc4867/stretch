package dev.chieppa.stretch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchSizes

private const val disabledAlpha = 0.4f

/**
 * Picks exactly one of two to five options. A neutral pick uses the neutral color; [mode] is for top-level modes and
 * the armed view, which use the on colors. [fill] stretches the buttons across the row. An option [enabled] says no to
 * is dimmed and cannot be picked.
 */
@Composable
fun <T> Segmented(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    mode: Boolean = false,
    fill: Boolean = false,
    enabled: (T) -> Boolean = { true },
) {
    val c = Stretch.colors
    val track = Stretch.shapes.rowShape
    Row(
        modifier = modifier
            .clip(track)
            .background(c.surfaceControl)
            .border(Stretch.stroke, c.lineControl, track)
            .padding(2.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (option in options) {
            val isOn = option == selected
            val canPick = enabled(option)
            val state = rememberControlState()
            val shape = Stretch.shapes.segShape
            val (bg, edge, ink) = when {
                isOn && mode -> Triple(Fills.on(state), c.onEdge, c.onInk)
                isOn -> Triple(c.neutralOn, Color.Transparent, c.inkStrong)
                canPick -> Triple(Fills.menu(state), Color.Transparent, c.inkControl)
                else -> Triple(Color.Transparent, Color.Transparent, c.inkControl.copy(alpha = disabledAlpha))
            }
            Box(
                modifier = (if (fill) Modifier.weight(1f) else Modifier)
                    .focusRing(state, shape, bg)
                    .height(StretchSizes.seg)
                    .clip(shape)
                    .background(bg)
                    .border(Stretch.stroke, edge, shape)
                    .hoverable(state.source, canPick)
                    .selectable(isOn, state.source, indication = null, canPick, Role.Tab) { onSelect(option) }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                val style = Stretch.type.bodySm.copy(fontWeight = if (isOn && mode) FontWeight.SemiBold else FontWeight.Medium)
                Label(label(option), style, ink)
            }
        }
    }
}
