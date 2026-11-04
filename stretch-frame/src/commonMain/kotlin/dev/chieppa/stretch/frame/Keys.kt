package dev.chieppa.stretch.frame

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import dev.chieppa.stretch.core.Action
import dev.chieppa.stretch.core.KeyChord
import dev.chieppa.stretch.core.Keymap

/** The chord a key-down event spells, or null for a modifier on its own or a key-up. */
internal expect fun KeyEvent.chord(): KeyChord?

/**
 * Runs the action bound to a pressed chord in [mode]. Attach it near the root: it sees keys only after focused
 * controls have passed on them. Stretch's text fields hold what is typed into them, so typing never triggers a
 * shortcut; a raw `BasicTextField` needs `Modifier.holdsTyping`.
 */
fun Modifier.actionKeys(keymap: Keymap, mode: String?): Modifier = onKeyEvent { event ->
    val chord = event.chord() ?: return@onKeyEvent false
    val action = keymap.resolve(chord, mode) ?: return@onKeyEvent false
    action.run()
    true
}

/** Turns an action's text key into the label people see. Apps provide it from their string resources. */
val LocalActionText = staticCompositionLocalOf<(String) -> String> { { it } }

/** Key caps for an action's first key, as menus and tooltips show them. */
fun Keymap.capsFor(action: Action): List<String>? = keysFor(action.id).firstOrNull()?.caps()

/**
 * The key caps for a chord on this platform. Ctrl in a chord is the platform's main shortcut modifier, so on macOS it
 * reads and works as Cmd, and Meta is the Control key.
 */
fun KeyChord.caps(): List<String> = toString().split(' ').map { if (it == key) it else modifierCap(it) }

internal expect fun modifierCap(name: String): String
