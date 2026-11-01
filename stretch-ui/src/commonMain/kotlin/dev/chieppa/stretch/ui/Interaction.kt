package dev.chieppa.stretch.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch

/** Hover and focus for one control. */
internal class ControlState(val source: MutableInteractionSource, val hovered: Boolean, val focused: Boolean)

@Composable
internal fun rememberControlState(): ControlState {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val focused by source.collectIsFocusedAsState()
    return ControlState(source, hovered, focused)
}

/**
 * The 2px focus ring, drawn inset: inside the control's border with a small gap, so it never grows the control or
 * touches a neighbor. Controls under 24dp have no room for the gap and get the ring just inside their border.
 * It shows whenever the control has focus; a click on empty space moves focus away (see [focusHome]). On a [fill]
 * the focus color would vanish against (an amber "on" control in a theme whose focus is amber) it uses `ink-strong`.
 */
@Composable
internal fun Modifier.focusRing(state: ControlState, shape: Shape, fill: Color? = null): Modifier {
    if (!state.focused) return this
    val focus = Stretch.colors.focus
    val color = if (fill != null && contrast(focus, fill) < 3f) Stretch.colors.inkStrong else focus
    val width = Stretch.theme.focusStroke
    val border = Stretch.stroke
    return drawWithContent {
        drawContent()
        val stroke = width.toPx()
        val gap = if (size.minDimension < 24.dp.toPx()) 0f else 1.5.dp.toPx()
        val inset = border.toPx() + gap + stroke / 2
        val outline = shape.createOutline(Size(size.width - 2 * inset, size.height - 2 * inset), layoutDirection, this)
        translate(inset, inset) { drawOutline(outline, color, style = Stroke(stroke)) }
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

/**
 * Makes this element the focus home: it takes focus when it appears and whenever a click lands on empty space inside
 * it, meaning nothing under the pointer used the click. Clicking away from a text field or a button then takes focus
 * off it, as people expect, while keys keep reaching shortcut handlers on this element. Clicks on controls, inside a
 * text field included, are left alone. Put it on the window or app root.
 */
fun Modifier.focusHome(): Modifier = composed {
    val home = remember { FocusRequester() }
    LaunchedEffect(Unit) { home.requestFocus() }
    focusRequester(home)
        .focusable()
        .pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
                if (down.isConsumed) return@awaitEachGesture
                val up = waitForUpOrCancellation(PointerEventPass.Final) ?: return@awaitEachGesture
                if (!up.isConsumed) home.requestFocus()
            }
        }
}

internal fun contrast(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    return (maxOf(la, lb) + 0.05f) / (minOf(la, lb) + 0.05f)
}
