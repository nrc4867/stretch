package dev.chieppa.stretch.console

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.chieppa.stretch.ui.TextFind
import kotlinx.coroutines.flow.Flow

/** Which of a process's outputs text came from. */
enum class ConsoleStream { Out, Err }

/** A run of output in one style, from one stream. */
data class ConsoleSpan(val text: String, val style: AnsiStyle, val stream: ConsoleStream)

/** One line of output, without its line break. [id] stays the same while the line is kept, as lines before it go. */
data class ConsoleLine(val id: Long, val spans: List<ConsoleSpan>) {
    val text: String by lazy { spans.joinToString("") { it.text } }
}

/** A piece of output to [ConsoleState.feed]. */
data class ConsoleChunk(val text: String, val stream: ConsoleStream = ConsoleStream.Out)

/** A match of the console's find: on the line at [index] in [ConsoleState.lines], characters [start] up to [end]. */
data class ConsoleMatch(val index: Int, val start: Int, val end: Int)

/**
 * The output a [Console] shows, and how it shows it. Feed it with [append] or [feed], on the UI thread; it parses ANSI
 * colors, handles carriage returns as progress bars use them, and keeps the last [maxLines] lines. [follow] keeps the
 * newest line in view; scrolling up pauses it and scrolling back to the end resumes it.
 */
@Stable
class ConsoleState(val maxLines: Int = DEFAULT_MAX_LINES) : TextFind {
    val lines = mutableStateListOf<ConsoleLine>()

    /** Whether new output scrolls into view. */
    var follow by mutableStateOf(true)

    val list = LazyListState()

    /** Goes up with every change, for views that follow the output. */
    var version by mutableIntStateOf(0)
        private set

    private val parsers = ConsoleStream.entries.associateWith { AnsiParser() }
    private var open = mutableListOf<ConsoleSpan>()
    private var lineOpen = false
    private var returnPending = false
    private var nextId = 0L

    /** Adds [text] from [stream]. A line without its break yet shows and grows as more comes. */
    fun append(text: String, stream: ConsoleStream = ConsoleStream.Out) {
        for (run in parsers.getValue(stream).parse(text)) {
            var rest = run.text
            while (rest.isNotEmpty()) {
                val cut = rest.indexOfFirst { it == '\n' || it == '\r' }
                val piece = if (cut < 0) rest else rest.substring(0, cut)
                if (piece.isNotEmpty()) {
                    if (returnPending) open.clear()
                    returnPending = false
                    open += ConsoleSpan(piece, run.style, stream)
                    showOpen()
                }
                if (cut < 0) break
                when (rest[cut]) {
                    '\n' -> endLine()
                    else -> returnPending = true
                }
                rest = rest.substring(cut + 1)
            }
        }
        version++
    }

    /** Appends every chunk of [chunks] as it comes, until it ends. */
    suspend fun feed(chunks: Flow<ConsoleChunk>) {
        chunks.collect { append(it.text, it.stream) }
    }

    /** Removes all output and starts following again. */
    fun clear() {
        lines.clear()
        open = mutableListOf()
        lineOpen = false
        returnPending = false
        parsers.values.forEach { it.reset() }
        follow = true
        matches = emptyList()
        version++
    }

    /** The whole output as plain text, one line per line. */
    val text: String get() = lines.joinToString("\n") { it.text }

    private fun showOpen() {
        val line = ConsoleLine(if (lineOpen) lines.last().id else nextId++, open.toList())
        if (lineOpen) lines[lines.lastIndex] = line else lines += line
        lineOpen = true
        trim()
    }

    private fun endLine() {
        if (!lineOpen) showOpen()
        open = mutableListOf()
        lineOpen = false
        returnPending = false
    }

    private fun trim() {
        val over = lines.size - maxLines
        if (over > 0) lines.removeRange(0, over)
    }

    override var findOpen by mutableStateOf(false)
    override var query by mutableStateOf("")
    override var matchCase by mutableStateOf(false)

    /** Where [query] occurs in [lines], as the view last found. */
    var matches by mutableStateOf(emptyList<ConsoleMatch>())
        internal set

    override val matchCount: Int get() = matches.size

    override var currentMatch by mutableIntStateOf(-1)
        private set

    internal fun updateMatches(found: List<ConsoleMatch>) {
        matches = found
        currentMatch = if (found.isEmpty()) -1 else currentMatch.coerceIn(0, found.lastIndex)
    }

    override suspend fun nextMatch() = goTo(currentMatch + 1)

    override suspend fun previousMatch() = goTo(currentMatch - 1)

    private suspend fun goTo(index: Int) {
        if (matches.isEmpty()) return
        currentMatch = index.mod(matches.size)
        follow = false
        list.scrollToItem((matches[currentMatch].index - LEAD_IN_LINES).coerceAtLeast(0))
    }

    /** Scrolls to the newest line and follows again. */
    suspend fun scrollToEnd() {
        follow = true
        if (lines.isNotEmpty()) list.scrollToItem(lines.lastIndex)
    }

    companion object {
        const val DEFAULT_MAX_LINES = 100_000

        /** Lines left showing above a match scrolled to. */
        private const val LEAD_IN_LINES = 3
    }
}

@Composable
fun rememberConsoleState(maxLines: Int = ConsoleState.DEFAULT_MAX_LINES): ConsoleState = remember { ConsoleState(maxLines) }

/** Every place [query] occurs in [lines], ignoring case unless [matchCase]. */
fun findInConsole(lines: List<ConsoleLine>, query: String, matchCase: Boolean = false): List<ConsoleMatch> {
    if (query.isEmpty()) return emptyList()
    return buildList {
        lines.forEachIndexed { index, line ->
            var at = line.text.indexOf(query, 0, ignoreCase = !matchCase)
            while (at >= 0) {
                add(ConsoleMatch(index, at, at + query.length))
                at = line.text.indexOf(query, at + query.length, ignoreCase = !matchCase)
            }
        }
    }
}
