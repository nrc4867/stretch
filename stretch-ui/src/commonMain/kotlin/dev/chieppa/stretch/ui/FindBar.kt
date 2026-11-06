package dev.chieppa.stretch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.theme.StretchSizes
import dev.chieppa.stretch.theme.StretchSpacing
import kotlinx.coroutines.launch

/** Finding text in a view: what to find, how many matches there are, which is current and moving between them. */
interface TextFind {
    var findOpen: Boolean
    var query: String
    var matchCase: Boolean

    /** How many matches there are. */
    val matchCount: Int

    /** Which match is current, counting from 0, or −1 when there are none. */
    val currentMatch: Int

    suspend fun nextMatch()

    suspend fun previousMatch()
}

/** Text a [FindBar] shows. Apps pass their own for other languages. */
data class FindStrings(
    val find: String = "Find",
    val previousMatch: String = "Previous match",
    val nextMatch: String = "Next match",
    val matchCase: String = "Match case",
    val closeFind: String = "Close find",
    val noMatches: String = "No matches",
    val matchCount: (current: Int, total: Int) -> String = { current, total -> "$current of $total" },
)

/** The narrowest the find field gets. */
private val FindFieldWidth = 220.dp

/**
 * A bar for finding text in a view: the query, how many matches there are and which is current, moving between them
 * and closing. Enter goes to the next match, Shift Enter the previous, Esc closes. [focus] puts the caret in the field
 * each time [opened] changes.
 */
@Composable
fun FindBar(find: TextFind, strings: FindStrings, focus: FocusRequester, opened: Int, modifier: Modifier = Modifier) {
    val c = Stretch.colors
    val scope = rememberCoroutineScope()
    LaunchedEffect(opened) { focus.requestFocus() }
    Row(
        modifier.fillMaxWidth().background(c.surfaceRail).padding(horizontal = StretchSpacing.space12, vertical = StretchSpacing.space6),
        horizontalArrangement = Arrangement.spacedBy(StretchSpacing.space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            find.query,
            { find.query = it },
            Modifier
                .widthIn(min = FindFieldWidth)
                .focusRequester(focus)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.Enter, Key.NumPadEnter -> scope.launch { if (event.isShiftPressed) find.previousMatch() else find.nextMatch() }
                        Key.Escape -> find.findOpen = false
                        else -> return@onPreviewKeyEvent false
                    }
                    true
                },
            placeholder = strings.find,
            icon = StretchIcons.Search,
        )
        val none = find.query.isNotEmpty() && find.matchCount == 0
        val count = when {
            find.query.isEmpty() -> ""
            none -> strings.noMatches
            else -> strings.matchCount(find.currentMatch + 1, find.matchCount)
        }
        Label(count, Stretch.type.bodySm, if (none) c.errorInk else c.inkMuted, Modifier.weight(1f))
        Checkbox(find.matchCase, { find.matchCase = it }, strings.matchCase)
        IconButton(StretchIcons.ArrowUp, strings.previousMatch, { scope.launch { find.previousMatch() } }, size = StretchSizes.seg)
        IconButton(StretchIcons.ArrowDown, strings.nextMatch, { scope.launch { find.nextMatch() } }, size = StretchSizes.seg)
        IconButton(StretchIcons.Close, strings.closeFind, { find.findOpen = false }, size = StretchSizes.seg)
    }
    Box(Modifier.fillMaxWidth().height(Stretch.stroke).background(c.linePanel))
}
