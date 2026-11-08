package dev.chieppa.stretch.ui

import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/**
 * A control's [Label]: one line, cut short, and never part of a text selection, even when the control sits in
 * selectable content such as a card under a line of code. An app's own control labels its face with it.
 */
@Composable
fun ControlLabel(text: String, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    DisableSelection { Label(text, style, color, modifier) }
}
