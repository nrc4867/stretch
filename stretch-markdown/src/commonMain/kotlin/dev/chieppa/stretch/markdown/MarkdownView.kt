package dev.chieppa.stretch.markdown

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier

/**
 * Markdown shown read-only, as tall as its content, to sit inside other content such as a card or a scrolling column
 * of comments: it has no scrolling and no toolbar of its own, and its text selects. Blocks draw as a [MarkdownEditor]
 * draws them, and GitHub's HTML draws too: `<details>` opens on a click on its summary, and paragraphs, headings,
 * lists, quotes, code, links, pictures, tables and inline styles show as what they are, while comments, link
 * reference definitions and tags it does not know show nothing but their text. Pictures, from `![alt](url)` or
 * `<img>`, load through [images] and show under their text; [onLinkClick] gets a link clicked.
 */
@Composable
fun MarkdownView(
    text: String,
    modifier: Modifier = Modifier,
    images: ImageSource? = null,
    onLinkClick: (String) -> Unit = {},
    strings: MarkdownStrings = MarkdownStrings(),
    diagrams: DiagramRenderer? = null,
) {
    val state = remember(text) { MarkdownEditorState(text, MarkdownMode.Wysiwyg) }
    val styles = rememberRichStyles()
    val nodes = remember(state, styles) { viewTree(state.document.blocks, styles) }
    val click by rememberUpdatedState(onLinkClick)
    val context = remember(state, diagrams, images, strings, styles) {
        val onLink: (String) -> Unit = { click(it) }
        val blockText: BlockTextView = { block, style, textModifier -> ReadOnlyBlockText(block, block.kind, style, textModifier, styles, images, onLink) }
        BlockContext(state, diagrams, images, onLink, strings, readOnly = true, text = blockText)
    }
    SelectionContainer(modifier) { ViewNodes(nodes, context) }
}
