package dev.chieppa.stretch.console

/** A color a program asks for: one of the 256 numbered ones (the first 16 follow the theme), or an exact one. */
sealed interface AnsiColor {
    data class Indexed(val index: Int) : AnsiColor

    data class Rgb(val red: Int, val green: Int, val blue: Int) : AnsiColor
}

/** How a run of output looks, as SGR escape codes set it. Null colors are the console's own. */
data class AnsiStyle(
    val foreground: AnsiColor? = null,
    val background: AnsiColor? = null,
    val bold: Boolean = false,
    val dim: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val inverse: Boolean = false,
    val strikethrough: Boolean = false,
) {
    companion object {
        val Plain = AnsiStyle()
    }
}

/** A run of text in one style. It holds no escape codes; it can hold line breaks and carriage returns. */
data class AnsiRun(val text: String, val style: AnsiStyle)

private const val ESC = '\u001B'
private const val BEL = '\u0007'
private const val SGR_EXTENDED_FOREGROUND = 38
private const val SGR_EXTENDED_BACKGROUND = 48
private const val EXTENDED_INDEXED = 5
private const val EXTENDED_RGB = 2

/**
 * Splits output into styled runs, following SGR codes (`ESC [ … m`) and dropping every other escape sequence, such as
 * cursor movement and window titles. It keeps its style between chunks, and holds back an escape sequence a chunk cuts
 * off until the next chunk completes it.
 */
class AnsiParser {
    var style: AnsiStyle = AnsiStyle.Plain
        private set
    private var held = ""

    fun reset() {
        style = AnsiStyle.Plain
        held = ""
    }

    fun parse(chunk: String): List<AnsiRun> {
        val text = held + chunk
        held = ""
        val runs = mutableListOf<AnsiRun>()
        val plain = StringBuilder()
        fun flush() {
            if (plain.isNotEmpty()) runs += AnsiRun(plain.toString(), style)
            plain.clear()
        }
        var at = 0
        while (at < text.length) {
            val char = text[at]
            if (char != ESC) {
                plain.append(char)
                at++
                continue
            }
            val end = sequenceEnd(text, at)
            if (end == null) {
                held = text.substring(at)
                break
            }
            if (text.getOrNull(at + 1) == '[' && text[end - 1] == 'm') {
                flush()
                style = applySgr(style, text.substring(at + 2, end - 1))
            }
            at = end
        }
        flush()
        return runs
    }

    /** Where the escape sequence starting at [start] ends, exclusive, or null when [text] stops before it does. */
    private fun sequenceEnd(text: String, start: Int): Int? {
        val kind = text.getOrNull(start + 1) ?: return null
        return when (kind) {
            '[' -> {
                var i = start + 2
                while (i < text.length && text[i].code !in 0x40..0x7E) i++
                if (i < text.length) i + 1 else null
            }
            ']' -> {
                var i = start + 2
                while (i < text.length) {
                    if (text[i] == BEL) return i + 1
                    if (text[i] == ESC && text.getOrNull(i + 1) == '\\') return i + 2
                    if (text[i] == ESC && i + 1 >= text.length) return null
                    i++
                }
                null
            }
            else -> start + 2
        }
    }
}

/** [style] after the SGR codes in [params], such as `1;31` or `38;5;208`. An empty list resets. */
fun applySgr(style: AnsiStyle, params: String): AnsiStyle {
    val codes = if (params.isEmpty()) listOf(0) else params.split(';', ':').map { it.toIntOrNull() ?: 0 }
    var s = style
    var i = 0
    while (i < codes.size) {
        val code = codes[i]
        when (code) {
            0 -> s = AnsiStyle.Plain
            1 -> s = s.copy(bold = true)
            2 -> s = s.copy(dim = true)
            3 -> s = s.copy(italic = true)
            4 -> s = s.copy(underline = true)
            7 -> s = s.copy(inverse = true)
            9 -> s = s.copy(strikethrough = true)
            22 -> s = s.copy(bold = false, dim = false)
            23 -> s = s.copy(italic = false)
            24 -> s = s.copy(underline = false)
            27 -> s = s.copy(inverse = false)
            29 -> s = s.copy(strikethrough = false)
            in 30..37 -> s = s.copy(foreground = AnsiColor.Indexed(code - 30))
            39 -> s = s.copy(foreground = null)
            in 40..47 -> s = s.copy(background = AnsiColor.Indexed(code - 40))
            49 -> s = s.copy(background = null)
            in 90..97 -> s = s.copy(foreground = AnsiColor.Indexed(code - 90 + 8))
            in 100..107 -> s = s.copy(background = AnsiColor.Indexed(code - 100 + 8))
            SGR_EXTENDED_FOREGROUND, SGR_EXTENDED_BACKGROUND -> {
                val (color, used) = extendedColor(codes, i + 1)
                if (color != null) s = if (code == SGR_EXTENDED_FOREGROUND) s.copy(foreground = color) else s.copy(background = color)
                i += used
            }
        }
        i++
    }
    return s
}

/** The color of a `38;…` or `48;…` code whose mode starts at [from], and how many codes it took. */
private fun extendedColor(codes: List<Int>, from: Int): Pair<AnsiColor?, Int> = when (codes.getOrNull(from)) {
    EXTENDED_INDEXED -> codes.getOrNull(from + 1)?.let { AnsiColor.Indexed(it.coerceIn(0, 255)) } to 2
    EXTENDED_RGB -> {
        val rgb = codes.subList(from + 1, minOf(from + 4, codes.size)).map { it.coerceIn(0, 255) }
        (if (rgb.size == 3) AnsiColor.Rgb(rgb[0], rgb[1], rgb[2]) else null) to 4
    }
    else -> null to 0
}
