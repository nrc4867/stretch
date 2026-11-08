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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Icon
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchSizes

/** Side padding of a [ToggleButton] at the full control height and at anything smaller. */
private val toggleButtonPadding = 14.dp
private val compactToggleButtonPadding = 10.dp

/**
 * A text button that stays on, for an action that marks something, like "Mark reviewed" or "Pin". Off it is a
 * [ButtonKind.Default] button, with a fill and border, so it always reads as a control; on it takes the on colors, like
 * an armed tool. [height] is the control height; pass `StretchSizes.seg` for a compact one in a header or toolbar.
 * Screen readers hear a checkbox with [text] as its name.
 */
@Composable
fun ToggleButton(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = StretchSizes.ctl,
    enabled: Boolean = true,
) {
    val c = Stretch.colors
    val state = rememberControlState()
    val shape = Stretch.shapes.ctlShape
    val fill = if (checked) Fills.on(state) else Fills.filled(state)
    val edge = if (checked) c.onEdge else c.lineStrong
    val ink = (if (checked) c.onInk else c.inkStrong).let { if (enabled) it else it.copy(alpha = disabledAlpha) }
    Row(
        modifier = modifier
            .focusRing(state, shape, fill)
            .height(height)
            .clip(shape)
            .background(fill)
            .border(Stretch.stroke, edge, shape)
            .hoverable(state.source, enabled)
            .controlPointer().toggleable(checked, state.source, indication = null, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange)
            .padding(horizontal = if (height < StretchSizes.ctl) compactToggleButtonPadding else toggleButtonPadding),
        horizontalArrangement = Arrangement.spacedBy(iconGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, ink)
        val style = Stretch.type.control
        Label(text, if (checked) style.copy(fontWeight = FontWeight.SemiBold) else style, ink)
    }
}
