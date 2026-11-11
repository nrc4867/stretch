package dev.chieppa.stretch.code

import androidx.compose.runtime.Stable

/** A symbol card open over a code view, as its content sees it. */
@Stable
interface SymbolCard {
    /** Pinned, the card stays where it is, whatever the pointer does or the view scrolls, until it is closed. */
    var pinned: Boolean

    /** Closes the card. */
    fun close()
}
