package dev.chieppa.stretch.frame

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.unit.IntSize
import kotlin.reflect.KClass

/**
 * The drags of [dragSource] items onto [dropTarget]s in one window. A [DragAndDropHost] at the window's root keeps one;
 * `StretchWindow`, its pane and drawer windows, and `stretchPage` each have one already.
 */
@Stable
class DragAndDrop internal constructor() {
    internal var session by mutableStateOf<DragSession?>(null)
        private set

    internal val targets = mutableListOf<DropTargetEntry>()

    /** The item being dragged, while a drag is under way and until a cancelled one has gone back; otherwise null. */
    val item: Any? get() = session?.item

    /** Ends the drag under way, if any, as Esc does: no target takes it, and its preview goes back where it came from. */
    fun cancel() {
        session?.let(::cancel)
    }

    internal fun begin(item: Any, preview: @Composable () -> Unit, size: IntSize, grab: Offset, start: Offset, pointer: Offset): DragSession {
        val drag = DragSession(item, preview, size, grab, start)
        session = drag
        moveTo(drag, pointer)
        return drag
    }

    internal fun moveTo(drag: DragSession, pointer: Offset) {
        if (session !== drag || drag.returning) return
        drag.pointer = pointer
        val target = targetAt(drag.item, pointer)
        val previous = drag.hovered
        if (previous != null && previous !== target) previous.onHover(null, Offset.Zero)
        drag.hovered = target
        target?.let { it.onHover(drag.item, it.local(pointer)) }
    }

    /** Lets go of [drag] where its pointer is: the target there takes it, or it goes back. */
    internal fun drop(drag: DragSession) {
        if (session !== drag || drag.returning) return
        val target = drag.hovered
        drag.hovered = null
        target?.onHover(null, Offset.Zero)
        val taken = target != null && target.onDrop(drag.item, target.local(drag.pointer))
        if (taken) session = null else drag.returning = true
    }

    internal fun cancel(drag: DragSession) {
        if (session !== drag || drag.returning) return
        drag.hovered?.onHover(null, Offset.Zero)
        drag.hovered = null
        drag.returning = true
    }

    /** Clears [drag] once its preview is back where it started. */
    internal fun finish(drag: DragSession) {
        if (session === drag) session = null
    }

    internal fun remove(target: DropTargetEntry) {
        targets.remove(target)
        val drag = session ?: return
        if (drag.hovered === target) drag.hovered = null
    }

    private fun targetAt(item: Any, pointer: Offset): DropTargetEntry? {
        val live = targets.filter { it.coordinates?.isAttached == true }
        val index = innermostAt(live.map { it.coordinates!!.boundsInWindow() }, pointer) { live[it].takes(item) }
        return index?.let(live::get)
    }
}

/** A drag under way: the [item], the [preview] following the pointer, held at [grab] (a fraction of its size). */
internal class DragSession(val item: Any, val preview: @Composable () -> Unit, val size: IntSize, val grab: Offset, val start: Offset) {
    /** Where the pointer is, in the window. */
    var pointer by mutableStateOf(start)

    /** Whether the drag has ended with no target taking it, and its preview is going back to [start]. */
    var returning by mutableStateOf(false)

    var hovered: DropTargetEntry? = null
}

/** One [dropTarget] as the drags see it, its callbacks kept current by its modifier. */
internal class DropTargetEntry(
    val type: KClass<*>,
    var accepts: (Any) -> Boolean,
    var onHover: (Any?, Offset) -> Unit,
    var onDrop: (Any, Offset) -> Boolean,
) {
    var coordinates: LayoutCoordinates? = null

    fun takes(item: Any) = type.isInstance(item) && accepts(item)

    fun local(pointer: Offset): Offset = coordinates?.takeIf { it.isAttached }?.windowToLocal(pointer) ?: Offset.Zero
}

/** The window's [DragAndDrop], or null outside a [DragAndDropHost], where drag sources do nothing. */
val LocalDragAndDrop = staticCompositionLocalOf<DragAndDrop?> { null }
