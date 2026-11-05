package dev.chieppa.stretch.markdown

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** How markdown's markers show while editing. */
enum class MarkdownMode {
    /** Blocks show formatted, and the block with the caret shows its markers, as a file's source would. */
    LivePreview,

    /** Markers never show. Formatting comes from shortcuts and menus, or from typing a marker, which then hides. */
    Wysiwyg,
}

/** A state for a [MarkdownEditor] starting from [text], kept across recompositions. */
@Composable
fun rememberMarkdownEditorState(text: String = "", mode: MarkdownMode = MarkdownMode.LivePreview): MarkdownEditorState =
    remember { MarkdownEditorState(text, mode) }
