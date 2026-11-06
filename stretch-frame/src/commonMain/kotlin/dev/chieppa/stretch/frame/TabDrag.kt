package dev.chieppa.stretch.frame

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

/**
 * A rectangle in one window's pixels and the way to put that window's points on screen, so places in different
 * windows compare. The window can move after the rectangle is taken; [onScreen] asks where it is now.
 */
internal class Placed(val rect: Rect, val toScreen: (Offset) -> Offset) {
    fun onScreen(): Rect = Rect(toScreen(rect.topLeft), toScreen(rect.bottomRight))
}

/** Leaves points where they are: the screen of a single window, where places never leave it. */
internal val SameWindow: (Offset) -> Offset = { it }

/** Puts points of the window this is composed in on screen, for drags that cross windows. */
internal val LocalPaneScreen = staticCompositionLocalOf { SameWindow }

/**
 * The drag of one tab, shared by the [TabStrip]s that can take it. Each strip and tab reports where it is, so the
 * dragged tab can land in any of them, including strips in other windows of the same app.
 */
@Stable
class TabDragState {
    /** The tab being dragged, or null. */
    var tab by mutableStateOf<String?>(null)
        private set

    /** The strip the dragged tab came from. */
    internal var from by mutableStateOf<String?>(null)
        private set

    /** The pointer, in the source window's pixels. */
    internal var pointer by mutableStateOf(Offset.Zero)
        private set

    /** Where on the tab the pointer took it, in pixels. */
    internal var grab = Offset.Zero
        private set

    /** The dragged tab's size, in pixels. */
    internal var size = Size.Zero
        private set

    internal var toScreen: (Offset) -> Offset = SameWindow
        private set

    internal val strips = mutableMapOf<String, Placed>()
    internal val spans = mutableMapOf<String, MutableMap<String, Placed>>()

    /** The pointer on screen. */
    internal val screenPointer: Offset get() = toScreen(pointer)

    internal fun begin(tab: String, from: String, grab: Offset, pointer: Offset, size: Size, toScreen: (Offset) -> Offset) {
        this.grab = grab
        this.size = size
        this.toScreen = toScreen
        this.pointer = pointer
        this.from = from
        this.tab = tab
    }

    internal fun moveBy(amount: Offset) {
        pointer += amount
    }

    internal fun end() {
        tab = null
        from = null
    }

    /**
     * Where the dragged tab goes in strip [strip], whose tabs are [order], if the pointer is over that strip: its
     * index there, counted without the dragged tab.
     */
    internal fun indexIn(strip: String, order: List<String>, rtl: Boolean): Int? {
        val dragged = tab ?: return null
        return indexAt(strip, order, rtl, screenPointer, dragged)
    }

    /** Where a tab other than [dragged] goes in strip [strip], whose tabs are [order], for a point [at] on screen. */
    internal fun indexAt(strip: String, order: List<String>, rtl: Boolean, at: Offset, dragged: String?): Int? {
        val bounds = strips[strip]?.onScreen() ?: return null
        val slack = bounds.height / 2
        if (at.x < bounds.left || at.x > bounds.right || at.y < bounds.top - slack || at.y > bounds.bottom + slack) return null
        val placed = spans[strip].orEmpty()
        val others = order.filter { it != dragged }.mapNotNull { id -> placed[id]?.onScreen()?.let { it.left..it.right } }
        return stripIndex(others, at.x, rtl)
    }
}
