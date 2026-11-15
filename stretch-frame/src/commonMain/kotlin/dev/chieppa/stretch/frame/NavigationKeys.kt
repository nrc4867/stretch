package dev.chieppa.stretch.frame

import dev.chieppa.stretch.core.KeyChord

/**
 * The platform's keys for going back and forward through places visited, as browsers and editors bind them: Alt Left
 * and Alt Right on Windows and Linux, Cmd [ and Cmd ] on macOS. Pair them with [dev.chieppa.stretch.ui.onBackForwardButtons] for the mouse's
 * back and forward buttons.
 */
object NavigationKeys {
    val back: KeyChord get() = if (isMac) KeyChord.parse("Ctrl [") else KeyChord.parse("Alt Left")
    val forward: KeyChord get() = if (isMac) KeyChord.parse("Ctrl ]") else KeyChord.parse("Alt Right")
}
