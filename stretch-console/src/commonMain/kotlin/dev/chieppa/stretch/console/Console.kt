package dev.chieppa.stretch.console

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchSpacing
import dev.chieppa.stretch.ui.Button
import dev.chieppa.stretch.ui.FindBar
import dev.chieppa.stretch.ui.FindStrings
import dev.chieppa.stretch.ui.Scrollbar
import kotlinx.coroutines.launch

/** Text a [Console] shows. Apps pass their own for other languages. */
data class ConsoleStrings(
    val find: FindStrings = FindStrings(),
    val jumpToEnd: String = "Jump to the end",
)

/**
 * The output of a process or anything else that prints, kept in [state]: colored as its ANSI codes say, standard error
 * in the theme's error ink, and built only for the lines on screen. It follows new output until someone scrolls up,
 * and shows a button back to the end meanwhile. Text selects across lines. [links] finds what to make clickable in
 * each line, `path:line` by default, and clicks go to [onLinkClick]; without it nothing is a link. Ctrl F (Cmd F on
 * macOS) finds, F3 and Shift F3 move between matches.
 */
@Composable
fun Console(
    state: ConsoleState,
    modifier: Modifier = Modifier,
    links: ConsoleLinkDetector = PathLineLinks,
    onLinkClick: ((ConsoleLink) -> Unit)? = null,
    strings: ConsoleStrings = ConsoleStrings(),
) {
    val palette = rememberConsolePalette()
    val style = Stretch.type.valueSm
    val scope = rememberCoroutineScope()
    val findFocus = remember { FocusRequester() }
    var opened by remember { mutableIntStateOf(0) }
    val click by rememberUpdatedState(onLinkClick)
    val onLink: ((ConsoleLink) -> Unit)? = if (onLinkClick != null) { link -> click?.invoke(link) } else null

    LaunchedEffect(state) {
        snapshotFlow { state.version }.collect {
            if (state.follow && state.lines.isNotEmpty()) state.list.scrollToItem(state.lines.lastIndex)
        }
    }
    LaunchedEffect(state, state.query, state.matchCase, if (state.query.isEmpty()) 0 else state.version) {
        state.updateMatches(findInConsole(state.lines.toList(), state.query, state.matchCase))
    }
    val matchesByLine = remember(state.matches) { state.matches.groupBy { it.index } }
    val current = state.matches.getOrNull(state.currentMatch)
    val following = remember(state) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Content moving down shows earlier lines: someone is reading back.
                if (source == NestedScrollSource.UserInput && available.y > 0f) state.follow = false
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && !state.list.canScrollForward) state.follow = true
                return Offset.Zero
            }
        }
    }

    Column(
        modifier.background(palette.background).onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            when {
                event.key == Key.F && (event.isCtrlPressed || event.isMetaPressed) -> {
                    state.findOpen = true
                    opened++
                }
                event.key == Key.F3 -> scope.launch { if (event.isShiftPressed) state.previousMatch() else state.nextMatch() }
                event.key == Key.Escape && state.findOpen -> state.findOpen = false
                else -> return@onPreviewKeyEvent false
            }
            true
        },
    ) {
        if (state.findOpen) FindBar(state, strings.find, findFocus, opened)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            SelectionContainer {
                LazyColumn(Modifier.fillMaxSize().nestedScroll(following), state.list) {
                    itemsIndexed(state.lines, key = { _, line -> line.id }) { index, line ->
                        val found = matchesByLine[index].orEmpty()
                        val text = remember(line, palette, found, current, onLink != null) {
                            consoleLineText(line, palette, if (onLink != null) links.find(line.text) else emptyList(), onLink, found, current)
                        }
                        BasicText(
                            text,
                            Modifier.fillMaxWidth().heightIn(min = StretchSpacing.space16).padding(horizontal = StretchSpacing.space8),
                            style = style,
                        )
                    }
                }
            }
            Scrollbar(state.list, Modifier.align(Alignment.CenterEnd))
            if (!state.follow) {
                Button(strings.jumpToEnd, { scope.launch { state.scrollToEnd() } }, Modifier.align(Alignment.BottomEnd).padding(StretchSpacing.space12))
            }
        }
    }
}
