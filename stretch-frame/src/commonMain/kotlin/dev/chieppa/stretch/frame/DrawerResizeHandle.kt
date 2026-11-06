package dev.chieppa.stretch.frame

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import dev.chieppa.stretch.frame.generated.Res
import dev.chieppa.stretch.frame.generated.resize
import org.jetbrains.compose.resources.stringResource

/** The inner edge of a side column. Reports the new width in dp; [sign] is -1 when dragging left grows it. */
@Composable
internal fun ResizeHandle(width: Float, sign: Float, onResize: (Float) -> Unit) {
    val density = LocalDensity.current.density
    val currentWidth by rememberUpdatedState(width)
    var start by remember { mutableFloatStateOf(0f) }
    EdgeHandle(vertical = false, stringResource(Res.string.resize), { start = currentWidth }) { px -> onResize(start + sign * px / density) }
}
