package dev.chieppa.stretch.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint

/** The code point a desktop key event carries when its key has no character. */
private const val NO_CHARACTER = 0xFFFF

private val functionKeys = setOf(
    Key.F1, Key.F2, Key.F3, Key.F4, Key.F5, Key.F6, Key.F7, Key.F8, Key.F9, Key.F10, Key.F11, Key.F12,
)

/**
 * Keeps the keys typed into a text field from reaching key handlers above it, such as a plain-letter shortcut. A text
 * field takes its text from a separate typed event and passes on the key press itself, so put this on every
 * `BasicTextField`. Chords with Ctrl, Alt or Meta, and keys that type nothing, such as Esc or F1, still pass.
 */
fun Modifier.holdsTyping(): Modifier = onKeyEvent { it.typesCharacter() }

private fun KeyEvent.typesCharacter(): Boolean {
    if (type != KeyEventType.KeyDown || isCtrlPressed || isAltPressed || isMetaPressed || key in functionKeys) return false
    val code = utf16CodePoint
    return code != NO_CHARACTER && !code.toChar().isISOControl()
}
