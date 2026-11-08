package dev.chieppa.stretch.markdown.html

/** Named character references GitHub comments use; anything else named stays as written. */
private val named = mapOf(
    "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to " ",
    "ndash" to "–", "mdash" to "—", "hellip" to "…", "copy" to "©", "reg" to "®",
    "trade" to "™", "larr" to "←", "rarr" to "→", "uarr" to "↑", "darr" to "↓",
    "lsquo" to "‘", "rsquo" to "’", "ldquo" to "“", "rdquo" to "”", "bull" to "•",
    "middot" to "·", "times" to "×", "check" to "✓", "zwj" to "‍", "zwnj" to "‌",
)

/** A character reference: `&amp;`, `&#64;` or `&#x40;`. */
internal val entityPattern = Regex("""&(#[0-9]{1,7}|#[xX][0-9a-fA-F]{1,6}|[a-zA-Z][a-zA-Z0-9]{1,31});""")

/** The text [reference] (one [entityPattern] match, `&` to `;`) stands for, or null for one it does not know. */
internal fun decodeEntity(reference: String): String? {
    val body = reference.substring(1, reference.length - 1)
    if (!body.startsWith("#")) return named[body]
    val code = if (body.length > 1 && (body[1] == 'x' || body[1] == 'X')) body.substring(2).toIntOrNull(HEX) else body.substring(1).toIntOrNull()
    if (code == null || code <= 0 || code > MAX_CODE_POINT) return null
    return codePointToString(code)
}

/** [text] with every character reference it knows replaced by what it stands for. */
internal fun decodeEntities(text: String): String =
    if ('&' !in text) text else entityPattern.replace(text) { decodeEntity(it.value) ?: it.value }

private fun codePointToString(code: Int): String {
    if (code < SUPPLEMENTARY_START) return code.toChar().toString()
    val shifted = code - SUPPLEMENTARY_START
    val high = (HIGH_SURROGATE_START + (shifted shr SURROGATE_BITS)).toChar()
    val low = (LOW_SURROGATE_START + (shifted and SURROGATE_MASK)).toChar()
    return "$high$low"
}

private const val HEX = 16
private const val MAX_CODE_POINT = 0x10FFFF
private const val SUPPLEMENTARY_START = 0x10000
private const val HIGH_SURROGATE_START = 0xD800
private const val LOW_SURROGATE_START = 0xDC00
private const val SURROGATE_BITS = 10
private const val SURROGATE_MASK = 0x3FF
