package dev.chieppa.stretch.code

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.chieppa.stretch.ui.FindStrings
import dev.chieppa.stretch.ui.TextFind

/**
 * How a [CodeViewer] is laid out, where it has scrolled and what it is finding. Changing a property updates the viewer.
 * Each line is one item of [list], at index line − 1, so two viewers can follow each other by line through [list] and
 * sideways through [horizontal].
 */
@Stable
class CodeViewerState(wrap: Boolean = false) : TextFind {
    /** Long lines wrap, rather than scroll sideways. */
    var wrap by mutableStateOf(wrap)

    val list = LazyListState()

    /** The sideways scroll every line shares while lines do not wrap. */
    val horizontal = ScrollState(0)

    override var findOpen by mutableStateOf(false)

    override var query by mutableStateOf("")

    override var matchCase by mutableStateOf(false)

    /** Where [query] occurs, top to bottom. */
    var matches by mutableStateOf(emptyList<CodeRange>())
        internal set

    override val matchCount: Int get() = matches.size

    /** Which of [matches] is current, or −1 when there are none. */
    override var currentMatch by mutableIntStateOf(-1)
        private set

    /** One character's width in pixels, to scroll sideways to a column. */
    internal var charWidth = 0f

    /** The lines showing, to find a match's column once tabs expand. */
    internal var lines: List<String> = emptyList()

    /** The first line showing at the top, counting from 1. */
    val firstVisibleLine: Int get() = list.firstVisibleItemIndex + 1

    /** Scrolls so line [number] is near the top, with a few lines above it to read it in context. */
    suspend fun scrollToLine(number: Int, leadIn: Int = LEAD_IN_LINES) {
        list.scrollToItem((number - 1 - leadIn).coerceAtLeast(0))
    }

    /** Puts line [number] at the very top, as another viewer following this one does. */
    suspend fun alignLine(number: Int, offset: Int = 0) {
        list.scrollToItem((number - 1).coerceAtLeast(0), offset)
    }

    internal fun updateMatches(found: List<CodeRange>) {
        matches = found
        currentMatch = if (found.isEmpty()) -1 else currentMatch.coerceIn(0, found.lastIndex)
    }

    /** Moves to the next match, wrapping around, and scrolls to it. */
    override suspend fun nextMatch() = goToMatch(currentMatch + 1)

    /** Moves to the previous match, wrapping around, and scrolls to it. */
    override suspend fun previousMatch() = goToMatch(currentMatch - 1)

    private suspend fun goToMatch(index: Int) {
        if (matches.isEmpty()) return
        currentMatch = index.mod(matches.size)
        scrollToMatch(matches[currentMatch])
    }

    /**
     * Scrolls so [match] shows: to its line, with lines above it, unless it already shows away from the edges, and
     * sideways to it while lines do not wrap. For matches the find bar finds and for those an app passes in.
     */
    suspend fun scrollToMatch(match: CodeRange) {
        val visible = list.layoutInfo.visibleItemsInfo
        val shown = visible.size > 2 && match.line - 1 in visible[1].index..visible[visible.size - 2].index
        if (!shown) scrollToLine(match.line, (visible.size / 2).coerceAtLeast(LEAD_IN_LINES))
        if (!wrap) {
            val line = lines.getOrNull(match.line - 1).orEmpty()
            horizontal.revealColumn(tabOffsets(line)[match.start.coerceIn(0, line.length)], charWidth)
        }
    }

    private companion object {
        /** Lines left showing above a line scrolled to. */
        const val LEAD_IN_LINES = 3
    }
}

@Composable
fun rememberCodeViewerState(wrap: Boolean = false): CodeViewerState = remember { CodeViewerState(wrap) }

/** Text a [CodeViewer] shows. Apps pass their own for other languages. */
data class CodeViewerStrings(val find: FindStrings = FindStrings())
