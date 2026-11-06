package dev.chieppa.stretch.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow

/**
 * A file path on one line that keeps its file name: when the path is too wide, folders drop out of its middle
 * (`src/main/…/ui/App.kt`), and only when the name alone is too wide does the name lose its own middle. Screen
 * readers hear the whole [path].
 */
@Composable
fun PathLabel(path: String, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val merged = style.merge(TextStyle(color = color))
    BoxWithConstraints(modifier.semantics { contentDescription = path }) {
        val room = constraints.maxWidth
        val shown = remember(path, merged, room) {
            val (folders, name) = splitPath(path)
            if (folders.isEmpty()) {
                path
            } else {
                middleEllipsis(folders, name) { measurer.measure(it, merged, softWrap = false, maxLines = 1).size.width <= room }
            }
        }
        BasicText(shown, style = merged, maxLines = 1, softWrap = false, overflow = TextOverflow.MiddleEllipsis)
    }
}
