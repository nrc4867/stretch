package dev.chieppa.stretch.frame

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/** How far beside a rail a dragged button still lands on it. Past this it has left the rails. */
internal val RailDragSlack = 48.dp

/** How far below a drawer window's top edge the pointer sits when the window opens where a rail button was dropped. */
internal const val WINDOW_DROP_GRAB = 16f

/** Where a dragged rail button lands if let go now. */
internal sealed interface RailDragTarget {
    /** A slot on the rail on [side]. */
    data class Slot(val side: Side, val drop: RailDrop) : RailDragTarget

    /** Away from both rails. */
    data object Away : RailDragTarget
}

/** The rail nearest to [x], if [x] is within [slack] of one. */
internal fun railNear(x: Float, rails: Map<Side, Rect>, slack: Float): Side? =
    rails.entries
        .filter { (_, rail) -> x >= rail.left - slack && x <= rail.right + slack }
        .minByOrNull { (_, rail) -> abs(x - rail.center.x) }
        ?.key

/** The window a drawer opens in when its rail button is dropped at [screen]: [width] by [height], under the pointer. */
internal fun windowUnderPointer(screen: Offset, width: Float, height: Float): WindowBounds =
    WindowBounds(screen.x - width / 2, screen.y - WINDOW_DROP_GRAB, width, height)

/**
 * The drag of one rail button, shared by the rails that can take it. Positions are in the window's pixels, so the
 * button can travel across the frame.
 */
@Stable
class RailDragState {
    internal var id by mutableStateOf<String?>(null)
        private set
    internal var from by mutableStateOf(Side.Left)
        private set
    internal var pointer by mutableStateOf(Offset.Zero)
        private set
    internal var grab = Offset.Zero
        private set

    internal val rails = mutableMapOf<Side, Rect>()
    private val items = mutableMapOf<Side, List<RailItem>>()

    internal fun report(side: Side, shown: List<RailItem>) {
        items[side] = shown
    }

    internal fun begin(id: String, from: Side, grab: Offset, pointer: Offset) {
        this.id = id
        this.from = from
        this.grab = grab
        this.pointer = pointer
    }

    internal fun moveBy(amount: Offset) {
        pointer += amount
    }

    internal fun end() {
        id = null
    }

    /** Where the button lands if let go now, on rails whose buttons are [slot] tall inside [pad] padding. */
    internal fun target(slot: Float, pad: Float, slack: Float): RailDragTarget {
        val side = railNear(pointer.x, rails, slack) ?: return RailDragTarget.Away
        val rail = rails.getValue(side)
        val rest = items[side].orEmpty().filter { it.id != id }
        val y = pointer.y - grab.y - rail.top
        return RailDragTarget.Slot(side, railDrop(y, rail.height, rest.count { !it.bottom }, rest.count { it.bottom }, slot, pad))
    }
}
