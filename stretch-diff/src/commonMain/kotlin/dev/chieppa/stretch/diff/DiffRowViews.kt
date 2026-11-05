package dev.chieppa.stretch.diff

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.code.CodeColors
import dev.chieppa.stretch.code.CodeToken
import dev.chieppa.stretch.code.codeColors
import dev.chieppa.stretch.theme.Icon
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.ui.Label
import kotlinx.coroutines.launch

/** The colors lines draw with, gathered once for all the rows. */
internal class LineLook(val code: CodeColors, val style: TextStyle, val ink: Color, val muted: Color, val addWash: Color, val addStrong: Color, val delWash: Color, val delStrong: Color, val blank: Color)

@Composable
internal fun rememberLineLook(): LineLook {
    val c = Stretch.colors
    val code = codeColors
    val style = Stretch.type.valueSm
    return remember(c, style) {
        LineLook(code, style.merge(TextStyle(color = c.ink)), c.ink, c.inkSubtle, c.diffAddWash, c.diffAddStrong, c.diffDelWash, c.diffDelStrong, c.surfaceCanvas)
    }
}

private fun LineLook.wash(kind: LineKind?) = when (kind) {
    LineKind.Added -> addWash
    LineKind.Removed -> delWash
    LineKind.Context -> Color.Transparent
    null -> blank
}

private fun LineLook.strong(kind: LineKind) = if (kind == LineKind.Added) addStrong else delStrong

private fun signOf(kind: LineKind) = when (kind) {
    LineKind.Added -> "+"
    LineKind.Removed -> "−"
    LineKind.Context -> " "
}

@Composable
private fun Number(value: Int?, metrics: DiffMetrics, look: LineLook, modifier: Modifier = Modifier) {
    BasicText(
        value?.toString().orEmpty(),
        modifier.width(metrics.numberWidth).padding(horizontal = 6.dp),
        style = look.style.merge(TextStyle(color = look.muted, textAlign = TextAlign.End)),
        maxLines = 1,
    )
}

@Composable
private fun Code(line: DiffLine, tokens: List<CodeToken>, metrics: DiffMetrics, look: LineLook, wrap: Boolean, scroll: ScrollState, modifier: Modifier) {
    val text = remember(line, tokens, look) { lineText(line, tokens, look.code, look.strong(line.kind)) }
    if (wrap) {
        BasicText(text, modifier, style = look.style)
    } else {
        Box(modifier.clipToBounds().horizontalScroll(scroll)) {
            BasicText(text, Modifier.width(metrics.codeWidth), style = look.style, softWrap = false, overflow = TextOverflow.Clip)
        }
    }
}

@Composable
internal fun UnifiedLine(line: DiffLine, tokens: List<CodeToken>, metrics: DiffMetrics, look: LineLook, wrap: Boolean, scroll: ScrollState, onNumberClick: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().heightIn(min = metrics.lineHeight).background(look.wash(line.kind)).semantics { stateDescription = line.kind.name }) {
        DisableSelection {
            val click = if (onNumberClick != null) Modifier.clickable(role = Role.Button, onClick = onNumberClick) else Modifier
            Row(click) {
                Number(line.oldNumber, metrics, look)
                Number(line.newNumber, metrics, look)
            }
            BasicText(signOf(line.kind), Modifier.width(16.dp), style = look.style.merge(TextStyle(color = look.muted, textAlign = TextAlign.Center)))
        }
        Code(line, tokens, metrics, look, wrap, scroll, Modifier.weight(1f))
    }
}

@Composable
internal fun RowScope.SplitSide(line: DiffLine?, tokens: List<CodeToken>, metrics: DiffMetrics, look: LineLook, wrap: Boolean, scroll: ScrollState, file: FileDiff, hunk: DiffHunk, slots: DiffSlots) {
    Row(Modifier.weight(1f).fillMaxHeight().background(look.wash(line?.kind)).semantics { if (line != null) stateDescription = line.kind.name }) {
        if (line == null) return@Row
        DisableSelection {
            val click = slots.onLineClick
            val modifier = if (click != null) Modifier.clickable(role = Role.Button) { click(DiffLineRef(file, hunk, line)) } else Modifier
            Number(if (line.kind == LineKind.Removed) line.oldNumber else line.newNumber, metrics, look, modifier)
            BasicText(signOf(line.kind), Modifier.width(16.dp), style = look.style.merge(TextStyle(color = look.muted, textAlign = TextAlign.Center)))
        }
        Code(line, tokens, metrics, look, wrap, scroll, Modifier.weight(1f))
    }
}

@Composable
internal fun FileHeaderRow(row: DiffRow.FileHeader, state: DiffViewerState, strings: DiffStrings, slots: DiffSlots) {
    val c = Stretch.colors
    val file = row.file
    val path = if (file.status == FileStatus.Renamed || file.status == FileStatus.Copied) "${file.oldPath} → ${file.newPath}" else file.path
    Row(
        Modifier
            .fillMaxWidth()
            .background(c.surfaceControl)
            .clickable(role = Role.Button, onClickLabel = strings.toggleFile) { state.toggleFile(row.fileIndex) }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(if (row.collapsed) StretchIcons.ChevronRight else StretchIcons.ChevronDown, c.inkMuted, size = 12.dp)
        Label(file.status.name.first().toString(), Stretch.type.valueSm, c.inkMuted)
        Label(path, Stretch.type.valueSm, c.inkStrong, Modifier.weight(1f))
        Label("+${file.added}", Stretch.type.valueSm, c.deltaUp)
        Label("−${file.removed}", Stretch.type.valueSm, c.deltaDown)
        slots.fileActions?.invoke(this, file)
    }
}

@Composable
internal fun HunkHeaderRow(row: DiffRow.HunkHeader, file: FileDiff, slots: DiffSlots) {
    val c = Stretch.colors
    DisableSelection {
        Row(
            Modifier.fillMaxWidth().background(c.surfaceCanvas).padding(horizontal = 10.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Label(row.hunk.header, Stretch.type.valueSm, c.inkSubtle, Modifier.weight(1f))
            slots.hunkActions?.invoke(this, file, row.hunk)
        }
    }
}

@Composable
internal fun FoldRow(row: DiffRow.Fold, strings: DiffStrings, onUnfold: () -> Unit) {
    val c = Stretch.colors
    DisableSelection {
        Row(
            Modifier.fillMaxWidth().background(c.surfaceCanvas).clickable(role = Role.Button, onClick = onUnfold).padding(horizontal = 10.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(StretchIcons.More, c.inkSubtle, size = 12.dp)
            Label(strings.unfold(row.lineCount), Stretch.type.bodySm, c.inkSubtle)
        }
    }
}

/** A thin strip beside the list marking where changes are in the whole diff. Clicking jumps there. */
@Composable
internal fun ChangeRuler(rows: List<DiffRow>, state: DiffViewerState) {
    val c = Stretch.colors
    val scope = rememberCoroutineScope()
    val marks = remember(rows) { rulerMarks(rows) }
    Canvas(
        Modifier
            .width(10.dp)
            .fillMaxHeight()
            .background(c.surfaceCanvas)
            .pointerInput(rows) {
                detectTapGestures { at ->
                    val row = (at.y / size.height * rows.size).toInt().coerceIn(0, (rows.size - 1).coerceAtLeast(0))
                    scope.launch { state.list.scrollToItem(row) }
                }
            },
    ) {
        if (rows.isEmpty()) return@Canvas
        val rowHeight = size.height / rows.size
        for (mark in marks) {
            drawRect(
                if (mark.added) c.deltaUp else c.deltaDown,
                Offset(if (mark.added) size.width / 2 else 0f, mark.row * rowHeight),
                Size(size.width / 2, maxOf(rowHeight * mark.rows, 2f)),
            )
        }
    }
}

private class RulerMark(val row: Int, val rows: Int, val added: Boolean)

private fun rulerMarks(rows: List<DiffRow>): List<RulerMark> = buildList {
    fun scan(added: Boolean, on: (DiffRow) -> Boolean) {
        var start = -1
        for (index in 0..rows.size) {
            val hit = index < rows.size && on(rows[index])
            if (hit && start < 0) start = index
            if (!hit && start >= 0) {
                add(RulerMark(start, index - start, added))
                start = -1
            }
        }
    }
    scan(true) { row ->
        (row is DiffRow.Unified && row.line.kind == LineKind.Added) || (row is DiffRow.Split && row.right?.kind == LineKind.Added)
    }
    scan(false) { row ->
        (row is DiffRow.Unified && row.line.kind == LineKind.Removed) || (row is DiffRow.Split && row.left?.kind == LineKind.Removed)
    }
}
