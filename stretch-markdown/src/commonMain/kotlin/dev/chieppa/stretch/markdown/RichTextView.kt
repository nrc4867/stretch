package dev.chieppa.stretch.markdown

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString

/** What an inline picture stands for in its text, for copying and screen readers when it has no alt text. */
private const val PICTURE_STANDS_FOR = "￼"

private fun pictureId(index: Int) = "picture-$index"

/**
 * [rich] text in [style], read-only: its links open through [onLinkClick], its inline code is drawn on a fill behind
 * the text so a selection shows over it, and its pictures load through [images]. A picture no taller than
 * [INLINE_PICTURE_HEIGHT], such as a badge, shows in the line where it sat; a bigger one, or one that did not load,
 * shows under the text.
 */
@Composable
internal fun RichTextView(rich: RichText, style: TextStyle, modifier: Modifier, images: ImageSource?, onLinkClick: (String) -> Unit) {
    val density = LocalDensity.current
    val loads = rich.pictures.mapIndexed { index, picture -> key(index, picture.url) { rememberPictureLoad(picture.url, images) } }
    val sizes = rich.pictures.mapIndexed { index, picture -> (loads[index] as? PictureLoad.Shown)?.let { pictureSize(picture, it.image, density) } }
    val inline = rich.pictures.indices.filter { sizes[it]?.let { size -> size.height <= INLINE_PICTURE_HEIGHT } == true }
    val text = remember(rich, inline, onLinkClick) { withPictures(clickableLinks(rich.text, onLinkClick), rich.places, inline) }
    val content = inline.associate { index ->
        val picture = rich.pictures[index]
        val size = sizes[index]!!
        val image = (loads[index] as PictureLoad.Shown).image
        val placeholder = with(density) { Placeholder(size.width.toSp(), size.height.toSp(), PlaceholderVerticalAlign.TextCenter) }
        pictureId(index) to InlineTextContent(placeholder) {
            Image(image, picture.alt, Modifier.fillMaxSize().opensLink(picture.link, onLinkClick))
        }
    }
    val fill = rememberMarkupLook().codeFill
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val codes = remember(text) { text.getStringAnnotations(CODE_ANNOTATION, 0, text.length) }
    val shows = text.isNotBlank() || inline.isNotEmpty()
    val behind = Modifier.drawBehind {
        val laid = layout ?: return@drawBehind
        if (laid.layoutInput.text != text) return@drawBehind
        for (code in codes) drawPath(laid.getPathForRange(code.start, code.end), fill)
    }
    val below = rich.pictures.indices.filter { it !in inline && loads[it] != PictureLoad.Loading }
    if (below.isEmpty()) {
        if (shows) BasicText(text, modifier.then(behind), style, onTextLayout = { layout = it }, inlineContent = content)
        return
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(blockGap)) {
        if (shows) BasicText(text, Modifier.fillMaxWidth().then(behind), style, onTextLayout = { layout = it }, inlineContent = content)
        below.forEach { PictureView(rich.pictures[it], loads[it], onLinkClick) }
    }
}

/** [text] with the pictures numbered in [inline] put in at their [places] as inline content. */
private fun withPictures(text: AnnotatedString, places: List<Int>, inline: List<Int>): AnnotatedString {
    if (inline.isEmpty()) return text
    return buildAnnotatedString {
        var at = 0
        for (index in inline.sortedBy { places[it] }) {
            val place = places[index].coerceIn(at, text.length)
            append(text.subSequence(at, place))
            appendInlineContent(pictureId(index), PICTURE_STANDS_FOR)
            at = place
        }
        append(text.subSequence(at, text.length))
    }
}
