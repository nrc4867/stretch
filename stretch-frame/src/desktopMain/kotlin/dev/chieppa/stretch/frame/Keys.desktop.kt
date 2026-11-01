package dev.chieppa.stretch.frame

import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.type
import dev.chieppa.stretch.core.KeyChord
import java.awt.event.KeyEvent as AwtKeyEvent

private val modifierCodes = setOf(
    AwtKeyEvent.VK_SHIFT, AwtKeyEvent.VK_CONTROL, AwtKeyEvent.VK_ALT, AwtKeyEvent.VK_META, AwtKeyEvent.VK_ALT_GRAPH,
)

internal actual fun KeyEvent.chord(): KeyChord? {
    if (type != KeyEventType.KeyDown) return null
    val code = key.nativeKeyCode
    if (code in modifierCodes) return null
    val name = when (code) {
        AwtKeyEvent.VK_ESCAPE -> "Esc"
        AwtKeyEvent.VK_DELETE -> "Delete"
        AwtKeyEvent.VK_BACK_SPACE -> "Backspace"
        AwtKeyEvent.VK_ENTER -> "Enter"
        AwtKeyEvent.VK_SPACE -> "Space"
        AwtKeyEvent.VK_TAB -> "Tab"
        AwtKeyEvent.VK_SLASH -> "/"
        else -> AwtKeyEvent.getKeyText(code)
    }
    return KeyChord(
        key = if (name.length == 1) name.uppercase() else name,
        ctrl = if (isMac) isMetaPressed else isCtrlPressed,
        shift = isShiftPressed,
        alt = isAltPressed,
        meta = if (isMac) isCtrlPressed else isMetaPressed,
    )
}

internal val isMac = System.getProperty("os.name").orEmpty().startsWith("Mac")
internal val isWindows = System.getProperty("os.name").orEmpty().startsWith("Windows")

internal actual fun modifierCap(name: String): String = if (!isMac) name else when (name) {
    "Ctrl" -> "\u2318"
    "Alt" -> "\u2325"
    "Shift" -> "\u21E7"
    "Meta" -> "\u2303"
    else -> name
}
