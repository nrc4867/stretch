package dev.chieppa.stretch.markdown

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle

/** Draws a block's text in [style]: the editor's field, or plain text in a [MarkdownView]. */
internal typealias BlockTextView = @Composable (block: MarkdownBlock, style: TextStyle, modifier: Modifier) -> Unit

/** What every block view needs from its editor or viewer. [text] draws block text in place of the editor's field. */
internal class BlockContext(
    val state: MarkdownEditorState,
    val diagrams: DiagramRenderer?,
    val images: ImageSource?,
    val onLinkClick: (String) -> Unit,
    val strings: MarkdownStrings,
    val readOnly: Boolean,
    val text: BlockTextView? = null,
)

/** A block's text, drawn by the context's [BlockContext.text] or else as the editor's field. */
@Composable
internal fun BlockText(block: MarkdownBlock, context: BlockContext, style: TextStyle, modifier: Modifier = Modifier) {
    val draw = context.text
    if (draw != null) draw(block, style, modifier) else BlockField(block, context, style, modifier)
}
