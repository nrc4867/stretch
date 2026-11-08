package dev.chieppa.stretch.markdown

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle

/** A block's text, of [kind], read-only as [readOnlyText] reads it, with its pictures under it. */
@Composable
internal fun ReadOnlyBlockText(
    block: MarkdownBlock,
    kind: BlockKind,
    style: TextStyle,
    modifier: Modifier,
    styles: RichStyles,
    images: ImageSource?,
    onLinkClick: (String) -> Unit,
) {
    val rich = remember(block.source, kind, styles) { readOnlyText(block.source, kind, styles) }
    val text = remember(rich, onLinkClick) { clickableLinks(rich.text, onLinkClick) }
    if (rich.pictures.isEmpty()) {
        BasicText(text, modifier, style)
        return
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(blockGap)) {
        if (text.isNotBlank()) BasicText(text, style = style)
        rich.pictures.forEach { PictureView(it, images, onLinkClick) }
    }
}
