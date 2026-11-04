package dev.chieppa.stretch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch

/**
 * Several lines of plain text, for notes and descriptions. It starts [minLines] tall and grows to [maxLines], then
 * scrolls; a height from [modifier] overrides both. Enter starts a new line and Tab moves on to the next control. With
 * [error] it shows the error edge and icon, and the message in the icon's tooltip.
 */
@Composable
fun TextArea(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    contentDescription: String = placeholder,
    minLines: Int = 3,
    maxLines: Int = 10,
    error: String? = null,
) {
    val c = Stretch.colors
    val state = rememberControlState()
    val shape = Stretch.shapes.ctlShape
    val focus = LocalFocusManager.current
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .holdsTyping()
            .focusRing(state, shape, always = true)
            .clip(shape)
            .background(c.surfaceFloat)
            .border(Stretch.stroke, if (error != null) c.errorEdge else c.lineControl, shape)
            .onPreviewKeyEvent { event ->
                if (event.key != Key.Tab) return@onPreviewKeyEvent false
                if (event.type == KeyEventType.KeyDown) focus.moveFocus(if (event.isShiftPressed) FocusDirection.Previous else FocusDirection.Next)
                true
            }
            .semantics {
                this.contentDescription = contentDescription
                if (error != null) error(error)
            },
        minLines = minLines,
        maxLines = maxLines,
        textStyle = Stretch.type.body.merge(TextStyle(color = c.inkStrong)),
        cursorBrush = SolidColor(c.inkStrong),
        interactionSource = state.source,
        decorationBox = { inner ->
            Box(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                if (value.isEmpty()) Label(placeholder, Stretch.type.body, c.inkSubtle)
                inner()
                if (error != null) Box(Modifier.align(Alignment.TopEnd)) { ErrorMark(error) }
            }
        },
    )
}
