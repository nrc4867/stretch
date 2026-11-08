package dev.chieppa.stretch.code

import androidx.compose.ui.geometry.Rect

/**
 * A click through to a symbol in a [CodeViewer]: [position] is where it was clicked, or the caret for a key, and [range]
 * the symbol the app named there. [bounds] is where [range] shows, in pixels from the viewer's top left as it is laid
 * out with its modifier, to open a popup at, such as a `Popover` or `Menu` with `anchor = bounds` composed in a `Box`
 * the viewer fills. It is null only when the line could not be laid out in time, which a key press far off screen can meet.
 */
data class CodeSymbolClick(val position: CodePosition, val range: CodeRange, val bounds: Rect?)
