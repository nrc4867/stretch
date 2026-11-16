package dev.chieppa.stretch.frame

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import kotlin.reflect.KClass

/**
 * Makes this element a place to drop items of type [T] dragged from a [dragSource] in the same window. A drag is over
 * the target while the pointer is inside the part of it that shows and [accepts] says yes to the item; where targets
 * overlap, the smallest one under the pointer that takes the item has it, so a target inside another wins over it.
 *
 * [onHover] hears the item and the pointer, in pixels from the target's top left, each time it moves over the target,
 * and null when the drag leaves, ends or is cancelled, so the target can mark where the item would land, or test what
 * of its own content is under the pointer, as a viewport does with its scene. Letting go over the target calls
 * [onDrop] with the item and the same point; it returns true when it took the item, and false sends the item's preview
 * back to where it came from. Items of other types pass over the target as if it were not there.
 */
inline fun <reified T : Any> Modifier.dropTarget(
    noinline accepts: (T) -> Boolean,
    noinline onHover: (T?, Offset) -> Unit = { _, _ -> },
    noinline onDrop: (T, Offset) -> Boolean,
): Modifier = typedDropTarget(T::class, accepts, onHover, onDrop)

/** [dropTarget] for items of [type]. */
@PublishedApi
@Suppress("UNCHECKED_CAST")
internal fun <T : Any> Modifier.typedDropTarget(
    type: KClass<T>,
    accepts: (T) -> Boolean,
    onHover: (T?, Offset) -> Unit,
    onDrop: (T, Offset) -> Boolean,
): Modifier = composed {
    val drags = LocalDragAndDrop.current ?: return@composed Modifier
    val entry = remember(drags, type) { DropTargetEntry(type, { true }, { _, _ -> }, { _, _ -> false }) }
    entry.accepts = { accepts(it as T) }
    entry.onHover = { item, at -> onHover(item as T?, at) }
    entry.onDrop = { item, at -> onDrop(item as T, at) }
    DisposableEffect(entry) {
        drags.targets += entry
        onDispose { drags.remove(entry) }
    }
    onGloballyPositioned { entry.coordinates = it }
}
