package dev.chieppa.stretch.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.frame.chord
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.ui.Scrollbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

/** Typing that pauses this long ends an undo step. */
private const val TYPING_PAUSE_MILLIS = 600L

private val readerWidth = 760.dp

/**
 * A rich editor for a markdown file. Each block draws as what it is: headings, lists, tasks, quotes, callouts, code with
 * its syntax colored, tables, images, dividers and diagrams. What the editor saves is markdown, and blocks no one
 * touched stay as they were in the file, byte for byte.
 *
 * [modes] are the ways of editing the app offers: with more than one, the toolbar lets a person switch, and the state's
 * `mode` says which is on. [readOnly] turns it into a viewer. Typing `/` in an empty block lists the block types.
 * Pass [diagrams] to draw diagrams (`stretch-mermaid` has one for Mermaid) and [images] to load pictures; without them
 * diagrams show as code and pictures as a marked placeholder.
 */
@Composable
fun MarkdownEditor(
    state: MarkdownEditorState,
    modifier: Modifier = Modifier,
    modes: List<MarkdownMode> = listOf(state.mode),
    readOnly: Boolean = false,
    showToolbar: Boolean = true,
    diagrams: DiagramRenderer? = null,
    images: ImageSource? = null,
    onLinkClick: (String) -> Unit = {},
    strings: MarkdownStrings = MarkdownStrings(),
) {
    val c = Stretch.colors
    val list = rememberLazyListState()
    val root = remember { FocusRequester() }
    val clipboard = LocalClipboardManager.current
    LaunchedEffect(state) { state.history.document.collectLatest { state.followHistory() } }
    LaunchedEffect(state) {
        snapshotFlow { state.document }.collectLatest {
            delay(TYPING_PAUSE_MILLIS)
            state.endTyping()
        }
    }
    // A block off screen has no field to take the focus, so the list brings it in first.
    LaunchedEffect(state) {
        snapshotFlow { state.focusTicket }.collectLatest {
            val index = state.document.indexOf(state.focusedBlock ?: return@collectLatest)
            if (index >= 0 && list.layoutInfo.visibleItemsInfo.none { it.index == index }) list.scrollToItem(index)
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.selectedBlocks != null }.collectLatest { if (it) root.requestFocus() }
    }
    val context = remember(state, diagrams, images, onLinkClick, strings, readOnly) {
        BlockContext(state, diagrams, images, onLinkClick, strings, readOnly)
    }
    Column(modifier.background(c.surfacePanel)) {
        if (showToolbar && !readOnly) EditorToolbar(state, modes, strings)
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .focusRequester(root)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown || state.selectedBlocks == null) return@onPreviewKeyEvent false
                    selectionKey(state, event.chord()?.toString()) { clipboard.setText(AnnotatedString(it)) }
                }
                .focusable(),
        ) {
            LazyColumn(
                Modifier.fillMaxSize(),
                list,
                contentPadding = PaddingValues(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(blockGap),
            ) {
                itemsIndexed(state.document.blocks, key = { _, block -> block.id }, contentType = { _, block -> block.kind::class.simpleName }) { index, block ->
                    val selected = state.selectedBlocks?.contains(index) == true
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .then(if (selected) Modifier.background(c.onFill) else Modifier)
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Box(Modifier.widthIn(max = readerWidth).fillMaxWidth()) { BlockView(block, context) }
                    }
                }
            }
            Scrollbar(list, Modifier.align(Alignment.CenterEnd))
        }
    }
}

/** The keys that act on selected blocks. Returns true for a key it used. */
private fun selectionKey(state: MarkdownEditorState, chord: String?, copy: (String) -> Unit): Boolean {
    when (chord) {
        "Ctrl C" -> state.selectedSource?.let(copy)
        "Ctrl X" -> {
            state.selectedSource?.let(copy)
            state.deleteSelectedBlocks()
        }
        "Delete", "Backspace" -> state.deleteSelectedBlocks()
        "Esc" -> state.clearBlockSelection()
        "Ctrl A" -> state.selectAllBlocks()
        else -> return false
    }
    return true
}
