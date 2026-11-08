package dev.chieppa.stretch.code

/**
 * A place between characters of a code view's text: [line] counts from 1 and [column] is the offset of the character
 * after it within the line, counted before tabs expand, as [CodeRange] counts.
 */
data class CodePosition(val line: Int, val column: Int)
