package dev.chieppa.stretch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchSizes

/**
 * A field that records a key press, the base of a shortcut field. At rest it shows [caps] as key caps, or
 * [placeholder] when there are none; clicking it (or Enter or Space while it has focus) turns on [listening], and
 * it shows [prompt] while every key-down goes to [onKey] before anything else sees it. The caller decides what a
 * key means and turns listening off; leaving the field turns it off too.
 */
@Composable
fun KeyCaptureField(
    caps: List<String>?,
    listening: Boolean,
    onListeningChange: (Boolean) -> Unit,
    onKey: (KeyEvent) -> Boolean,
    contentDescription: String,
    placeholder: String,
    prompt: String,
    modifier: Modifier = Modifier,
) {
    val c = Stretch.colors
    val state = rememberControlState()
    val shape = Stretch.shapes.ctlShape
    LaunchedEffect(state.focused) { if (!state.focused && listening) onListeningChange(false) }
    Box(
        modifier
            .widthIn(min = 120.dp)
            .height(StretchSizes.ctl)
            .focusRing(state, shape, always = true)
            .clip(shape)
            .background(if (listening) c.surfaceControlHi else c.surfaceFloat)
            .border(Stretch.stroke, if (listening) c.lineStrong else c.lineControl, shape)
            .hoverable(state.source)
            .onPreviewKeyEvent { if (listening) onKey(it) else false }
            .clickable(state.source, null, role = Role.Button) { onListeningChange(true) }
            .semantics {
                this.contentDescription = contentDescription
                stateDescription = if (listening) prompt else caps?.joinToString(" ") ?: placeholder
            }
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        when {
            listening -> Label(prompt, Stretch.type.body, c.inkStrong)
            caps.isNullOrEmpty() -> Label(placeholder, Stretch.type.body, c.inkSubtle)
            else -> Kbd(caps)
        }
    }
}
