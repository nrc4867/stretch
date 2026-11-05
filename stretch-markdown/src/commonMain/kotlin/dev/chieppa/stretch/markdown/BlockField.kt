package dev.chieppa.stretch.markdown

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import dev.chieppa.stretch.frame.chord
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.ui.Label
import dev.chieppa.stretch.ui.holdsTyping

/** The text field of a block: what it shows, its keys, and handing the caret on to the blocks around it. */
@Composable
internal fun BlockField(block: MarkdownBlock, context: BlockContext, style: TextStyle, modifier: Modifier = Modifier) {
    val state = context.state
    val look = rememberMarkupLook()
    val requester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var slashPick by remember(block.id) { mutableIntStateOf(0) }
    var slashDismissed by remember(block.id) { mutableStateOf(false) }

    val isCurrent = state.focusedBlock == block.id
    val hide = context.readOnly || state.hidesMarkers || !focused
    val transformation = remember(block.kind, hide, look) { MarkupTransformation(block.kind, hide, look) }
    // Only the focused block holds a selection, so the others need no state of their own.
    val value = if (isCurrent && state.fieldValue.text == block.source) state.fieldValue else TextFieldValue(block.source, TextRange(block.source.length))

    LaunchedEffect(state.focusTicket) {
        if (state.focusTicket > 0 && isCurrent && !context.readOnly && state.selectedBlocks == null) requester.requestFocus()
    }

    val query = if (focused && !slashDismissed) slashQuery(block) else null
    val matches = query?.let { slashMatches(it, context.strings) }.orEmpty()
    val pick: (BlockType) -> Unit = { type ->
        state.replaceSource(block.id, "")
        state.setBlockType(type)
    }

    fun keys(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyDown) return false
        val chord = event.chord()?.toString() ?: return false
        if (matches.isNotEmpty()) {
            when (chord) {
                "Up" -> slashPick = (slashPick - 1).mod(matches.size)
                "Down" -> slashPick = (slashPick + 1).mod(matches.size)
                "Enter" -> pick(matches[slashPick.coerceAtMost(matches.lastIndex)])
                "Esc" -> slashDismissed = true
                else -> return false
            }
            return true
        }
        return blockKey(block, context, chord, value, layout, transformation)
    }

    BasicTextField(
        value = value,
        onValueChange = { state.onFieldChange(block.id, it) },
        modifier = modifier
            .focusRequester(requester)
            .holdsTyping()
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) {
                    state.revealedBlock = block.id
                    state.noteFocused(block.id)
                } else {
                    if (state.revealedBlock == block.id) state.revealedBlock = null
                    state.endTyping()
                }
            }
            .onPreviewKeyEvent(::keys)
            .linkClicks(context, { layout }, transformation),
        readOnly = context.readOnly,
        textStyle = style,
        cursorBrush = SolidColor(Stretch.colors.inkStrong),
        visualTransformation = transformation,
        onTextLayout = { layout = it },
        decorationBox = { inner ->
            if (block.source.isEmpty() && context.strings.placeholder.isNotEmpty() && state.document.blocks.size == 1) {
                Label(context.strings.placeholder, style, Stretch.colors.inkSubtle)
            }
            inner()
        },
    )
    if (matches.isNotEmpty()) {
        SlashMenu(matches, slashPick.coerceAtMost(matches.lastIndex), context.strings, pick) { slashDismissed = true }
    }
}

/** What a key does in a block's field, with the block-level keys shared by every kind. */
private fun blockKey(block: MarkdownBlock, context: BlockContext, chord: String, value: TextFieldValue, layout: TextLayoutResult?, transformation: MarkupTransformation): Boolean {
    val state = context.state
    val id = block.id
    if (context.readOnly) return false
    val isCode = block.kind is BlockKind.Code || block.kind == BlockKind.Raw
    when (chord) {
        "Ctrl Z" -> state.undo()
        "Ctrl Shift Z", "Ctrl Y" -> state.redo()
        "Ctrl B" -> state.toggleInline(InlineStyle.Bold)
        "Ctrl I" -> state.toggleInline(InlineStyle.Italic)
        "Ctrl E" -> state.toggleInline(InlineStyle.Code)
        "Ctrl K" -> state.toggleInline(InlineStyle.Link)
        "Ctrl Shift X" -> state.toggleInline(InlineStyle.Strike)
        "Ctrl A" -> {
            if (value.selection.length != value.text.length) return false
            state.selectAllBlocks()
        }
        "Enter" -> if (isCode) return false else state.pressEnter(id)
        "Backspace" -> return state.pressBackspaceAtStart(id)
        "Tab" -> when (block.kind) {
            is BlockKind.ListItem -> state.indent(id, 1)
            else -> return false
        }
        "Shift Tab" -> when (block.kind) {
            is BlockKind.ListItem -> state.indent(id, -1)
            else -> return false
        }
        "Up" -> return atEdgeLine(value, layout, transformation, last = false) && state.moveToNeighbor(id, forward = false)
        "Down" -> return atEdgeLine(value, layout, transformation, last = true) && state.moveToNeighbor(id, forward = true)
        "Shift Up" -> if (atEdgeLine(value, layout, transformation, last = false)) state.extendSelection(id, forward = false) else return false
        "Shift Down" -> if (atEdgeLine(value, layout, transformation, last = true)) state.extendSelection(id, forward = true) else return false
        "Left" -> return value.selection.collapsed && value.selection.start <= textStart(block.source, block.kind) && state.moveToNeighbor(id, forward = false)
        "Right" -> return value.selection.collapsed && value.selection.start >= value.text.length && state.moveToNeighbor(id, forward = true)
        else -> return false
    }
    return true
}

/** Whether the caret is on the first line of the field, or the last with [last]. */
private fun atEdgeLine(value: TextFieldValue, layout: TextLayoutResult?, transformation: MarkupTransformation, last: Boolean): Boolean {
    val result = layout ?: return true
    val shown = transformation.filter(AnnotatedString(value.text)).offsetMapping.originalToTransformed(value.selection.start)
    val line = result.getLineForOffset(shown.coerceAtMost(result.layoutInput.text.length))
    return if (last) line == result.lineCount - 1 else line == 0
}

/** Opens links on a click with the platform's modifier held, or on any click in a read-only view. */
private fun Modifier.linkClicks(context: BlockContext, layout: () -> TextLayoutResult?, transformation: MarkupTransformation): Modifier = pointerInput(context, transformation) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.type != PointerEventType.Press) continue
            val held = event.keyboardModifiers.isCtrlPressed || event.keyboardModifiers.isMetaPressed
            if (!held && !context.readOnly) continue
            val at: Offset = event.changes.firstOrNull()?.position ?: continue
            val result = layout() ?: continue
            val offset = result.getOffsetForPosition(at)
            val url = result.layoutInput.text.getStringAnnotations(URL_ANNOTATION, offset, offset + 1).firstOrNull()?.item ?: continue
            context.onLinkClick(url)
            event.changes.forEach { it.consume() }
        }
    }
}
