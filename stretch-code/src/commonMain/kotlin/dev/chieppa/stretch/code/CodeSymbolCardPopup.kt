package dev.chieppa.stretch.code

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import dev.chieppa.stretch.ui.StretchPopup

/** The gap between a symbol and its card, in pixels. */
private const val CARD_GAP = 4

/**
 * Shows [content] for the symbol [pointer]'s card is open for, under the symbol, or over it when there is no room
 * below, its start lined up with the symbol's. The content draws the card itself; emitting nothing shows nothing.
 * Compose it in the content [CodeSymbolPointer.inner] is the coordinates of.
 */
@Composable
fun <G> CodeSymbolCardPopup(pointer: CodeSymbolPointer<G>, content: @Composable (CodeSymbolRest<G>, SymbolCard) -> Unit) {
    val cards = pointer.card
    val open = cards.open ?: return
    val bounds = cards.bounds ?: return
    val area = bounds.roundToIntRect()
    StretchPopup(popupPositionProvider = remember(area) { UnderArea(area) }, properties = PopupProperties(focusable = false)) {
        Box(
            Modifier.pointerInput(cards) {
                awaitPointerEventScope {
                    while (true) {
                        when (awaitPointerEvent().type) {
                            PointerEventType.Enter -> cards.overCard = true
                            PointerEventType.Exit -> cards.overCard = false
                        }
                    }
                }
            },
        ) {
            content(open, cards)
        }
    }
}

/** Puts a popup under [area], a rectangle from the anchor's top left, or over it without room below, kept in the window. */
private class UnderArea(private val area: IntRect) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val target = area.translate(anchorBounds.topLeft)
        val x = target.left.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        val below = target.bottom + CARD_GAP
        val y = if (below + popupContentSize.height <= windowSize.height) below else (target.top - CARD_GAP - popupContentSize.height).coerceAtLeast(0)
        return IntOffset(x, y)
    }
}
