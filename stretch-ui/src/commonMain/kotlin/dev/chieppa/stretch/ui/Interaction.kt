package dev.chieppa.stretch.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
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

/** Hover and keyboard focus for one control. */
internal class ControlState(val source: MutableInteractionSource, val hovered: Boolean, val focused: Boolean)

@Composable
internal fun rememberControlState(): ControlState {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val focused by source.collectIsFocusedAsState()
    return ControlState(source, hovered, focused)
}

/** The 2px focus ring, drawn 2px outside the control. */
@Composable
internal fun Modifier.focusRing(state: ControlState, shape: Shape): Modifier {
    if (!state.focused) return this
    val color = Stretch.colors.focus
    val width = Stretch.theme.focusStroke
    return drawWithContent {
        drawContent()
        val stroke = width.toPx()
        val inflate = 2.dp.toPx() + stroke / 2
        val outline = shape.createOutline(
            Size(size.width + 2 * inflate, size.height + 2 * inflate), layoutDirection, this,
        )
        translate(-inflate, -inflate) { drawOutline(outline, color, style = Stroke(stroke)) }
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
