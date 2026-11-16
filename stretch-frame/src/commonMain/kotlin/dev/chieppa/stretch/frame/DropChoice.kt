package dev.chieppa.stretch.frame

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

/**
 * Which of the targets with [bounds] a drag at [pointer] is over: of those that hold the point and [take] the item,
 * the smallest, so a target inside another wins over it; of equal ones, the last. Null when none does.
 */
internal fun innermostAt(bounds: List<Rect>, pointer: Offset, take: (Int) -> Boolean): Int? =
    bounds.indices
        .filter { bounds[it].contains(pointer) && take(it) }
        .minWithOrNull(compareBy<Int> { bounds[it].width * bounds[it].height }.thenByDescending { it })

/** Where a drag's preview of [size] goes so the pointer at [pointer] holds it at [grab], a fraction of its size. */
internal fun previewTopLeft(pointer: Offset, grab: Offset, size: Size): Offset =
    pointer - Offset(grab.x * size.width, grab.y * size.height)
