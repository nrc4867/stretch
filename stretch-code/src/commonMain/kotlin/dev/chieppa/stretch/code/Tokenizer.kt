package dev.chieppa.stretch.code

enum class TokenKind { Keyword, String, Number, Comment, Type, Function }

/** The characters from [start] up to [end] are a [kind]. Text between tokens is plain. */
data class CodeToken(val start: Int, val end: Int, val kind: TokenKind)

/** Tokens of [text] in order, none overlapping. Handles comments and strings that cross lines. Null [language] gives none. */
fun tokenize(text: String, language: CodeLanguage?): List<CodeToken> =
    if (language == null) emptyList() else Tokenizer(text, language).run()

/**
 * The tokens of each of [lines], read as one contiguous text so a comment opened on one line still colours the next.
 * Offsets are relative to the start of their line.
 */
fun tokenizeLines(lines: List<String>, language: CodeLanguage?): List<List<CodeToken>> {
    if (language == null) return lines.map { emptyList() }
    val tokens = tokenize(lines.joinToString("\n"), language)
    var next = 0
    var lineStart = 0
    return lines.map { line ->
        val lineEnd = lineStart + line.length
        val here = buildList {
            while (next < tokens.size && tokens[next].start < lineEnd) {
                val token = tokens[next]
                if (token.end > lineStart) add(CodeToken(maxOf(token.start, lineStart) - lineStart, minOf(token.end, lineEnd) - lineStart, token.kind))
                if (token.end > lineEnd) break
                next++
            }
        }
        lineStart = lineEnd + 1
        here
    }
}

private class Tokenizer(private val text: String, private val language: CodeLanguage) {
    private val tokens = ArrayList<CodeToken>()
    private var at = 0
    private var inTag = false

    private val keywords = if (language.ignoreCase) language.keywords.mapTo(HashSet()) { it.lowercase() } else language.keywords
    private val constants = if (language.ignoreCase) language.constants.mapTo(HashSet()) { it.lowercase() } else language.constants

    fun run(): List<CodeToken> {
        while (at < text.length) {
            if (comment() || string() || (language.tags && tag()) || number() || word()) continue
            at++
        }
        return tokens
    }

    private fun emit(end: Int, kind: TokenKind) {
        tokens += CodeToken(at, end, kind)
        at = end
    }

    private fun comment(): Boolean {
        language.lineComments.firstOrNull { text.startsWith(it, at) }?.let { _ ->
            val end = text.indexOf('\n', at).let { if (it < 0) text.length else it }
            emit(end, TokenKind.Comment)
            return true
        }
        language.blockComments.firstOrNull { text.startsWith(it.first, at) }?.let { (open, close) ->
            val found = text.indexOf(close, at + open.length)
            emit(if (found < 0) text.length else found + close.length, TokenKind.Comment)
            return true
        }
        return false
    }

    private fun string(): Boolean {
        // Prose in markup holds apostrophes, so quotes only open strings inside a tag.
        if (language.tags && !inTag) return false
        language.multilineStrings.firstOrNull { text.startsWith(it, at) }?.let { delimiter ->
            emit(closingDelimiter(delimiter, multiline = true), TokenKind.String)
            return true
        }
        language.strings.firstOrNull { text.startsWith(it, at) }?.let { delimiter ->
            // An apostrophe after a word is a lifetime or a contraction, not a string.
            if (delimiter == "'" && at > 0 && text[at - 1].isLetterOrDigit()) return false
            emit(closingDelimiter(delimiter, multiline = false), TokenKind.String)
            return true
        }
        return false
    }

    private fun closingDelimiter(delimiter: String, multiline: Boolean): Int {
        var i = at + delimiter.length
        while (i < text.length) {
            val char = text[i]
            if (char == '\\') {
                i += 2
                continue
            }
            if (char == '\n' && !multiline) return i
            if (text.startsWith(delimiter, i)) return i + delimiter.length
            i++
        }
        return text.length
    }

    private fun tag(): Boolean {
        val char = text[at]
        if (char == '>' && inTag) {
            inTag = false
            at++
            return true
        }
        if (char != '<') return false
        val nameStart = if (text.getOrNull(at + 1) == '/') at + 2 else at + 1
        if (text.getOrNull(nameStart)?.isLetter() != true) return false
        var end = nameStart
        while (end < text.length && (text[end].isLetterOrDigit() || text[end] in "-:_.")) end++
        at = nameStart
        emit(end, TokenKind.Keyword)
        inTag = true
        return true
    }

    private fun number(): Boolean {
        if (!text[at].isDigit() || text.getOrNull(at - 1)?.isWordPart() == true) return false
        var end = at
        while (end < text.length && (text[end].isLetterOrDigit() || text[end] == '_' || (text[end] == '.' && text.getOrNull(end + 1)?.isDigit() == true))) end++
        emit(end, TokenKind.Number)
        return true
    }

    private fun word(): Boolean {
        val char = text[at]
        if (!(char.isLetter() || char == '_' || char == '$')) return false
        var end = at
        while (end < text.length && text[end].isWordPart()) end++
        val word = text.substring(at, end)
        val lookup = if (language.ignoreCase) word.lowercase() else word
        val kind = when {
            language.tags -> if (inTag) TokenKind.Function else null
            lookup in keywords -> TokenKind.Keyword
            lookup in constants -> TokenKind.Number
            language.keyed && nextNonSpace(end).let { it != null && it in KEY_ENDS } -> TokenKind.Function
            nextNonSpace(end) == '(' && !language.keyed -> TokenKind.Function
            word[0].isUpperCase() && !language.keyed -> TokenKind.Type
            else -> null
        }
        if (kind != null) emit(end, kind) else at = end
        return true
    }

    private fun nextNonSpace(from: Int): Char? {
        var i = from
        while (i < text.length && text[i] == ' ') i++
        return text.getOrNull(i)
    }

    private fun Char.isWordPart() = isLetterOrDigit() || this == '_' || this == '$'

    private companion object {
        const val KEY_ENDS = ":="
    }
}
