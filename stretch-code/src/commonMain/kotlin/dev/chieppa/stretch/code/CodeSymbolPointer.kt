package dev.chieppa.stretch.code

import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import kotlinx.coroutines.flow.collectLatest

/** A character of a code view: [line] counts from 1 and [column] is its offset before tabs expand, among the lines of [group]. */
data class CodeSpot<G>(val group: G, val line: Int, val column: Int)

/** Line [line], counting from 1, among the lines of [group]: one whole text, or one side of one file. */
data class CodeLineKey<G>(val group: G, val line: Int)

/** [range], on a line of [group]: the symbol under the pointer that a Ctrl or Cmd click opens. */
data class CodeLink<G>(val group: G, val range: CodeRange) {
    operator fun contains(spot: CodeSpot<G>): Boolean =
        spot.group == group && spot.line == range.line && spot.column >= range.start && spot.column < range.end
}

/** Frames a key press waits, at most, for a line it scrolled to to be laid out before reporting it. */
private const val REVEAL_FRAMES = 3

/**
 * What a code view knows of the pointer and caret over its code, for clicking through to symbols. Shared by the views
 * that draw code, where [G] tells apart groups of lines that number on their own, such as each side of each file of a
 * diff. While Ctrl or Cmd is held, the view asks [symbolAt] what is under the pointer and [link] holds the answer, which
 * draws underlined under a pointing hand; a Ctrl or Cmd click calls [onClick] with it. A view sets its callbacks and
 * coordinates as it composes and runs [followPointer] while it shows.
 */
@Stable
class CodeSymbolPointer<G> {
    /** Ctrl or Cmd is down, as the last key or pointer event the view saw said. */
    var held by mutableStateOf(false)

    /** The character under the pointer, or null while it is over no character. */
    var pointer by mutableStateOf<CodeSpot<G>?>(null)

    /** The symbol under the pointer while Ctrl or Cmd is held, once [symbolAt] finds it. */
    var link by mutableStateOf<CodeLink<G>?>(null)
        private set

    /** Where navigation by key starts: where the last plain press in the code landed, or where the app put it. */
    var caret by mutableStateOf<CodeSpot<G>?>(null)

    /** Asks the app for the symbol at a character, on that character's line, or null for none. */
    var symbolAt: (suspend (CodeSpot<G>) -> CodeRange?)? = null

    /** Called for a click through to a symbol, with where it started, the symbol and its bounds in [outer]'s pixels. */
    var onClick: ((CodeSpot<G>, CodeRange, Rect?) -> Unit)? = null

    /** The whole view, as its app lays it out; clicks report bounds in its pixels. */
    var outer: LayoutCoordinates? = null

    /** The view's content, where its own popups are placed. */
    var inner: LayoutCoordinates? = null

    /** The view's frame clock, to wait for a line scrolled to to be laid out. */
    var frameClock: MonotonicFrameClock? = null

    private val lines = HashMap<CodeLineKey<G>, CodeLineGeometry>()

    /** Clicking through to symbols is on. */
    val enabled: Boolean get() = symbolAt != null && onClick != null

    /** Records where the line known by each of [keys] is laid out, until [untrack] is called with the same. */
    fun track(keys: List<CodeLineKey<G>>, geometry: CodeLineGeometry) {
        for (key in keys) lines[key] = geometry
    }

    fun untrack(keys: List<CodeLineKey<G>>, geometry: CodeLineGeometry) {
        for (key in keys) if (lines[key] === geometry) lines.remove(key)
    }

    /** The bounds of [range] on a line of [group] in [root]'s pixels, or null while it is not laid out. */
    fun boundsOf(group: G, range: CodeRange, root: LayoutCoordinates?): Rect? {
        root ?: return null
        return lines[CodeLineKey(group, range.line)]?.bounds(range.start, range.end, root)
    }

    /**
     * Keeps [link] on the symbol under the pointer while Ctrl or Cmd is held, until cancelled. An answer that is slow to
     * come leaves the last symbol underlined until it does, and moving within an underlined symbol asks nothing, so the
     * underline never flickers; an answer for a place the pointer has left is dropped.
     */
    suspend fun followPointer() {
        snapshotFlow { if (held) pointer else null }.collectLatest { spot ->
            if (spot == null) {
                link = null
                return@collectLatest
            }
            if (link?.let { spot in it } == true) return@collectLatest
            link = lookup(spot)?.let { CodeLink(spot.group, it) }
        }
    }

    /** Clicks through from [spot]: the symbol underlined there, or else the one [symbolAt] finds. Whether there was one. */
    suspend fun click(spot: CodeSpot<G>): Boolean {
        val range = link?.takeIf { spot in it }?.range ?: lookup(spot) ?: return false
        val click = onClick ?: return false
        click(spot, range, boundsOf(spot.group, range, outer))
        return true
    }

    /**
     * Clicks through from [caret], as a Ctrl or Cmd click there would, after [reveal] scrolls the symbol into view.
     * Whether there was a symbol there.
     */
    suspend fun clickAtCaret(reveal: suspend (CodeSpot<G>, CodeRange) -> Unit): Boolean {
        val spot = caret ?: return false
        val range = lookup(spot) ?: return false
        val click = onClick ?: return false
        reveal(spot, range)
        var bounds: Rect? = null
        val clock = frameClock
        repeat(REVEAL_FRAMES) {
            if (bounds != null || clock == null) return@repeat
            clock.withFrameNanos { }
            bounds = boundsOf(spot.group, range, outer)
        }
        click(spot, range, bounds ?: boundsOf(spot.group, range, outer))
        return true
    }

    /** The app's answer for [spot], kept only when it is a run of characters on [spot]'s own line. */
    private suspend fun lookup(spot: CodeSpot<G>): CodeRange? {
        val ask = symbolAt ?: return null
        return ask(spot)?.takeIf { it.line == spot.line && it.end > it.start }
    }
}
