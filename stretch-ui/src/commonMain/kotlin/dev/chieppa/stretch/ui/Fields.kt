package dev.chieppa.stretch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Icon
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchSizes

/** A one-line text or search field. No inline help text: anything that needs explaining goes in the tooltip. */
@Composable
fun TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    icon: ImageVector? = null,
    contentDescription: String = placeholder,
) {
    val c = Stretch.colors
    val state = rememberControlState()
    val shape = Stretch.shapes.ctlShape
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.focusRing(state, shape).semantics { this.contentDescription = contentDescription },
        singleLine = true,
        textStyle = Stretch.type.body.merge(TextStyle(color = c.inkStrong)),
        cursorBrush = SolidColor(c.inkStrong),
        interactionSource = state.source,
        decorationBox = { inner ->
            Row(
                Modifier
                    .height(StretchSizes.ctl)
                    .clip(shape)
                    .background(c.surfaceFloat)
                    .border(Stretch.stroke, c.lineControl, shape)
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(iconGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icon != null) Icon(icon, c.inkMuted)
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Label(placeholder, Stretch.type.body, c.inkSubtle)
                    inner()
                }
            }
        },
    )
}

/**
 * A number with a unit. Drag sideways to scrub, use the arrow keys to step (Shift steps ten), or click to type;
 * typing commits on Enter or when focus leaves. [onValueChangeFinished] marks the end of a scrub or an edit.
 */
@Composable
fun NumberField(
    value: Float,
    onValueChange: (Float) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    unit: String = "",
    step: Float = 1f,
    range: ClosedFloatingPointRange<Float> = -Float.MAX_VALUE..Float.MAX_VALUE,
    format: (Float) -> String = { formatNumber(it) },
    width: Dp = 76.dp,
    onValueChangeFinished: () -> Unit = {},
) {
    val c = Stretch.colors
    val state = rememberControlState()
    val shape = Stretch.shapes.ctlShape
    var editing by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    var hadFocus by remember { mutableStateOf(false) }
    val change by rememberUpdatedState(onValueChange)
    val finished by rememberUpdatedState(onValueChangeFinished)
    val current by rememberUpdatedState(value)
    val focus = remember { FocusRequester() }
    val style = Stretch.type.value.merge(TextStyle(color = c.inkStrong, textAlign = TextAlign.End))

    fun commit() {
        text.trim().toFloatOrNull()?.let { change(it.coerceIn(range)) }
        editing = false
        finished()
    }

    val frame = modifier
        .focusRing(state, shape)
        .width(width)
        .heightIn(min = StretchSizes.ctl)
        .clip(shape)
        .background(c.surfaceFloat)
        .border(Stretch.stroke, c.lineControl, shape)
        .semantics { this.contentDescription = contentDescription }

    if (editing) {
        LaunchedEffect(Unit) {
            hadFocus = false
            focus.requestFocus()
        }
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            modifier = frame
                .focusRequester(focus)
                .onFocusChanged {
                    if (it.isFocused) hadFocus = true else if (hadFocus && editing) commit()
                }
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                        editing = false
                        true
                    } else {
                        false
                    }
                },
            singleLine = true,
            textStyle = style,
            cursorBrush = SolidColor(c.inkStrong),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { commit() }),
            decorationBox = { inner -> Box(Modifier.padding(horizontal = 10.dp), Alignment.CenterEnd) { inner() } },
        )
    } else {
        Row(
            frame
                .hoverable(state.source)
                .clickable(state.source, indication = null) {
                    text = format(current)
                    editing = true
                }
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    val amount = if (event.isShiftPressed) step * 10 else step
                    when (event.key) {
                        Key.DirectionUp, Key.DirectionRight -> change((value + amount).coerceIn(range))
                        Key.DirectionDown, Key.DirectionLeft -> change((value - amount).coerceIn(range))
                        Key.Enter -> {
                            text = format(value)
                            editing = true
                            return@onKeyEvent true
                        }
                        else -> return@onKeyEvent false
                    }
                    finished()
                    true
                }
                .pointerInput(step, range) {
                    var start = 0f
                    var travelled = 0f
                    detectHorizontalDragGestures(
                        onDragStart = {
                            start = current
                            travelled = 0f
                        },
                        onDragEnd = { finished() },
                        onDragCancel = { finished() },
                    ) { _, dx ->
                        travelled += dx
                        change((start + (travelled / 4f).toInt() * step).coerceIn(range))
                    }
                }
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Label(format(value), style, c.inkStrong)
            if (unit.isNotEmpty()) Label(unit, Stretch.type.value, c.inkMuted)
        }
    }
}

/** Formats a number with at most two decimals and no trailing zeros. */
fun formatNumber(value: Float): String {
    val rounded = kotlin.math.round(value * 100) / 100
    val whole = rounded.toLong()
    if (rounded == whole.toFloat()) return whole.toString()
    return rounded.toString().trimEnd('0').trimEnd('.')
}

/** Key caps for a shortcut, one cap per key: Kbd(listOf("Ctrl", "K")). */
@Composable
fun Kbd(keys: List<String>, modifier: Modifier = Modifier) {
    val c = Stretch.colors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        for (key in keys) {
            val shape = RoundedCornerShape(4.dp)
            Box(
                Modifier
                    .heightIn(min = 18.dp)
                    .clip(shape)
                    .background(c.surfaceFloat)
                    .border(1.dp, c.lineStrong, shape)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Label(key, Stretch.type.valueSm, c.inkControl)
            }
        }
    }
}
