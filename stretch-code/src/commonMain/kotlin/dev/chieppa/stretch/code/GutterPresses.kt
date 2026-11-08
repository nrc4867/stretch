package dev.chieppa.stretch.code

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Takes every press that lands here once what is inside has seen it, so a press in a code view's gutter, beside or on
 * a line number, never starts a text selection in the code or scrolls it. Put it before the gutter's own input.
 */
fun Modifier.gutterPresses(): Modifier = pointerInput(Unit) {
    awaitEachGesture { awaitFirstDown(requireUnconsumed = false).consume() }
}
