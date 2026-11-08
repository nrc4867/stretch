package dev.chieppa.stretch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchSizes

/**
 * An on/off switch for a setting that applies the moment it flips. On and off differ in knob size as well as
 * color, so the state never depends on color alone.
 */
@Composable
fun Toggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val c = Stretch.colors
    val state = rememberControlState()
    val shape = Stretch.shapes.pillShape
    Box(
        modifier = modifier
            .focusRing(state, shape, if (checked) c.toggleOn else c.toggleOff)
            .size(width = 34.dp, height = StretchSizes.chip)
            .clip(shape)
            .background(if (checked) c.toggleOn else c.toggleOff)
            .border(Stretch.stroke, if (checked) c.toggleOnEdge else c.toggleOffEdge, shape)
            .hoverable(state.source)
            .controlPointer().toggleable(checked, state.source, indication = null, role = Role.Switch, onValueChange = onCheckedChange)
            .semantics { this.contentDescription = contentDescription }
            .padding(horizontal = 2.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .padding(horizontal = if (checked) 0.dp else 1.dp)
                .size(if (checked) 14.dp else 12.dp)
                .clip(shape)
                .background(if (checked) c.toggleOnKnob else c.toggleOffKnob),
        )
    }
}
