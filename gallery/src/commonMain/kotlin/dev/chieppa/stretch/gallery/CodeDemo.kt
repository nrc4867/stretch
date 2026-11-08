package dev.chieppa.stretch.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.code.CodeLanguages
import dev.chieppa.stretch.code.CodeMark
import dev.chieppa.stretch.code.CodeText
import dev.chieppa.stretch.code.CodeViewer
import dev.chieppa.stretch.code.CodeViewerSlots
import dev.chieppa.stretch.code.CodeViewerState
import dev.chieppa.stretch.code.LineTone
import dev.chieppa.stretch.code.rememberCodeViewerState
import dev.chieppa.stretch.diff.LineKind
import dev.chieppa.stretch.diff.diffTexts
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.ui.Button
import dev.chieppa.stretch.ui.Label
import dev.chieppa.stretch.ui.Toggle
import kotlinx.coroutines.launch

/**
 * Two code viewers, old and new, with changed lines washed and barred, notes under lines and on the ruler, and scrolling
 * that can follow. The new one clicks through to names and shows decorations (see [NavigableCode]).
 */
@Composable
internal fun CodeDemo() {
    val old = remember { CodeText.of(List(12) { SampleDocuments.oldCode }.joinToString("\n")) }
    val new = remember { CodeText.of(List(12) { SampleDocuments.newCode }.joinToString("\n")) }
    val changes = remember {
        val lines = diffTexts(old.text, new.text, "Greeter.kt").hunks.flatMap { it.lines }
        lines.mapNotNull { if (it.kind == LineKind.Removed) it.oldNumber else null }.toSet() to
            lines.mapNotNull { if (it.kind == LineKind.Added) it.newNumber else null }.toSet()
    }
    val left = rememberCodeViewerState()
    val right = rememberCodeViewerState()
    var linked by remember { mutableStateOf(true) }
    var wrap by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf(setOf(4)) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(linked) {
        if (!linked) return@LaunchedEffect
        snapshotFlow { left.list.firstVisibleItemIndex to left.list.firstVisibleItemScrollOffset }.collect { (index, offset) ->
            right.alignLine(index + 1, offset)
        }
    }
    LaunchedEffect(wrap) {
        left.wrap = wrap
        right.wrap = wrap
    }
    val slots = CodeViewerSlots(
        gutterMark = { line -> if (line in notes) Box(Modifier.size(8.dp).background(Stretch.colors.accent, CircleShape)) },
        lineAnnotation = { line ->
            if (line in notes) {
                Label(
                    "A note on line $line. Click a line number to add or remove one.",
                    Stretch.type.body,
                    Stretch.colors.ink,
                    Modifier.fillMaxWidth().padding(8.dp).background(Stretch.colors.surfaceFloat, Stretch.shapes.cardShape).padding(12.dp),
                )
            }
        },
        onLineNumberClick = { line -> notes = if (line in notes) notes - line else notes + line },
    )
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Label("Follow scrolling", Stretch.type.bodySm, Stretch.colors.inkMuted)
            Toggle(linked, { linked = it }, "Follow scrolling")
            Label("Wrap", Stretch.type.bodySm, Stretch.colors.inkMuted)
            Toggle(wrap, { wrap = it }, "Wrap")
            Button("Go to line 120", { scope.launch { left.scrollToLine(120) } })
        }
        Row(Modifier.weight(1f).fillMaxWidth()) {
            Viewer(old, left, { if (it in changes.first) LineTone.Removed else null }, slots, notes.map { CodeMark(it, Stretch.colors.accent) })
            Box(Modifier.width(Stretch.stroke).fillMaxHeight().background(Stretch.colors.linePanel))
            NavigableCode(new, right, Modifier.weight(1f).fillMaxHeight()) { if (it in changes.second) LineTone.Added else null }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Viewer(
    text: CodeText,
    state: CodeViewerState,
    tones: (Int) -> LineTone?,
    slots: CodeViewerSlots,
    marks: List<CodeMark> = emptyList(),
) {
    CodeViewer(text, Modifier.weight(1f).fillMaxHeight(), state, CodeLanguages.named("kotlin"), tones, slots = slots, marks = marks)
}
