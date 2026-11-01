package dev.chieppa.stretch.frame

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import dev.chieppa.stretch.core.KeyChord
import dev.chieppa.stretch.frame.generated.Res
import dev.chieppa.stretch.frame.generated.shortcut_none
import dev.chieppa.stretch.frame.generated.shortcut_prompt
import dev.chieppa.stretch.ui.KeyCaptureField
import org.jetbrains.compose.resources.stringResource

/**
 * A field that records a keyboard shortcut. Click it and press the keys: the first chord with a key other than a
 * modifier is taken. While it listens, Esc cancels and Backspace or Delete clear the shortcut; Tab still moves on.
 */
@Composable
fun ShortcutField(chord: KeyChord?, onChordChange: (KeyChord?) -> Unit, contentDescription: String, modifier: Modifier = Modifier) {
    var listening by remember { mutableStateOf(false) }
    KeyCaptureField(
        caps = chord?.caps(),
        listening = listening,
        onListeningChange = { listening = it },
        onKey = { event -> capture(event, { onChordChange(it) }) { listening = false } },
        contentDescription = contentDescription,
        placeholder = stringResource(Res.string.shortcut_none),
        prompt = stringResource(Res.string.shortcut_prompt),
        modifier = modifier,
    )
}

/** What a key press does to a listening shortcut field. True when the field used it. */
internal fun capture(event: KeyEvent, set: (KeyChord?) -> Unit, stop: () -> Unit): Boolean {
    if (event.type != KeyEventType.KeyDown) return true
    val chord = event.chord() ?: return true
    val plain = !chord.ctrl && !chord.alt && !chord.shift && !chord.meta
    when {
        plain && chord.key == "Tab" -> { stop(); return false }
        plain && chord.key == "Esc" -> stop()
        plain && (chord.key == "Backspace" || chord.key == "Delete") -> { set(null); stop() }
        else -> { set(chord); stop() }
    }
    return true
}
