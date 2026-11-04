@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.chieppa.stretch.frame

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import dev.chieppa.stretch.core.KeyChord

private fun browserIsOnMac(): Boolean = js("navigator.platform.startsWith('Mac')")

internal val isMac = browserIsOnMac()

private val letterKeys = listOf(
    Key.A, Key.B, Key.C, Key.D, Key.E, Key.F, Key.G, Key.H, Key.I, Key.J, Key.K, Key.L, Key.M,
    Key.N, Key.O, Key.P, Key.Q, Key.R, Key.S, Key.T, Key.U, Key.V, Key.W, Key.X, Key.Y, Key.Z,
)

private val digitKeys = listOf(
    Key.Zero, Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six, Key.Seven, Key.Eight, Key.Nine,
)

private val functionKeys = listOf(
    Key.F1, Key.F2, Key.F3, Key.F4, Key.F5, Key.F6, Key.F7, Key.F8, Key.F9, Key.F10, Key.F11, Key.F12,
)

/** Key names as chords spell them, the same as on the desktop, so a keymap carries over. Modifiers have none. */
private val namedKeys: Map<Key, String> = buildMap {
    letterKeys.forEachIndexed { i, key -> put(key, ('A' + i).toString()) }
    digitKeys.forEachIndexed { i, key -> put(key, i.toString()) }
    functionKeys.forEachIndexed { i, key -> put(key, "F${i + 1}") }
    put(Key.Escape, "Esc")
    put(Key.Delete, "Delete")
    put(Key.Backspace, "Backspace")
    put(Key.Enter, "Enter")
    put(Key.Spacebar, "Space")
    put(Key.Tab, "Tab")
    put(Key.Slash, "/")
    put(Key.Backslash, "Back Slash")
    put(Key.Comma, "Comma")
    put(Key.Period, "Period")
    put(Key.Minus, "Minus")
    put(Key.Equals, "Equals")
    put(Key.Semicolon, "Semicolon")
    put(Key.Apostrophe, "Quote")
    put(Key.Grave, "Back Quote")
    put(Key.LeftBracket, "Open Bracket")
    put(Key.RightBracket, "Close Bracket")
    put(Key.DirectionLeft, "Left")
    put(Key.DirectionRight, "Right")
    put(Key.DirectionUp, "Up")
    put(Key.DirectionDown, "Down")
    put(Key.PageUp, "Page Up")
    put(Key.PageDown, "Page Down")
    put(Key.MoveHome, "Home")
    put(Key.MoveEnd, "End")
    put(Key.Insert, "Insert")
}

internal actual fun KeyEvent.chord(): KeyChord? {
    if (type != KeyEventType.KeyDown) return null
    val name = namedKeys[key] ?: return null
    return KeyChord(
        key = name,
        ctrl = if (isMac) isMetaPressed else isCtrlPressed,
        shift = isShiftPressed,
        alt = isAltPressed,
        meta = if (isMac) isCtrlPressed else isMetaPressed,
    )
}

internal actual fun modifierCap(name: String): String = if (!isMac) name else when (name) {
    "Ctrl" -> "\u2318"
    "Alt" -> "\u2325"
    "Shift" -> "\u21E7"
    "Meta" -> "\u2303"
    else -> name
}
