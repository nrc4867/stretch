package dev.chieppa.stretch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch

/**
 * A color swatch. [name] is the color's name for the tooltip and screen readers; show it near the row as well, so
 * color is never the only cue. The selected swatch gets a ring outside a panel-colored gap.
 */
@Composable
fun Swatch(color: Color, name: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Stretch.colors
    val state = rememberControlState()
    val seg = Stretch.shapes.seg
    val outer = RoundedCornerShape(seg + 4.dp)
    Box(
        modifier = modifier
            .focusRing(state, outer)
            .size(30.dp)
            .border(2.dp, if (selected) c.ringSelected else Color.Transparent, outer)
            .padding(4.dp)
            .hoverable(state.source)
            .controlPointer().selectable(selected, state.source, indication = null, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = name },
    ) {
        val shape = RoundedCornerShape(seg)
        Box(Modifier.size(22.dp).clip(shape).background(color).border(Stretch.stroke, c.lineStrong, shape))
    }
}
