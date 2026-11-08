package dev.chieppa.stretch.markdown

import androidx.compose.ui.text.AnnotatedString
import dev.chieppa.stretch.markdown.html.attributesOf
import dev.chieppa.stretch.markdown.html.decodeEntity
import dev.chieppa.stretch.markdown.html.entityPattern
import dev.chieppa.stretch.markdown.html.tagPattern

/** Characters [start] until [end] of a source read as [replacement]. */
private data class Edit(val start: Int, val end: Int, val replacement: String = "")

/** Source characters [start] until [end] styled [style], linking to [url] when it is a link. */
private data class StyleRange(val start: Int, val end: Int, val style: RichStyle, val url: String? = null)

private val imagePattern = Regex("""!\[([^\]]*)]\(\s*<?([^)\s>]+)>?(?:\s+"[^"]*")?\s*\)""")
private val commentPattern = Regex("""<!--[\s\S]*?-->""")
private val bareUrl = Regex("""https?://[^\s<>()]+[^\s<>().,;:!?'"]""")

/** Tags whose text takes a style, and the style. */
private val tagStyles = mapOf(
    "strong" to RichStyle.Bold, "b" to RichStyle.Bold, "em" to RichStyle.Italic, "i" to RichStyle.Italic,
    "code" to RichStyle.Code, "kbd" to RichStyle.Code, "s" to RichStyle.Strike, "del" to RichStyle.Strike,
    "strike" to RichStyle.Strike, "sub" to RichStyle.Sub, "sup" to RichStyle.Sup,
)

/**
 * The text of [source], a block of [kind], as a reader sees it: markers left out, markdown and inline HTML styled,
 * `<br>` breaking the line, character references decoded, bare addresses linked, comments and unknown tags dropped
 * keeping their text, and pictures, from `![alt](url)` or `<img>`, pulled out to show after the text. Code is left
 * as written.
 */
internal fun readOnlyText(source: String, kind: BlockKind, styles: RichStyles): RichText {
    if (kind is BlockKind.Code || kind == BlockKind.Raw) return RichText(AnnotatedString(if (kind is BlockKind.Code) codeBody(source, kind) else source))
    val markup = blockMarkup(source, kind)
    val code = markup.spans.filter { it.style == InlineStyle.Code }.map { it.start until it.end }
    fun inCode(at: Int) = code.any { at in it }
    val edits = ArrayList<Edit>()
    val ranges = ArrayList<StyleRange>()
    val pictures = ArrayList<Picture>()
    markup.hidden.forEach { edits += Edit(it.first, it.last + 1) }
    fun linkAround(at: Int) = markup.spans.lastOrNull { it.style == InlineStyle.Link && at >= it.start - 1 && at <= it.end }?.url
    for (match in imagePattern.findAll(source)) {
        if (inCode(match.range.first)) continue
        edits += Edit(match.range.first, match.range.last + 1)
        val url = match.groupValues[2]
        pictures += Picture(url, match.groupValues[1], link = linkAround(match.range.first)?.takeIf { it != url })
    }
    for (match in commentPattern.findAll(source)) if (!inCode(match.range.first)) edits += Edit(match.range.first, match.range.last + 1)
    htmlEdits(source, ::inCode, edits, ranges, pictures)
    for (match in entityPattern.findAll(source)) {
        if (inCode(match.range.first)) continue
        decodeEntity(match.value)?.let { edits += Edit(match.range.first, match.range.last + 1, it) }
    }
    val linked = markup.spans.filter { it.style == InlineStyle.Link }
    for (match in bareUrl.findAll(source)) {
        val start = match.range.first
        if (inCode(start) || linked.any { start >= it.start && start < it.end } || source.getOrNull(start - 1) in setOf('(', '<', '"', '\'')) continue
        ranges += StyleRange(start, match.range.last + 1, RichStyle.Link, match.value)
    }
    markup.spans.forEach { ranges += StyleRange(it.start, it.end, richStyleOf(it.style), it.url) }
    return RichText(rewrite(source, edits, ranges, styles), pictures)
}

/** Inline HTML tags in [source]: each tag dropped, `<br>` made a line break, `<img>` a picture, styled tags' text styled. */
private fun htmlEdits(source: String, inCode: (Int) -> Boolean, edits: MutableList<Edit>, ranges: MutableList<StyleRange>, pictures: MutableList<Picture>) {
    val open = ArrayList<Pair<String, Pair<Int, Map<String, String>>>>()
    var link: String? = null
    for (match in tagPattern.findAll(source)) {
        val start = match.range.first
        if (inCode(start)) continue
        val end = match.range.last + 1
        val name = match.groupValues[2].lowercase()
        val closing = match.groupValues[1] == "/"
        val attributes = if (closing) emptyMap() else attributesOf(match.groupValues[3])
        when {
            name == "br" -> edits += Edit(start, end, "\n")
            name == "img" -> {
                edits += Edit(start, end)
                attributes["src"]?.takeIf { it.isNotBlank() }?.let { pictures += Picture(it, attributes["alt"].orEmpty(), attributes["width"]?.toIntOrNull(), link) }
            }
            closing -> {
                edits += Edit(start, end)
                val index = open.indexOfLast { it.first == name }
                if (index < 0) continue
                val (from, opened) = open[index].second
                open.subList(index, open.size).clear()
                if (name == "a") {
                    link = null
                    opened["href"]?.let { ranges += StyleRange(from, start, RichStyle.Link, it) }
                } else {
                    tagStyles[name]?.let { ranges += StyleRange(from, start, it) }
                }
            }
            else -> {
                edits += Edit(start, end)
                if (name == "a") link = attributes["href"]
                if (match.groupValues[4] != "/") open += name to (end to attributes)
            }
        }
    }
}

/** [source] with [edits] made and [ranges] styled where their text lands. */
private fun rewrite(source: String, edits: List<Edit>, ranges: List<StyleRange>, styles: RichStyles): AnnotatedString {
    val builder = AnnotatedString.Builder()
    val landed = IntArray(source.length + 1)
    var at = 0
    fun copyTo(end: Int) {
        while (at < end) {
            landed[at] = builder.length
            builder.append(source[at])
            at++
        }
    }
    for (edit in edits.sortedWith(compareBy({ it.start }, { -it.end }))) {
        if (edit.start < at) {
            // Inside one already made: a removal reaching past it removes the rest too.
            if (edit.replacement.isEmpty() && edit.end > at) {
                while (at < edit.end) landed[at++] = builder.length
            }
            continue
        }
        copyTo(edit.start)
        while (at < edit.end) landed[at++] = builder.length
        builder.append(edit.replacement)
    }
    copyTo(source.length)
    landed[source.length] = builder.length
    for (range in ranges) {
        val start = landed[range.start.coerceIn(0, source.length)]
        val end = landed[range.end.coerceIn(0, source.length)]
        if (start >= end) continue
        builder.addStyle(styles(range.style), start, end)
        range.url?.let { builder.addStringAnnotation(URL_ANNOTATION, it, start, end) }
    }
    return builder.toAnnotatedString()
}
