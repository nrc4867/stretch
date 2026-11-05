package dev.chieppa.stretch.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.frame.Menu
import dev.chieppa.stretch.frame.MenuItem
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.ui.IconButton
import dev.chieppa.stretch.ui.Segmented
import dev.chieppa.stretch.ui.Tooltip

private val headingTypes = listOf(BlockType.Paragraph, BlockType.Heading1, BlockType.Heading2, BlockType.Heading3)

/** The icon of each block type, in the order the toolbar and slash menu show them. */
internal val blockIcons: Map<BlockType, ImageVector> = mapOf(
    BlockType.Paragraph to StretchIcons.File,
    BlockType.Heading1 to StretchIcons.Heading,
    BlockType.Heading2 to StretchIcons.Heading,
    BlockType.Heading3 to StretchIcons.Heading,
    BlockType.Bullet to StretchIcons.ListBullet,
    BlockType.Numbered to StretchIcons.ListOrdered,
    BlockType.Task to StretchIcons.ListTask,
    BlockType.Quote to StretchIcons.Quote,
    BlockType.Callout to StretchIcons.Info,
    BlockType.Code to StretchIcons.Code,
    BlockType.Diagram to StretchIcons.Diagram,
    BlockType.Table to StretchIcons.Table,
    BlockType.Image to StretchIcons.Image,
    BlockType.Divider to StretchIcons.Divider,
)

@Composable
private fun ToolAction(icon: ImageVector, label: String, enabled: Boolean = true, on: Boolean = false, onClick: () -> Unit) {
    // A field losing the focus collapses its selection, and these act on that selection, so they never take the focus.
    Tooltip(label) { IconButton(icon, label, onClick, Modifier.focusProperties { canFocus = false }, enabled = enabled, on = on) }
}

@Composable
private fun Separator() {
    Box(Modifier.padding(horizontal = 4.dp).width(Stretch.stroke).height(18.dp).background(Stretch.colors.linePanel))
}

/** The editor's bar: undo, inline styles, block types, and the mode switch when the app offers more than one mode. */
@Composable
internal fun EditorToolbar(state: MarkdownEditorState, modes: List<MarkdownMode>, strings: MarkdownStrings) {
    val c = Stretch.colors
    Row(
        Modifier.fillMaxWidth().background(c.surfaceRail).horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToolAction(StretchIcons.Undo, strings.undo, state.canUndo) { state.undo() }
        ToolAction(StretchIcons.Redo, strings.redo, state.canRedo) { state.redo() }
        Separator()
        ToolAction(StretchIcons.Bold, strings.bold) { state.toggleInline(InlineStyle.Bold) }
        ToolAction(StretchIcons.Italic, strings.italic) { state.toggleInline(InlineStyle.Italic) }
        ToolAction(StretchIcons.Strikethrough, strings.strikethrough) { state.toggleInline(InlineStyle.Strike) }
        ToolAction(StretchIcons.Code, strings.inlineCode) { state.toggleInline(InlineStyle.Code) }
        ToolAction(StretchIcons.Link, strings.link) { state.toggleInline(InlineStyle.Link) }
        Separator()
        HeadingMenu(state, strings)
        for (type in BlockType.entries - headingTypes.toSet()) {
            ToolAction(blockIcons.getValue(type), strings.blockType(type)) { state.setBlockType(type) }
        }
        if (modes.size > 1) {
            Separator()
            Segmented(modes, state.mode, { state.mode = it }, { if (it == MarkdownMode.LivePreview) strings.livePreview else strings.wysiwyg })
        }
    }
    Box(Modifier.fillMaxWidth().height(Stretch.stroke).background(c.linePanel))
}

@Composable
private fun HeadingMenu(state: MarkdownEditorState, strings: MarkdownStrings) {
    var open by remember { mutableStateOf(false) }
    Box {
        ToolAction(StretchIcons.Heading, strings.blockType(BlockType.Heading1), on = open) { open = !open }
        Menu(open, { open = false }) {
            for (type in headingTypes) {
                MenuItem(strings.blockType(type), { open = false; state.setBlockType(type) }, blockIcons[type])
            }
        }
    }
}
