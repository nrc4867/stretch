package dev.chieppa.stretch.frame

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf

/** Which [SubMenuItem] of the menu panel around it is open, so hovering another item of that panel closes it. */
internal val LocalOpenSubMenu = compositionLocalOf<MutableState<Any?>?> { null }
