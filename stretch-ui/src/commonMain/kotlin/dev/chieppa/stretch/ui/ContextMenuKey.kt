package dev.chieppa.stretch.ui

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

/** The keyboard's Menu key, as this platform reports it. */
internal expect val MenuKey: Key

/** Whether this key press opens the context menu of what has the focus: Shift F10, or the keyboard's Menu key. */
fun KeyEvent.opensContextMenu(): Boolean =
    type == KeyEventType.KeyDown && ((key == Key.F10 && isShiftPressed) || key == MenuKey || key == Key.Menu)
