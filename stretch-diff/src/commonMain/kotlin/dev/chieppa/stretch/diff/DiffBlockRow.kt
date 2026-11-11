package dev.chieppa.stretch.diff

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.chieppa.stretch.code.CodeBlockRow
import dev.chieppa.stretch.code.CodeLineNumber
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchSpacing

/**
 * The first row of [block], drawing it with [DiffSlots.lineBlock]: in one column beside both numbers, or on each side
 * of a split view beside that side's number. The row's own annotations follow.
 */
@Composable
internal fun DiffBlockRow(row: DiffRow, block: DiffBlock, context: DiffRowContext) {
    val draw = context.slots.lineBlock ?: return
    val metrics = context.metrics
    val look = context.look.code
    val slots = context.slots
    val refs = annotationsOf(row, context.files)
    val first = refs.firstOrNull() ?: return
    val line = first.line

    @Composable
    fun RowScope.gutter(ref: DiffLineRef) {
        slots.gutterMark?.let { mark ->
            Box(Modifier.width(slots.gutterWidth).heightIn(min = metrics.lineHeight), contentAlignment = Alignment.Center) { mark(ref) }
        }
    }
    if (row is DiffRow.Split) {
        Row(Modifier.fillMaxWidth()) {
            for ((index, side) in DiffSide.entries.withIndex()) {
                if (index > 0) Box(Modifier.width(Stretch.stroke).heightIn(min = metrics.lineHeight).background(Stretch.colors.linePanel))
                val ref = refs.firstOrNull { it.side == side } ?: first
                Box(Modifier.weight(1f)) {
                    CodeBlockRow(metrics, numbers = {
                        gutter(ref)
                        CodeLineNumber(line.numberOn(side), metrics, look)
                        Box(Modifier.width(StretchSpacing.space16))
                    }) { draw(block, side) }
                }
            }
        }
    } else {
        CodeBlockRow(metrics, numbers = {
            gutter(first)
            CodeLineNumber(line.oldNumber, metrics, look)
            CodeLineNumber(line.newNumber, metrics, look)
            Box(Modifier.width(StretchSpacing.space16))
        }) { draw(block, DiffSide.New) }
    }
    refs.forEach { ref -> slots.lineAnnotation?.invoke(ref) }
}

/** The lines a row shows, as its annotations are asked for: its one line in one column, each side's in a split view. */
internal fun annotationsOf(row: DiffRow, files: List<FileDiff>): List<DiffLineRef> = when (row) {
    is DiffRow.Unified -> files[row.fileIndex].let { file -> listOf(DiffLineRef(file, file.hunks[row.hunkIndex], row.line)) }
    is DiffRow.Split -> files[row.fileIndex].let { file ->
        val hunk = file.hunks[row.hunkIndex]
        listOfNotNull(row.left?.let { DiffLineRef(file, hunk, it, DiffSide.Old) }, row.right?.let { DiffLineRef(file, hunk, it, DiffSide.New) })
    }
    else -> emptyList()
}
