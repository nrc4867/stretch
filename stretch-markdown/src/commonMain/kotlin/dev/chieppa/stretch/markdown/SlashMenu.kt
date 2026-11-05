package dev.chieppa.stretch.markdown

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.PopupProperties
import dev.chieppa.stretch.frame.BelowAnchor
import dev.chieppa.stretch.frame.MenuItem
import dev.chieppa.stretch.frame.MenuPanel
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.ui.StretchPopup

private const val SLASH = "/"

/** What a paragraph typed as `/word` is filtering the slash menu by, or null when it is not one. */
internal fun slashQuery(block: MarkdownBlock): String? {
    if (block.kind != BlockKind.Paragraph || !block.source.startsWith(SLASH) || block.source.any { it.isWhitespace() }) return null
    return block.source.removePrefix(SLASH)
}

/** The block types matching [query], by their names. */
internal fun slashMatches(query: String, strings: MarkdownStrings): List<BlockType> =
    BlockType.entries.filter { strings.blockType(it).contains(query, ignoreCase = true) }

/** The list under a `/` command. The field keeps the focus and its keys pick from this list. */
@Composable
internal fun SlashMenu(matches: List<BlockType>, highlighted: Int, strings: MarkdownStrings, onPick: (BlockType) -> Unit, onDismiss: () -> Unit) {
    if (matches.isEmpty()) return
    StretchPopup(popupPositionProvider = BelowAnchor(4), onDismissRequest = onDismiss, properties = PopupProperties(focusable = false), shadow = Stretch.shadows.pop) {
        MenuPanel {
            matches.forEachIndexed { index, type ->
                MenuItem(strings.blockType(type), { onPick(type) }, blockIcons[type], highlighted = index == highlighted)
            }
        }
    }
}
