package dev.chieppa.stretch.markdown

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import dev.chieppa.stretch.core.Applied
import dev.chieppa.stretch.core.Command
import dev.chieppa.stretch.core.History

private fun replaceWith(document: MarkdownDocument): Command<MarkdownDocument> =
    Command { old -> Applied(document, replaceWith(old)) }

/**
 * The document in a [MarkdownEditor], with where the caret is. It keeps undo in [history], so a `DocumentSession` given
 * the same history tracks unsaved changes. Typing in one go is one undo step.
 */
@Stable
class MarkdownEditorState(
    val history: History<MarkdownDocument>,
    mode: MarkdownMode = MarkdownMode.LivePreview,
) {
    constructor(text: String = "", mode: MarkdownMode = MarkdownMode.LivePreview) : this(History(MarkdownDocument.parse(text)), mode)

    /** How markers show. An app can let people switch by passing several modes to the editor. */
    var mode by mutableStateOf(mode)

    var document by mutableStateOf(history.document.value)
        private set

    /** The markdown as it stands. */
    val text: String get() = document.text

    /** The block with the caret, which stays set while a toolbar has the focus. */
    var focusedBlock by mutableStateOf(document.blocks.firstOrNull()?.id)
        private set

    /** The focused block's text field: its text, selection and any composition an input method has open. */
    var fieldValue by mutableStateOf(TextFieldValue(document.blocks.firstOrNull()?.source.orEmpty()))
        private set

    /** Changes when a block should take the focus. It stays at 0 until then, so opening a document takes no focus. */
    internal var focusTicket by mutableIntStateOf(0)
        private set

    /** The block whose own field has the keyboard focus right now. */
    internal var revealedBlock by mutableStateOf<Long?>(null)

    /** The block being worked on, even while a toolbar has the focus. Null until someone starts on one. */
    internal val editingBlock: Long? get() = if (focusTicket > 0 || revealedBlock != null || edited) focusedBlock else null

    private var edited = false

    /** The blocks selected as a whole, as an index range, or null when the caret is in a block. */
    var selectedBlocks by mutableStateOf<IntRange?>(null)
        private set

    private var typing: History<MarkdownDocument>.Transaction? = null

    internal val hidesMarkers: Boolean get() = mode == MarkdownMode.Wysiwyg

    val canUndo: Boolean get() = typing != null || history.canUndo
    val canRedo: Boolean get() = typing == null && history.canRedo

    /** Replaces the whole document, as one undo step. */
    fun setText(text: String) {
        commit(MarkdownDocument.parse(text))
        focusOn(document.blocks.first().id, 0)
    }

    fun undo() {
        endTyping()
        if (history.undo()) followHistory()
    }

    fun redo() {
        endTyping()
        if (history.redo()) followHistory()
    }

    /** Ends the open run of typing, so the next keystroke starts a new undo step. */
    fun endTyping() {
        typing?.commit()
        typing = null
    }

    /** Brings the editor in line with [history] after someone else changed it, such as an app's Undo menu item. */
    internal fun followHistory() {
        val latest = history.document.value
        if (latest === document) return
        document = latest
        selectedBlocks = null
        val caret = latest.caret
        val block = caret?.let { c -> latest.blocks.firstOrNull { it.id == c.block } } ?: latest.blocks.first()
        val start = caret?.start ?: 0
        setField(block, start, caret?.end ?: start)
        focusTicket++
    }

    private fun setField(block: MarkdownBlock, start: Int, end: Int) {
        focusedBlock = block.id
        val length = block.source.length
        fieldValue = TextFieldValue(block.source, TextRange(start.coerceIn(0, length), end.coerceIn(0, length)))
    }

    /** One undoable change, which ends any run of typing. */
    private fun commit(next: MarkdownDocument) {
        endTyping()
        history.execute(replaceWith(next))
        settle(next)
    }

    /** A keystroke: it joins the run of typing still open. */
    private fun type(next: MarkdownDocument) {
        val run = typing ?: history.begin().also { typing = it }
        run.update(replaceWith(next))
        settle(next)
    }

    private fun settle(next: MarkdownDocument) {
        edited = true
        document = next
        selectedBlocks = null
        val caret = next.caret ?: return
        val block = next.blocks.firstOrNull { it.id == caret.block } ?: return
        val moved = caret.block != focusedBlock
        setField(block, caret.start, caret.end)
        if (moved) focusTicket++
    }

    /** Moves the caret to [start] in block [id], and the focus with it. */
    fun focusOn(id: Long, start: Int, end: Int = start) {
        val block = document.blocks.firstOrNull { it.id == id } ?: return
        endTyping()
        selectedBlocks = null
        setField(block, start, end)
        focusTicket++
    }

    /** The field of [id] gained focus by a click or by Tab. */
    internal fun noteFocused(id: Long) {
        if (focusedBlock == id) return
        val block = document.blocks.firstOrNull { it.id == id } ?: return
        endTyping()
        setField(block, block.source.length, block.source.length)
    }

    /** What a block's text field reports: typing, a paste, or only the caret moving. */
    internal fun onFieldChange(id: Long, value: TextFieldValue) {
        val block = document.blocks.firstOrNull { it.id == id } ?: return
        if (focusedBlock != id) noteFocused(id)
        val old = fieldValue
        val markup = if (hidesMarkers) blockMarkup(block.source, block.kind) else null
        if (value.text == block.source) {
            fieldValue = if (markup == null) value else value.copy(selection = snapSelection(old.selection, value.selection, markup.hidden, value.text.length))
            return
        }
        val fixed = if (markup == null) value else deleteThroughMarkers(old, value, markup)
        val next = document.editBlock(id, fixed.text, fixed.selection.start, fixed.selection.end)
        val keepsComposition = next.blocks.size == document.blocks.size && next.caret?.block == id
        type(next)
        if (keepsComposition && next.blocks.first { it.id == id }.source == fixed.text) fieldValue = fixed
    }

    /** Enter in [id]. */
    internal fun pressEnter(id: Long) = commit(document.splitBlock(id, caretIn(id).start))

    /** Backspace with the caret at the start of [id]'s text, returning false when it should act as usual. */
    internal fun pressBackspaceAtStart(id: Long): Boolean {
        val block = document.blocks.firstOrNull { it.id == id } ?: return false
        val selection = caretIn(id)
        if (selection.start != selection.end) return false
        val start = textStart(block.source, block.kind)
        return when {
            selection.start == 0 -> {
                if (block.source.isEmpty() || start == 0) commit(document.mergeWithPrevious(id)) else commit(document.stripMarker(id))
                true
            }
            selection.start == start && hidesMarkers && block.kind !is BlockKind.Code -> {
                commit(document.stripMarker(id))
                true
            }
            else -> false
        }
    }

    internal fun indent(id: Long, levels: Int) {
        val selection = caretIn(id)
        commit(document.with(caret = Caret(id, selection.start, selection.end)).indentBlock(id, levels))
    }

    private fun caretIn(id: Long): TextRange =
        if (focusedBlock == id) fieldValue.selection else TextRange(document.blocks.firstOrNull { it.id == id }?.source?.length ?: 0)

    /** Moves to the block before or after [id]. Returns false at the end of the document. */
    internal fun moveToNeighbor(id: Long, forward: Boolean): Boolean {
        val index = document.indexOf(id)
        val target = document.blocks.getOrNull(if (forward) index + 1 else index - 1) ?: return false
        val edge = if (forward) textStart(target.source, target.kind) else visibleEnd(target)
        focusOn(target.id, edge)
        return true
    }

    private fun visibleEnd(block: MarkdownBlock): Int {
        val kind = block.kind as? BlockKind.Code ?: return block.source.length
        return if (kind.closed) block.source.lastIndexOf('\n').coerceAtLeast(codeBodyStart(block.source)) else block.source.length
    }

    /** Turns the block with the caret into a block of [type], keeping its text. */
    fun setBlockType(type: BlockType) {
        val id = focusedBlock ?: return
        commit(document.convertBlock(id, type))
    }

    /** Puts bold, italic, strikethrough or code around the selection, or takes it off. */
    fun toggleInline(style: InlineStyle) = editInline { source, start, end ->
        when (style) {
            InlineStyle.Link -> insertLink(source, start, end)
            InlineStyle.Bold -> toggleWrap(source, start, end, "**")
            InlineStyle.Italic -> toggleWrap(source, start, end, "*")
            InlineStyle.Strike -> toggleWrap(source, start, end, "~~")
            InlineStyle.Code -> toggleWrap(source, start, end, "`")
        }
    }

    private fun editInline(edit: (String, Int, Int) -> Triple<String, Int, Int>) {
        val id = focusedBlock ?: return
        val block = document.blocks.firstOrNull { it.id == id } ?: return
        if (!block.kind.holdsPlainText()) return
        val selection = fieldValue.selection
        val (text, start, end) = edit(block.source, selection.min, selection.max)
        commit(document.editBlock(id, text, start, end))
        focusTicket++
    }

    /** The text of [id]'s check box was clicked. */
    fun setChecked(id: Long, checked: Boolean) = commit(document.setChecked(id, checked))

    /** Sets a table's cells; a table cell keeps its own caret, so the focus stays where it is. */
    internal fun updateTable(id: Long, table: MarkdownTable) {
        val source = table.toSource()
        val next = document.editBlock(id, source, 0)
        val run = typing ?: history.begin().also { typing = it }
        run.update(replaceWith(next))
        document = next
    }

    /** Replaces a block's source in one undo step, for controls other than its text, such as a code block's language. */
    internal fun replaceSource(id: Long, source: String) {
        val caret = source.length
        commit(document.editBlock(id, source, caret))
    }

    /** Selects every block, for copying or deleting them together. */
    fun selectAllBlocks() {
        endTyping()
        selectedBlocks = 0..document.blocks.lastIndex
    }

    /** Grows or shrinks the block selection from [id] by one block, starting one when there is none. */
    internal fun extendSelection(id: Long, forward: Boolean) {
        endTyping()
        val index = document.indexOf(id)
        val range = selectedBlocks ?: (index..index)
        selectedBlocks = when {
            selectedBlocks == null -> range
            forward -> range.first..(range.last + 1).coerceAtMost(document.blocks.lastIndex)
            else -> (range.first - 1).coerceAtLeast(0)..range.last
        }
    }

    /** The markdown of the selected blocks. */
    val selectedSource: String?
        get() = selectedBlocks?.let { document.sourceOfRange(it.first, it.last) }

    /** Removes the selected blocks. */
    fun deleteSelectedBlocks() {
        val range = selectedBlocks ?: return
        val next = document.removeBlocks(range.first, range.last)
        commit(next)
        focusTicket++
    }

    /** Removes one block. */
    internal fun deleteBlock(id: Long) {
        val index = document.indexOf(id)
        if (index >= 0) {
            commit(document.removeBlocks(index, index))
            focusTicket++
        }
    }

    /** Lets go of the block selection and returns the caret. */
    fun clearBlockSelection() {
        selectedBlocks = null
        focusTicket++
    }
}
