package dev.chieppa.stretch.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch

/** Hover and focus for one control. [pressed] says it was clicked or tapped since it took focus. */
internal class ControlState(val source: MutableInteractionSource, val hovered: Boolean, val focused: Boolean, val pressed: Boolean)

@Composable
internal fun rememberControlState(): ControlState {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val focused by source.collectIsFocusedAsState()
    var pressed by remember { mutableStateOf(false) }
    LaunchedEffect(source) {
        source.interactions.collect {
            when (it) {
                is PressInteraction.Press -> pressed = true
                is FocusInteraction.Unfocus -> pressed = false
            }
        }
    }
    return ControlState(source, hovered, focused, pressed)
}

/**
 * Whether the last input was a key that moves or acts on focus (Tab, arrows, Enter, Space, Esc) rather than a pointer
 * press. Shortcuts and modifier keys alone do not count, so a screenshot shortcut never lights up a ring.
 */
internal expect val keyboardInput: State<Boolean>

/**
 * The 2px focus ring, drawn on the control's own edge so it never grows the control or overlaps a neighbor. Only
 * keyboard focus shows it; focus that came from a click or a tap does not.
 */
@Composable
internal fun Modifier.focusRing(state: ControlState, shape: Shape): Modifier {
    if (!state.focused || state.pressed || !keyboardInput.value) return this
    val color = Stretch.colors.focus
    val width = Stretch.theme.focusStroke
    return drawWithContent {
        drawContent()
        val stroke = width.toPx()
        val outline = shape.createOutline(Size(size.width - stroke, size.height - stroke), layoutDirection, this)
        translate(stroke / 2, stroke / 2) { drawOutline(outline, color, style = Stroke(stroke)) }
    }
}

@Composable
internal fun Label(text: String, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.merge(TextStyle(color = color)),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

internal val iconGap = 6.dp
