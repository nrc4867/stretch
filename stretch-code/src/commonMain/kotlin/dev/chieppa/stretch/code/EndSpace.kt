package dev.chieppa.stretch.code

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity

/**
 * The room a code view leaves after its last line so it scrolls [share] of its [height] past the end: none for 0, half
 * the height for 0.5, and at most the whole height, for a view to scroll its last line off the top.
 */
@Composable
fun endSpace(height: Int, share: Float): PaddingValues {
    if (share <= 0f || height <= 0) return PaddingValues()
    return PaddingValues(bottom = with(LocalDensity.current) { (height * share.coerceAtMost(1f)).toDp() })
}
