package dev.chieppa.stretch.frame

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset

/**
 * Where a [dropTarget] holds a dragged item's preview while the drag is over it: the preview's point at [anchor], a
 * fraction of its size, sits on [at], in pixels from the target's top left. The default anchor centres it there.
 */
@Immutable
data class DropSnap(val at: Offset, val anchor: Offset = Offset(0.5f, 0.5f))
