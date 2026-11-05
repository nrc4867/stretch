package dev.chieppa.stretch.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import dev.chieppa.stretch.code.CodeColors
import dev.chieppa.stretch.code.CodeLanguages
import dev.chieppa.stretch.code.spanOf
import dev.chieppa.stretch.code.tokenize

/** The theme's choices for drawing markup. */
internal class MarkupLook(
    val marker: Color,
    val link: Color,
    val codeFill: Color,
    val mono: FontFamily,
    val code: CodeColors,
)

internal const val URL_ANNOTATION = "url"

/** Offsets between a source and the same text with its [hidden] ranges left out. */
internal class HiddenOffsets(length: Int, hidden: List<IntRange>) : OffsetMapping {
    private val toShown = IntArray(length + 1)
    private val toSource: IntArray

    init {
        val isHidden = BooleanArray(length)
        for (range in hidden) for (at in range) if (at in 0 until length) isHidden[at] = true
        val sources = ArrayList<Int>(length + 1)
        var shown = 0
        for (at in 0 until length) {
            toShown[at] = shown
            if (!isHidden[at]) {
                sources += at
                shown++
            }
        }
        toShown[length] = shown
        sources += length
        toSource = sources.toIntArray()
    }

    override fun originalToTransformed(offset: Int): Int = toShown[offset.coerceIn(0, toShown.lastIndex)]

    override fun transformedToOriginal(offset: Int): Int = toSource[offset.coerceIn(0, toSource.lastIndex)]

    /** How many characters show. */
    val shownLength: Int get() = toSource.lastIndex
}

/** Draws a block's source with its formatting, leaving out its markers when [hide] is set and fading them otherwise. */
internal class MarkupTransformation(
    private val kind: BlockKind,
    private val hide: Boolean,
    private val look: MarkupLook,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val source = text.text
        val markup = blockMarkup(source, kind)
        val hidden = if (hide) markup.hidden else emptyList()
        val offsets = HiddenOffsets(source.length, hidden)
        val builder = AnnotatedString.Builder()
        if (hide) {
            var at = 0
            for (range in hidden) {
                if (range.first > at) builder.append(source, at, range.first)
                at = range.last + 1
            }
            if (at < source.length) builder.append(source, at, source.length)
        } else {
            builder.append(source)
            for (range in markup.hidden) builder.addStyle(SpanStyle(color = look.marker), range.first, range.last + 1)
        }
        fun place(range: TextRange, style: SpanStyle) {
            val start = offsets.originalToTransformed(range.start)
            val end = offsets.originalToTransformed(range.end)
            if (start < end) builder.addStyle(style, start, end)
        }
        for (span in markup.spans) {
            place(TextRange(span.start, span.end), styleOf(span.style))
            if (span.url != null) {
                val start = offsets.originalToTransformed(span.start)
                val end = offsets.originalToTransformed(span.end)
                if (start < end) builder.addStringAnnotation(URL_ANNOTATION, span.url, start, end)
            }
        }
        (kind as? BlockKind.Code)?.let { code ->
            val bodyStart = codeBodyStart(source)
            val body = codeBody(source, code)
            for (token in tokenize(body, CodeLanguages.named(code.language))) {
                place(TextRange(bodyStart + token.start, bodyStart + token.end), look.code.spanOf(token.kind))
            }
        }
        return TransformedText(builder.toAnnotatedString(), offsets)
    }

    private fun styleOf(style: InlineStyle): SpanStyle = when (style) {
        InlineStyle.Bold -> SpanStyle(fontWeight = FontWeight.SemiBold)
        InlineStyle.Italic -> SpanStyle(fontStyle = FontStyle.Italic)
        InlineStyle.Strike -> SpanStyle(textDecoration = TextDecoration.LineThrough)
        InlineStyle.Code -> SpanStyle(fontFamily = look.mono, background = look.codeFill)
        InlineStyle.Link -> SpanStyle(color = look.link, textDecoration = TextDecoration.Underline)
    }

    override fun equals(other: Any?) = other is MarkupTransformation && other.kind == kind && other.hide == hide && other.look === look

    override fun hashCode() = 31 * kind.hashCode() + hide.hashCode()
}
