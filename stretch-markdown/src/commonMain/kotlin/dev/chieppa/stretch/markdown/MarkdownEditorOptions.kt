package dev.chieppa.stretch.markdown

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.Dp
import dev.chieppa.stretch.theme.StretchColors

/** A picture of a diagram, and the size to show it at. */
class DiagramPicture(val image: ImageBitmap, val width: Dp, val height: Dp)

/** What a diagram is drawn for: the theme to match, and how many pixels each dp has. */
data class DiagramStyle(val colors: StretchColors, val density: Float, val dark: Boolean)

/** Draws code blocks of some language as pictures, such as `stretch-mermaid`'s renderer for `mermaid` blocks. */
interface DiagramRenderer {
    fun handles(language: String): Boolean

    /** The picture of [source]. Throws with a message saying what is wrong with it. */
    suspend fun render(source: String, style: DiagramStyle): DiagramPicture
}

/** Finds the picture at an image's address, or null when there is none. Stretch has no network of its own, so the app decides. */
fun interface ImageSource {
    suspend fun load(url: String): ImageBitmap?
}

/** Text a [MarkdownEditor] or [MarkdownView] shows. Apps pass their own for other languages. */
data class MarkdownStrings(
    val placeholder: String = "",
    val livePreview: String = "Live preview",
    val wysiwyg: String = "Rich text",
    val undo: String = "Undo",
    val redo: String = "Redo",
    val bold: String = "Bold",
    val italic: String = "Italic",
    val strikethrough: String = "Strikethrough",
    val inlineCode: String = "Code",
    val link: String = "Link",
    val addRow: String = "Add a row",
    val removeRow: String = "Remove this row",
    val addColumn: String = "Add a column",
    val removeColumn: String = "Remove this column",
    val language: String = "Language",
    val imageAddress: String = "Image address",
    val imageDescription: String = "Description",
    /** What a `<details>` with no summary shows in its place. */
    val details: String = "Details",
    /** Said of a `<details>` that shows what it holds. */
    val open: String = "Open",
    /** Said of a `<details>` that hides what it holds. */
    val closed: String = "Closed",
    val blockType: (BlockType) -> String = {
        when (it) {
            BlockType.Paragraph -> "Text"
            BlockType.Heading1 -> "Heading 1"
            BlockType.Heading2 -> "Heading 2"
            BlockType.Heading3 -> "Heading 3"
            BlockType.Bullet -> "Bulleted list"
            BlockType.Numbered -> "Numbered list"
            BlockType.Task -> "Task list"
            BlockType.Quote -> "Quote"
            BlockType.Callout -> "Callout"
            BlockType.Code -> "Code block"
            BlockType.Diagram -> "Diagram"
            BlockType.Table -> "Table"
            BlockType.Image -> "Image"
            BlockType.Divider -> "Divider"
        }
    },
)
