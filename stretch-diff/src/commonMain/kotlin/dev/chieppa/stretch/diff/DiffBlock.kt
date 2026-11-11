package dev.chieppa.stretch.diff

/**
 * A run of unchanged lines of file [fileIndex], by their new-side numbers [lines], that [DiffSlots.lineBlock] draws in
 * place of their code. It is drawn only while every line of it shows, in order, in the diff; otherwise its lines draw
 * as code.
 */
data class DiffBlock(val fileIndex: Int, val lines: IntRange)
