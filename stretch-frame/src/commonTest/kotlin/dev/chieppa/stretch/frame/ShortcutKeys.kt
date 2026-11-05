package dev.chieppa.stretch.frame

import androidx.compose.ui.input.key.Key

/** The key that makes a chord's Ctrl on this platform: Command on macOS. */
internal val shortcutKey: Key get() = if (isMac) Key.MetaLeft else Key.CtrlLeft
