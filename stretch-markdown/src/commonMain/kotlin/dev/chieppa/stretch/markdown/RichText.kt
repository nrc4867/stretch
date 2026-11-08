package dev.chieppa.stretch.markdown

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em

/** How text read for showing, not editing, can be styled: markdown's inline styles and HTML's `<sub>` and `<sup>`. */
internal enum class RichStyle { Bold, Italic, Strike, Code, Link, Sub, Sup }

/** The span style for each [RichStyle]. */
internal typealias RichStyles = (RichStyle) -> SpanStyle

internal fun richStyleOf(style: InlineStyle): RichStyle = when (style) {
    InlineStyle.Bold -> RichStyle.Bold
    InlineStyle.Italic -> RichStyle.Italic
    InlineStyle.Strike -> RichStyle.Strike
    InlineStyle.Code -> RichStyle.Code
    InlineStyle.Link -> RichStyle.Link
}

/** A picture to show as a block of its own: from `![alt](url)` or `<img>`, linking to [link] when it sits in a link. */
internal data class Picture(val url: String, val alt: String, val width: Int? = null, val link: String? = null)

/** Text ready to show, with links as [URL_ANNOTATION]s, and the pictures it held, which show after it. */
internal class RichText(val text: AnnotatedString, val pictures: List<Picture> = emptyList())

/** How small `<sub>` and `<sup>` text is beside the text around it. */
private val SHIFTED_SIZE = 0.8.em

/** The theme's styles for [RichStyle]s, the same as the editor draws markdown's. */
@Composable
internal fun rememberRichStyles(): RichStyles {
    val look = rememberMarkupLook()
    return remember(look) {
        { style ->
            when (style) {
                RichStyle.Bold -> SpanStyle(fontWeight = FontWeight.SemiBold)
                RichStyle.Italic -> SpanStyle(fontStyle = FontStyle.Italic)
                RichStyle.Strike -> SpanStyle(textDecoration = TextDecoration.LineThrough)
                RichStyle.Code -> SpanStyle(fontFamily = look.mono, background = look.codeFill)
                RichStyle.Link -> SpanStyle(color = look.link, textDecoration = TextDecoration.Underline)
                RichStyle.Sub -> SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = SHIFTED_SIZE)
                RichStyle.Sup -> SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = SHIFTED_SIZE)
            }
        }
    }
}

/** [text] with each link annotation made clickable, calling [onLinkClick] with its address. */
internal fun clickableLinks(text: AnnotatedString, onLinkClick: (String) -> Unit): AnnotatedString {
    val links = text.getStringAnnotations(URL_ANNOTATION, 0, text.length)
    if (links.isEmpty()) return text
    return AnnotatedString.Builder(text).apply {
        for (link in links) addLink(LinkAnnotation.Clickable(link.item) { onLinkClick(link.item) }, link.start, link.end)
    }.toAnnotatedString()
}
