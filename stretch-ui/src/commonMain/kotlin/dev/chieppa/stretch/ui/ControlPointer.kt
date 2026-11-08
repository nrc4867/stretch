package dev.chieppa.stretch.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon

/**
 * Shows the normal arrow while the pointer is over a control, whatever the content around it shows: a dialog header's
 * move cursor, or the text beam of code a control sits in. Every Stretch control carries it; an app's own clickable
 * control puts it before its click handling.
 */
fun Modifier.controlPointer(): Modifier = pointerHoverIcon(PointerIcon.Default)

/** Shows the pointing hand while the pointer is over something that reads as clickable in running content. */
fun Modifier.handPointer(): Modifier = pointerHoverIcon(PointerIcon.Hand)
