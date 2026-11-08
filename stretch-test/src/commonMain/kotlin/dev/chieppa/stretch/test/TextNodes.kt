package dev.chieppa.stretch.test

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextDecoration

/** The text a node shows, with its styles. */
fun SemanticsNodeInteraction.annotatedText(): AnnotatedString = fetchSemanticsNode().config[SemanticsProperties.Text].first()

/** How a node's text is laid out. */
fun SemanticsNodeInteraction.textLayout(): TextLayoutResult {
    val results = mutableListOf<TextLayoutResult>()
    fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
    return results.first()
}

/** The middle of the character at [offset] of a node's text, in the node's own pixels, to move the pointer to. */
fun SemanticsNodeInteraction.characterCenter(offset: Int): Offset = textLayout().getBoundingBox(offset).center

/** Just inside the start edge of the character at [offset] of a node's text, nearer the place before it than after. */
fun SemanticsNodeInteraction.characterStart(offset: Int): Offset = textLayout().getBoundingBox(offset).let { Offset(it.left + 1f, it.center.y) }

/** The first run of a node's text drawn underlined, as its start and end, or null when none is. */
fun SemanticsNodeInteraction.underlinedRun(): Pair<Int, Int>? =
    annotatedText().spanStyles.firstOrNull { it.item.textDecoration == TextDecoration.Underline }?.let { it.start to it.end }
