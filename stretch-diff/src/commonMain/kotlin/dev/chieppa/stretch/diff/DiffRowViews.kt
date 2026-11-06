package dev.chieppa.stretch.diff

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.code.CodeHighlight
import dev.chieppa.stretch.code.CodeLineBody
import dev.chieppa.stretch.code.CodeLineNumber
import dev.chieppa.stretch.code.CodeLook
import dev.chieppa.stretch.code.CodeMetrics
import dev.chieppa.stretch.code.CodeToken
import dev.chieppa.stretch.code.LineTone
import dev.chieppa.stretch.code.rememberCodeLook
import dev.chieppa.stretch.theme.StretchSpacing
import dev.chieppa.stretch.theme.Icon
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.ui.Label

/** The colors lines draw with, gathered once for all the rows. */
internal class LineLook(val code: CodeLook, val addStrong: Color, val delStrong: Color, val blank: Color)

@Composable
internal fun rememberLineLook(): LineLook {
    val c = Stretch.colors
    val code = rememberCodeLook()
    return remember(c, code) { LineLook(code, c.diffAddStrong, c.diffDelStrong, c.surfaceCanvas) }
}

/** The wash behind a line: [tone] when an app or the selection gives one, otherwise by what changed. */
private fun LineLook.wash(kind: LineKind?, tone: LineTone?): Color = when {
    tone != null -> code.wash(tone)
    kind == LineKind.Added -> code.wash(LineTone.Added)
    kind == LineKind.Removed -> code.wash(LineTone.Removed)
    kind == LineKind.Context -> Color.Transparent
    else -> blank
}

private fun LineLook.strong(kind: LineKind) = if (kind == LineKind.Added) addStrong else delStrong

private fun signOf(kind: LineKind) = when (kind) {
    LineKind.Added -> "+"
    LineKind.Removed -> "−"
    LineKind.Context -> " "
}

/** What an app and the viewer add to one line: its wash, its gutter mark and how its numbers answer the pointer. */
internal class LineExtras(
    val tone: LineTone? = null,
    val gutter: (@Composable () -> Unit)? = null,
    val gutterWidth: Dp = StretchSpacing.space16,
    val oldNumber: Modifier = Modifier,
    val newNumber: Modifier = Modifier,
    val finds: List<CodeHighlight> = emptyList(),
)

@Composable
private fun Gutter(extras: LineExtras, metrics: CodeMetrics) {
    val gutter = extras.gutter ?: return
    Box(Modifier.width(extras.gutterWidth).heightIn(min = metrics.lineHeight), contentAlignment = Alignment.Center) { gutter() }
}

@Composable
private fun Sign(kind: LineKind, look: LineLook) {
    val c = Stretch.colors
    val ink = when (kind) {
        LineKind.Added -> c.diffAddInk
        LineKind.Removed -> c.diffDelInk
        else -> look.code.muted
    }
    BasicText(signOf(kind), Modifier.width(StretchSpacing.space16), style = look.code.style.merge(TextStyle(color = ink, textAlign = TextAlign.Center)))
}

@Composable
private fun LineCode(line: DiffLine, tokens: List<CodeToken>, finds: List<CodeHighlight>, metrics: CodeMetrics, look: LineLook, wrap: Boolean, scroll: ScrollState, modifier: Modifier) {
    val text = remember(line, tokens, finds, look) { lineText(line, tokens, look.code.code, look.strong(line.kind), finds) }
    CodeLineBody(text, metrics, look.code, wrap, scroll, modifier)
}

@Composable
internal fun UnifiedLine(line: DiffLine, tokens: List<CodeToken>, metrics: CodeMetrics, look: LineLook, wrap: Boolean, scroll: ScrollState, extras: LineExtras) {
    Row(Modifier.fillMaxWidth().heightIn(min = metrics.lineHeight).background(look.wash(line.kind, extras.tone)).semantics { stateDescription = line.kind.name }) {
        DisableSelection {
            Gutter(extras, metrics)
            val selected = extras.tone == LineTone.Selected
            CodeLineNumber(line.oldNumber, metrics, look.code, extras.oldNumber, strong = selected)
            CodeLineNumber(line.newNumber, metrics, look.code, extras.newNumber, strong = selected)
            Sign(line.kind, look)
        }
        LineCode(line, tokens, extras.finds, metrics, look, wrap, scroll, Modifier.weight(1f))
    }
}

@Composable
internal fun RowScope.SplitSide(side: DiffSide, line: DiffLine?, tokens: List<CodeToken>, metrics: CodeMetrics, look: LineLook, wrap: Boolean, scroll: ScrollState, extras: LineExtras) {
    Row(Modifier.weight(1f).fillMaxHeight().background(look.wash(line?.kind, extras.tone)).semantics { if (line != null) stateDescription = line.kind.name }) {
        if (line == null) return@Row
        DisableSelection {
            Gutter(extras, metrics)
            val number = if (side == DiffSide.Old) line.oldNumber else line.newNumber
            CodeLineNumber(number, metrics, look.code, if (side == DiffSide.Old) extras.oldNumber else extras.newNumber, strong = extras.tone == LineTone.Selected)
            Sign(line.kind, look)
        }
        LineCode(line, tokens, extras.finds, metrics, look, wrap, scroll, Modifier.weight(1f))
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
        Label("+${file.added}", Stretch.type.valueSm, c.diffAddInk)
        Label("−${file.removed}", Stretch.type.valueSm, c.diffDelInk)
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
