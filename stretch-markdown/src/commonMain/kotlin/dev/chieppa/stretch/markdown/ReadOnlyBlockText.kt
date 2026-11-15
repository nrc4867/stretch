package dev.chieppa.stretch.markdown

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle

/** A block's text, of [kind], read-only as [readOnlyText] reads it, with its pictures in it or under it. */
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
    RichTextView(rich, style, modifier, images, onLinkClick)
}
