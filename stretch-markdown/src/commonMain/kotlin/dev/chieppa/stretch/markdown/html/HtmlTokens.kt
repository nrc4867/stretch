package dev.chieppa.stretch.markdown.html

/** A piece of HTML source: a tag opening or closing, or the text between tags with its references decoded. */
internal sealed interface HtmlToken {
    /** `<name attrs>`; [selfClosing] for `<br/>` and for tags that never hold anything, such as `img`. */
    data class Open(val name: String, val attributes: Map<String, String>, val selfClosing: Boolean) : HtmlToken

    data class Close(val name: String) : HtmlToken

    data class Text(val text: String) : HtmlToken
}

/** Tags that hold nothing and need no closing tag. */
internal val voidTags = setOf("br", "hr", "img", "input", "wbr", "meta", "link", "source", "col", "area")

internal val tagPattern = Regex("""<(/?)([a-zA-Z][a-zA-Z0-9-]*)((?:\s+[^\s"'>/=]+(?:\s*=\s*(?:"[^"]*"|'[^']*'|[^\s"'=<>`]+))?)*)\s*(/?)>""")
private val attributePattern = Regex("""([^\s"'>/=]+)(?:\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s"'=<>`]+)))?""")
private const val COMMENT_OPEN = "<!--"
private const val COMMENT_CLOSE = "-->"

/**
 * Splits [source] into tags and text. Comments, `<!DOCTYPE>` and `<?...?>` are left out; a `<` that starts no tag is
 * text. Names come lowercased, and text and attribute values have their character references decoded.
 */
internal fun htmlTokens(source: String): List<HtmlToken> {
    val tokens = ArrayList<HtmlToken>()
    val text = StringBuilder()
    fun flush() {
        if (text.isNotEmpty()) tokens += HtmlToken.Text(decodeEntities(text.toString()))
        text.clear()
    }
    var at = 0
    while (at < source.length) {
        val char = source[at]
        if (char != '<') {
            text.append(char)
            at++
            continue
        }
        val skipTo = skippedEnd(source, at)
        if (skipTo != null) {
            at = skipTo
            continue
        }
        val match = tagPattern.matchAt(source, at)
        if (match == null) {
            text.append(char)
            at++
            continue
        }
        flush()
        val name = match.groupValues[2].lowercase()
        tokens += if (match.groupValues[1] == "/") {
            HtmlToken.Close(name)
        } else {
            HtmlToken.Open(name, attributesOf(match.groupValues[3]), match.groupValues[4] == "/" || name in voidTags)
        }
        at = match.range.last + 1
    }
    flush()
    return tokens
}

/** Where a comment, declaration or processing instruction starting at [at] ends, or null when none starts there. */
private fun skippedEnd(source: String, at: Int): Int? {
    if (source.startsWith(COMMENT_OPEN, at)) {
        val close = source.indexOf(COMMENT_CLOSE, at + COMMENT_OPEN.length)
        return if (close < 0) source.length else close + COMMENT_CLOSE.length
    }
    val next = source.getOrNull(at + 1)
    if (next == '!' || next == '?') {
        val close = source.indexOf('>', at)
        return if (close < 0) source.length else close + 1
    }
    return null
}

internal fun attributesOf(text: String): Map<String, String> = buildMap {
    for (match in attributePattern.findAll(text)) {
        val (name, double, single, bare) = match.destructured
        put(name.lowercase(), decodeEntities(double.ifEmpty { single.ifEmpty { bare } }))
    }
}
