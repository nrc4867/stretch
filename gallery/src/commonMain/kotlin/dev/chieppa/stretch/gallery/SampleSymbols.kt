package dev.chieppa.stretch.gallery

import dev.chieppa.stretch.code.CodeDecoration
import dev.chieppa.stretch.code.CodeDecorationStyle
import dev.chieppa.stretch.code.CodePosition
import dev.chieppa.stretch.code.CodeRange

/** Words that start a definition in the sample code: the name after one is defined there. */
private val definers = setOf("fun", "val", "var", "class")

/** A name in code. */
private val identifier = Regex("[A-Za-z_][A-Za-z0-9_]*")

/**
 * Names in sample code found by their words alone, the way an app's parser would find symbols: a name's definition is
 * its first place after `fun`, `val`, `var` or `class`, and every other place is a usage. Enough to show clicking
 * through to symbols in the gallery.
 */
internal class SampleSymbols(private val lines: List<String>) {
    private val places: Map<String, List<CodeRange>> = buildMap<String, MutableList<CodeRange>> {
        lines.forEachIndexed { index, line ->
            identifier.findAll(line).forEach { word ->
                if (word.value !in definers) getOrPut(word.value) { mutableListOf() } += CodeRange(index + 1, word.range.first, word.range.last + 1)
            }
        }
    }

    /** The name at [position], or null off a name. */
    fun nameAt(position: CodePosition): CodeRange? {
        val line = lines.getOrNull(position.line - 1) ?: return null
        val word = identifier.findAll(line).firstOrNull { position.column in it.range }?.value ?: return null
        return places[word]?.firstOrNull { it.line == position.line && position.column in it.start until it.end }
    }

    /** Where the name at [range] is defined, if it is defined in the text. */
    fun definitionOf(range: CodeRange): CodeRange? = places[nameOf(range)]?.firstOrNull { isDefinition(it) }

    /** Every place the name at [range] is used, other than where it is defined. */
    fun usagesOf(range: CodeRange): List<CodeRange> = places[nameOf(range)].orEmpty().filterNot { isDefinition(it) }

    /** The name at [range] decorated: its definition and every usage. */
    fun decorate(range: CodeRange): List<CodeDecoration> =
        listOfNotNull(definitionOf(range)?.let { CodeDecoration(it, CodeDecorationStyle.Definition, "Defined here") }) +
            usagesOf(range).map { CodeDecoration(it, CodeDecorationStyle.Reference) }

    /** The text of line [number], counting from 1. */
    fun line(number: Int): String = lines[number - 1]

    private fun nameOf(range: CodeRange) = lines[range.line - 1].substring(range.start, range.end)

    private fun isDefinition(range: CodeRange): Boolean {
        val before = lines[range.line - 1].substring(0, range.start).trimEnd()
        return identifier.findAll(before).lastOrNull()?.let { it.value in definers && it.range.last == before.lastIndex } == true
    }
}
