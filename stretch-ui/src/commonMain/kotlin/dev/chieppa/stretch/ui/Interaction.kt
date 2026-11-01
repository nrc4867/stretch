package dev.chieppa.stretch.ui

import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
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

/** Hover, press and focus for one control. */
internal class ControlState(val source: MutableInteractionSource, val hovered: Boolean, val pressed: Boolean, val focused: Boolean) {
    /** [rest], [hover] while the pointer is over it, [press] while it is held down. */
    fun fill(rest: Color, hover: Color, press: Color) = when {
        pressed -> press
        hovered -> hover
        else -> rest
    }
}

@Composable
internal fun rememberControlState(): ControlState {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val pressed by source.collectIsPressedAsState()
    val focused by source.collectIsFocusedAsState()
    return ControlState(source, hovered, pressed, focused)
}

/** Fill ladders: rest, hover, press. Each step is visibly different, so a click always shows. */
internal object Fills {
    /** Buttons and chips with a resting fill. */
    @Composable fun filled(state: ControlState) = Stretch.colors.let { state.fill(it.surfaceControl, it.surfaceControlHi, it.surfaceControlPress) }

    /** Quiet and borderless buttons, list rows, tabs. */
    @Composable fun borderless(state: ControlState) = Stretch.colors.let { state.fill(Color.Transparent, it.surfaceControl, it.surfaceControlHi) }

    /** Menu items and segments: hover is already the hi step. */
    @Composable fun menu(state: ControlState) = Stretch.colors.let { state.fill(Color.Transparent, it.surfaceControlHi, it.surfaceControlPress) }

    /** Anything on, and primary buttons: no hover change, darker while pressed. */
    @Composable fun on(state: ControlState) = Stretch.colors.let { state.fill(it.onFill, it.onFill, it.onFillPress) }
}

/**
 * Whether focus shows on controls other than text fields, like CSS `:focus-visible`: on after keyboard use, off after
 * a pointer press, so a click never leaves a focus border behind while Tab still shows where you are.
 * [focusHome] keeps it current.
 */
internal object FocusVisibility {
    var keyboard by mutableStateOf(true)
}

private val modifierKeys = setOf(
    Key.ShiftLeft, Key.ShiftRight, Key.CtrlLeft, Key.CtrlRight, Key.AltLeft, Key.AltRight, Key.MetaLeft, Key.MetaRight,
)

/** How much thicker a control's border gets while it shows focus. */
internal val focusGrowth = 1.dp

/**
 * Focus shows as the control's border turning the focus color and [focusGrowth] thicker. The thicker border is drawn
 * over the control from its outer edge inward, so focus never changes the control's size or spacing. It shows only
 * after keyboard use (see [FocusVisibility]); text fields pass [always], since you need to see where you type.
 * On a [fill] the focus color would vanish against (an amber "on" control in a theme whose focus is amber) it uses
 * `ink-strong`.
 */
@Composable
internal fun Modifier.focusRing(state: ControlState, shape: Shape, fill: Color? = null, always: Boolean = false): Modifier {
    if (!state.focused || !(always || FocusVisibility.keyboard)) return this
    val focus = Stretch.colors.focus
    val color = if (fill != null && contrast(focus, fill) < 3f) Stretch.colors.inkStrong else focus
    val width = Stretch.stroke + focusGrowth
    return drawWithContent {
        drawContent()
        val stroke = width.toPx()
        val inset = stroke / 2
        val outline = shape.createOutline(Size(size.width - stroke, size.height - stroke), layoutDirection, this)
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
 * text field included, are left alone. It also tells [FocusVisibility] whether the last input was a key (a key
 * without Ctrl, Alt or Meta) or a pointer press. Put it on the window or app root.
 */
fun Modifier.focusHome(): Modifier = composed {
    val home = remember { FocusRequester() }
    LaunchedEffect(Unit) { home.requestFocus() }
    onPreviewKeyEvent {
        if (it.type == KeyEventType.KeyDown && it.key !in modifierKeys && !it.isCtrlPressed && !it.isAltPressed && !it.isMetaPressed) {
            FocusVisibility.keyboard = true
        }
        false
    }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                FocusVisibility.keyboard = false
            }
        }
        .focusRequester(home)
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

/** Whether the layout runs right to left. */
@Composable
@ReadOnlyComposable
internal fun isRtl(): Boolean = LocalLayoutDirection.current == LayoutDirection.Rtl

/**
 * Flips a control drawn left to right, such as a slider, so it runs right to left in an RTL layout. Pointer
 * positions flip with it, so the control's own maths stays left to right.
 */
internal fun Modifier.mirrored(rtl: Boolean): Modifier = if (rtl) graphicsLayer(scaleX = -1f) else this

/** [key] with Left and Right swapped in a right-to-left control, so an arrow moves a knob the way it points. */
internal fun along(key: Key, rtl: Boolean): Key = when {
    !rtl -> key
    key == Key.DirectionLeft -> Key.DirectionRight
    key == Key.DirectionRight -> Key.DirectionLeft
    else -> key
}
