package dev.chieppa.stretch.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.diff.DiffViewer
import dev.chieppa.stretch.diff.diffTexts
import dev.chieppa.stretch.diff.findInDiff
import dev.chieppa.stretch.diff.parseUnifiedDiff
import dev.chieppa.stretch.diff.rememberDiffViewerState
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.ui.IconButton
import dev.chieppa.stretch.ui.Label
import dev.chieppa.stretch.ui.TextField
import kotlinx.coroutines.launch

/** A patch and a two-text diff side by side, with notes over picked lines and an app's own find drawn in place. */
@Composable
internal fun DiffDemo() {
    val files = remember {
        parseUnifiedDiff(SampleDocuments.patch) + diffTexts(SampleDocuments.oldCode, SampleDocuments.newCode, "src/Greeter.kt")
    }
    val state = rememberDiffViewerState(split = true)
    var query by remember { mutableStateOf("") }
    var current by remember { mutableIntStateOf(0) }
    val matches = remember(files, query) { findInDiff(files, query) }
    val scope = rememberCoroutineScope()
    fun move(step: Int) {
        if (matches.isEmpty()) return
        current = (current + step).mod(matches.size)
        scope.launch { state.scrollToMatch(matches[current]) }
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextField(query, { query = it; current = 0 }, Modifier.width(220.dp), placeholder = "Find in the diff", icon = StretchIcons.Search)
            IconButton(StretchIcons.ArrowUp, "Previous match", { move(-1) })
            IconButton(StretchIcons.ArrowDown, "Next match", { move(1) })
            Label(if (matches.isEmpty()) "" else "${current + 1} of ${matches.size}", Stretch.type.bodySm, Stretch.colors.inkMuted)
        }
        DiffViewer(
            files, Modifier.weight(1f).fillMaxWidth(), state, slots = rememberDiffNotes(),
            matches = matches, currentMatch = matches.getOrNull(current),
        )
    }
}
