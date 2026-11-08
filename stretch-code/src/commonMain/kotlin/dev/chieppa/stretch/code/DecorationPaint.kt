package dev.chieppa.stretch.code

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** The shape of a [CodeUnderline]. */
enum class UnderlineShape { Wavy, Dotted }

/** An underline from [start] until [end], offsets in the text as shown once tabs expand, in [color]. */
@Immutable
data class CodeUnderline(val start: Int, val end: Int, val color: Color, val shape: UnderlineShape)

/** What a line's decorations draw: [washes] behind runs of text and [underlines] under them, none overlapping another of its kind. */
@Immutable
class DecorationPaint(val washes: List<CodeHighlight>, val underlines: List<CodeUnderline>) {
    companion object {
        val None = DecorationPaint(emptyList(), emptyList())
    }
}

/** A run from [start] until [end] holding the strongest [rank] over it. */
internal data class RankedRun(val start: Int, val end: Int, val rank: Int)

/**
 * [runs] over a text [length] characters long, cut so that no two overlap and each character keeps the highest rank of
 * the runs over it. Characters under no run are left out, and touching pieces of the same rank join.
 */
internal fun strongestRuns(length: Int, runs: List<RankedRun>): List<RankedRun> {
    if (runs.isEmpty() || length <= 0) return emptyList()
    val ranks = IntArray(length) { -1 }
    for (run in runs) {
        for (at in run.start.coerceIn(0, length) until run.end.coerceIn(0, length)) ranks[at] = maxOf(ranks[at], run.rank)
    }
    return buildList {
        var at = 0
        while (at < length) {
            val rank = ranks[at]
            var end = at + 1
            while (end < length && ranks[end] == rank) end++
            if (rank >= 0) add(RankedRun(at, end, rank))
            at = end
        }
    }
}

/** Which layer a style draws on and how strong it is there; a higher rank wins over a lower one on its layer. */
private fun CodeDecorationStyle.isWash() = this == CodeDecorationStyle.Reference || this == CodeDecorationStyle.Definition

private fun CodeDecorationStyle.rank(): Int = when (this) {
    CodeDecorationStyle.Reference -> 0
    CodeDecorationStyle.Definition -> 1
    CodeDecorationStyle.Accent -> 0
    CodeDecorationStyle.Warning -> 1
    CodeDecorationStyle.Error -> 2
}

private val washByRank = listOf(CodeDecorationStyle.Reference, CodeDecorationStyle.Definition)
private val underlineByRank = listOf(CodeDecorationStyle.Accent, CodeDecorationStyle.Warning, CodeDecorationStyle.Error)

/**
 * How [decorations] on [line] draw in [look]: on each layer, each character takes the strongest style over it, as
 * [CodeDecoration] describes. Their ranges' lines are not checked; pass only those on [line].
 */
fun decorationPaint(line: String, decorations: List<CodeDecoration>, look: CodeDecorationLook): DecorationPaint {
    if (decorations.isEmpty()) return DecorationPaint.None
    val (washes, underlines) = decorations.partition { it.style.isWash() }
    val offsets = tabOffsets(line)
    fun ranked(of: List<CodeDecoration>) = strongestRuns(line.length, of.map { RankedRun(it.range.start, it.range.end, it.style.rank()) })
    return DecorationPaint(
        washes = ranked(washes).map { CodeHighlight(it.start, it.end, look.of(washByRank[it.rank])) },
        underlines = ranked(underlines).map {
            val style = underlineByRank[it.rank]
            val shape = if (style == CodeDecorationStyle.Accent) UnderlineShape.Dotted else UnderlineShape.Wavy
            CodeUnderline(offsets[it.start], offsets[it.end], look.of(style), shape)
        },
    )
}

/** Styles from most to least severe, the order tooltips that meet show in. */
private val bySeverity = listOf(
    CodeDecorationStyle.Error,
    CodeDecorationStyle.Warning,
    CodeDecorationStyle.Definition,
    CodeDecorationStyle.Reference,
    CodeDecorationStyle.Accent,
)

/** A tooltip to show over [range]. */
data class DecorationTip(val text: String, val range: CodeRange)

/**
 * The tooltip for the character at [column] among [decorations] on one line: the text of each decoration over it with
 * a tooltip, the most severe first and each text once, one per line, over the range of the most severe. Null when none
 * over it has a tooltip.
 */
fun decorationTip(decorations: List<CodeDecoration>, column: Int): DecorationTip? {
    val over = decorations.filter { it.tooltip != null && column >= it.range.start && column < it.range.end }
    if (over.isEmpty()) return null
    val ordered = over.sortedBy { bySeverity.indexOf(it.style) }
    return DecorationTip(ordered.mapNotNull { it.tooltip }.distinct().joinToString("\n"), ordered.first().range)
}
