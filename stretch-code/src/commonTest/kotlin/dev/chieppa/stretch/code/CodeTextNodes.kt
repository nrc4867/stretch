package dev.chieppa.stretch.code

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextDecoration

/** Longer than a tooltip waits before it shows. */
internal const val TOOLTIP_WAIT_MS = 600L

/** The text a node shows, with its styles. */
internal fun SemanticsNodeInteraction.annotated(): AnnotatedString = fetchSemanticsNode().config[SemanticsProperties.Text].first()

/** How the text of a node is laid out. */
internal fun SemanticsNodeInteraction.textLayout(): TextLayoutResult {
    val results = mutableListOf<TextLayoutResult>()
    fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
    return results.first()
}

/** The middle of the character at [offset] of a node's text, in the node's own pixels. */
internal fun SemanticsNodeInteraction.characterCenter(offset: Int): Offset = textLayout().getBoundingBox(offset).center

/** Just inside the start edge of the character at [offset], nearest the place before it. */
internal fun SemanticsNodeInteraction.characterStart(offset: Int): Offset = textLayout().getBoundingBox(offset).let { Offset(it.left + 1f, it.center.y) }

/** The run of a node's text drawn underlined as a link, as start and end, or null for none. */
internal fun SemanticsNodeInteraction.underlined(): Pair<Int, Int>? {
    if (fetchSemanticsNode().config.getOrNull(SemanticsProperties.Text) == null) return null
    return annotated().spanStyles.firstOrNull { it.item.textDecoration == TextDecoration.Underline }?.let { it.start to it.end }
}
