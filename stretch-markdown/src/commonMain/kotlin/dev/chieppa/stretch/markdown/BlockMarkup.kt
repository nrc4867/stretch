package dev.chieppa.stretch.markdown

/** What can be hidden and styled in a text block: its block markers, such as `## `, and all its inline markup. */
internal data class BlockMarkup(val spans: List<InlineSpan>, val markers: List<IntRange>, val blockMarkers: List<IntRange>) {
    /** Every character a person need not see, in order. */
    val hidden: List<IntRange> = mergeRanges(markers + blockMarkers)
}

/** The markup of [source], a block of [kind]. A code block's fence lines are its block markers. */
internal fun blockMarkup(source: String, kind: BlockKind): BlockMarkup = when (kind) {
    is BlockKind.Heading -> if (kind.setext) setextMarkup(source) else {
        val prefixEnd = headingLine.find(source)?.range?.last?.plus(1) ?: 0
        withInline(source, listOf(0 until prefixEnd), prefixEnd)
    }
    is BlockKind.ListItem -> withInline(source, listOf(0 until kind.markerEnd), kind.markerEnd)
    BlockKind.Quote -> quotedMarkup(source, hideFirstLine = false)
    is BlockKind.Callout -> quotedMarkup(source, hideFirstLine = true)
    is BlockKind.Code -> codeMarkup(source, kind)
    BlockKind.Paragraph -> withInline(source, emptyList(), 0)
    BlockKind.Table, is BlockKind.Image, BlockKind.Divider, BlockKind.Raw -> BlockMarkup(emptyList(), emptyList(), emptyList())
}

private fun withInline(source: String, blockMarkers: List<IntRange>, from: Int): BlockMarkup {
    val inline = parseInline(source, from)
    return BlockMarkup(inline.spans, inline.markers, blockMarkers)
}

private fun setextMarkup(source: String): BlockMarkup {
    val lineEnd = source.indexOf('\n')
    val inline = parseInline(source, 0, lineEnd)
    return BlockMarkup(inline.spans, inline.markers, listOf(lineEnd until source.length))
}

/** Each line's `> ` is a block marker, and each line's text is inline markup. */
private fun quotedMarkup(source: String, hideFirstLine: Boolean): BlockMarkup {
    val spans = ArrayList<InlineSpan>()
    val markers = ArrayList<IntRange>()
    val blockMarkers = ArrayList<IntRange>()
    var at = 0
    source.split('\n').forEachIndexed { index, line ->
        val prefix = quoteLine.find(line)?.range?.last?.plus(1) ?: 0
        if (index == 0 && hideFirstLine) {
            val nextLine = at + line.length + 1
            blockMarkers += at until minOf(nextLine, source.length)
        } else {
            blockMarkers += at until at + prefix
            val inline = parseInline(source, at + prefix, at + line.length)
            spans += inline.spans
            markers += inline.markers
        }
        at += line.length + 1
    }
    return BlockMarkup(spans, markers, blockMarkers)
}

private fun codeMarkup(source: String, kind: BlockKind.Code): BlockMarkup {
    val firstEnd = source.indexOf('\n').let { if (it < 0) source.length else it }
    val markers = ArrayList<IntRange>()
    // The opening fence goes with the line break after it, the closing fence with the line break before it.
    markers += 0 until minOf(firstEnd + 1, source.length)
    if (kind.closed) {
        val lastStart = source.lastIndexOf('\n')
        if (lastStart >= firstEnd) markers += lastStart until source.length
    }
    return BlockMarkup(emptyList(), emptyList(), markers)
}

/** The code inside a fenced block, without its fence lines. */
internal fun codeBody(source: String, kind: BlockKind.Code): String {
    val lines = source.split('\n')
    return lines.subList(1, if (kind.closed) lines.lastIndex else lines.size).joinToString("\n")
}

/** Where the code inside a fenced block starts in its source. */
internal fun codeBodyStart(source: String): Int = source.indexOf('\n').let { if (it < 0) source.length else it + 1 }

/** The first caret position past [source]'s leading markers: where its visible text starts. */
internal fun textStart(source: String, kind: BlockKind): Int = when (kind) {
    is BlockKind.Code -> codeBodyStart(source)
    else -> blockMarkup(source, kind).blockMarkers.firstOrNull()?.takeIf { it.first == 0 }?.let { it.last + 1 } ?: 0
}
