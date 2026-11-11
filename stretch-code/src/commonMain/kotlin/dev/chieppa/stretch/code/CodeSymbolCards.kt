package dev.chieppa.stretch.code

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Rect
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/** How long the pointer rests on a symbol before its card opens, in milliseconds. */
const val SYMBOL_CARD_DELAY_MS = 600L

/** How long a card stays open once the pointer leaves its symbol, so the pointer can move onto it, in milliseconds. */
const val SYMBOL_CARD_GRACE_MS = 300L

/** The symbol a card is open for: the character the pointer rested on and the [range] of the symbol there. */
data class CodeSymbolRest<G>(val spot: CodeSpot<G>, val range: CodeRange)

/**
 * The card a code view opens over a symbol the pointer rests on, when the app gives one. It opens once the pointer has
 * rested on a symbol [CodeSymbolPointer.symbolAt] names for [SYMBOL_CARD_DELAY_MS], never while Ctrl or Cmd is held or
 * a button is down. It stays while the pointer is on the symbol or on the card, and closes [SYMBOL_CARD_GRACE_MS]
 * after it leaves both, when the view scrolls, on a press in the code, or on Escape, unless it is [pinned].
 */
@Stable
class CodeSymbolCards<G> internal constructor(private val pointer: CodeSymbolPointer<G>) : SymbolCard {
    /** The symbol the card is open for, or null while none is. */
    var open by mutableStateOf<CodeSymbolRest<G>?>(null)
        private set

    /** Where [open]'s symbol was when the card opened, in [CodeSymbolPointer.inner]'s pixels. */
    var bounds by mutableStateOf<Rect?>(null)
        private set

    /** The app gives a card, so resting on a symbol opens one. */
    var enabled by mutableStateOf(false)

    /** The pointer is over the card. */
    var overCard by mutableStateOf(false)

    /** A mouse button is down in the code. */
    var pressing by mutableStateOf(false)

    override var pinned by mutableStateOf(false)

    override fun close() {
        open = null
        bounds = null
        pinned = false
        overCard = false
    }

    /** Closes the card unless it is pinned, as scrolling and pressing in the code do. */
    fun dismiss() {
        if (!pinned) close()
    }

    /** Opens and closes the card as the pointer moves, until cancelled. */
    suspend fun follow() {
        snapshotFlow { Rest(if (enabled) pointer.pointer else null, pointer.held || pressing, overCard, pinned) }.collectLatest { now ->
            if (now.pinned || now.overCard) return@collectLatest
            if (now.blocked) {
                if (open != null) close()
                return@collectLatest
            }
            val shown = open
            if (shown != null) {
                if (now.spot != null && now.spot.group == shown.spot.group && now.spot.line == shown.range.line &&
                    now.spot.column >= shown.range.start && now.spot.column < shown.range.end
                ) {
                    return@collectLatest
                }
                delay(SYMBOL_CARD_GRACE_MS)
                close()
            }
            val spot = now.spot ?: return@collectLatest
            delay(SYMBOL_CARD_DELAY_MS)
            val range = pointer.lookup(spot) ?: return@collectLatest
            val at = pointer.boundsOf(spot.group, range, pointer.inner) ?: return@collectLatest
            bounds = at
            open = CodeSymbolRest(spot, range)
        }
    }

    private data class Rest<G>(val spot: CodeSpot<G>?, val blocked: Boolean, val overCard: Boolean, val pinned: Boolean)
}
