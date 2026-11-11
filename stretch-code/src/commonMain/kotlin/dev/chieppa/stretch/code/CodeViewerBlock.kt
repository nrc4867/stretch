package dev.chieppa.stretch.code

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * The run of lines [block] of a [CodeViewer] drawn by [CodeViewerSlots.lineBlock]: the first line's gutter mark and
 * number, then the block, indented as its first [line] is, then the annotations of its first line.
 */
@Composable
internal fun CodeViewerBlock(block: IntRange, line: String, tone: LineTone?, metrics: CodeMetrics, look: CodeLook, slots: CodeViewerSlots) {
    val draw = slots.lineBlock ?: return
    val first = block.first
    Column {
        CodeBlockRow(metrics, wash = look.wash(tone), indent = indentOf(line, metrics), numbers = {
            slots.gutterMark?.let { mark ->
                Box(Modifier.width(slots.gutterWidth).heightIn(min = metrics.lineHeight), contentAlignment = Alignment.Center) { mark(first) }
            }
            CodeLineNumber(first, metrics, look)
        }) { draw(block) }
        slots.lineAnnotation?.let { annotation -> DisableSelection { annotation(first) } }
    }
}
