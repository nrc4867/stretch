package dev.chieppa.stretch.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.code.codeColors
import dev.chieppa.stretch.frame.chord
import dev.chieppa.stretch.theme.Icon
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.ui.Label

/** What every block view needs from its editor. */
internal class BlockContext(
    val state: MarkdownEditorState,
    val diagrams: DiagramRenderer?,
    val images: ImageSource?,
    val onLinkClick: (String) -> Unit,
    val strings: MarkdownStrings,
    val readOnly: Boolean,
)

@Composable
internal fun rememberMarkupLook(): MarkupLook {
    val c = Stretch.colors
    val mono = Stretch.type.valueSm.fontFamily ?: FontFamily.Monospace
    val code = codeColors
    return remember(c, mono) { MarkupLook(c.inkSubtle, c.syntaxType, c.surfaceControl, mono, code) }
}

private val bodyStyle: TextStyle
    @Composable @ReadOnlyComposable
    get() = Stretch.type.body.merge(TextStyle(color = Stretch.colors.ink))

private val codeStyle: TextStyle
    @Composable @ReadOnlyComposable
    get() = Stretch.type.valueSm.merge(TextStyle(color = Stretch.colors.ink))

@Composable
@ReadOnlyComposable
private fun headingStyle(level: Int): TextStyle {
    val ink = TextStyle(color = Stretch.colors.inkStrong)
    return when (level) {
        1 -> Stretch.type.pageTitle
        2 -> Stretch.type.boardTitle
        3 -> Stretch.type.heading
        4 -> Stretch.type.drawerTitle
        else -> Stretch.type.body.copy(fontWeight = FontWeight.SemiBold)
    }.merge(ink)
}

/** One block, drawn as what it is. */
@Composable
internal fun BlockView(block: MarkdownBlock, context: BlockContext) {
    when (val kind = block.kind) {
        BlockKind.Paragraph -> BlockField(block, context, bodyStyle, Modifier.fillMaxWidth())
        is BlockKind.Heading -> BlockField(block, context, headingStyle(kind.level), Modifier.fillMaxWidth().padding(top = (HEADING_SPACE_LEVELS - kind.level).coerceAtLeast(0).dp * 2))
        is BlockKind.ListItem -> ListItemView(block, kind, context)
        BlockKind.Quote -> QuoteView(block, context)
        is BlockKind.Callout -> CalloutView(block, kind, context)
        is BlockKind.Code -> if (context.diagrams?.handles(kind.language) == true) DiagramView(block, kind, context) else CodeView(block, kind, context)
        BlockKind.Table -> TableView(block, context)
        is BlockKind.Image -> ImageView(block, kind, context)
        BlockKind.Divider -> DividerView(block, context)
        BlockKind.Raw -> Box(Modifier.fillMaxWidth().clip(Stretch.shapes.ctlShape).background(Stretch.colors.surfaceControl).padding(10.dp)) {
            BlockField(block, context, codeStyle, Modifier.fillMaxWidth())
        }
    }
}

private const val HEADING_SPACE_LEVELS = 7
private val listGutter = 20.dp

@Composable
private fun ListItemView(block: MarkdownBlock, kind: BlockKind.ListItem, context: BlockContext) {
    val c = Stretch.colors
    val state = context.state
    // The caret's block in live preview shows its marker in its text, so the drawn one steps aside.
    val showsSource = !context.readOnly && !state.hidesMarkers && state.revealedBlock == block.id
    Row(Modifier.fillMaxWidth().padding(start = (kind.indent * LIST_INDENT_PER_SPACE).dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        if (!showsSource) {
            Box(Modifier.width(listGutter), contentAlignment = Alignment.TopCenter) {
                when (kind.style) {
                    ListStyle.Bullet -> Label("•", Stretch.type.body, c.inkMuted)
                    ListStyle.Ordered -> Label("${kind.number}.", Stretch.type.body, c.inkMuted)
                    ListStyle.Task -> TaskBox(kind.checked, plainTextOf(block), enabled = !context.readOnly) { state.setChecked(block.id, !kind.checked) }
                }
            }
        }
        val struck = kind.style == ListStyle.Task && kind.checked
        val ink = if (struck) TextStyle(color = c.inkSubtle) else TextStyle()
        BlockField(block, context, bodyStyle.merge(ink), Modifier.weight(1f))
    }
}

private const val LIST_INDENT_PER_SPACE = 8

/** A task's check box: only the box, since the block's own field is its label. */
@Composable
private fun TaskBox(checked: Boolean, label: String, enabled: Boolean, onToggle: () -> Unit) {
    val c = Stretch.colors
    val shape = RoundedCornerShape(3.dp)
    Box(
        Modifier
            .padding(top = 2.dp)
            .size(15.dp)
            .clip(shape)
            .background(if (checked) c.toggleOn else c.surfaceFloat)
            .border(Stretch.stroke, if (checked) c.toggleOnEdge else c.toggleOffEdge, shape)
            .toggleable(checked, enabled = enabled, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(StretchIcons.Check, c.onAccent, size = 12.dp)
    }
}

@Composable
private fun QuoteView(block: MarkdownBlock, context: BlockContext) {
    val c = Stretch.colors
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(c.lineControl))
        BlockField(block, context, bodyStyle.merge(TextStyle(color = c.inkMuted)), Modifier.weight(1f).padding(start = 10.dp))
    }
}

/** A callout's icon, and the edge and text colors for its kind. */
private class CalloutLook(val icon: ImageVector, val wash: Color, val edge: Color, val ink: Color)

@Composable
@ReadOnlyComposable
private fun calloutLook(kind: CalloutKind): CalloutLook {
    val c = Stretch.colors
    return when (kind) {
        CalloutKind.Note -> CalloutLook(StretchIcons.Info, c.surfaceControl, c.affected, c.ink)
        CalloutKind.Tip -> CalloutLook(StretchIcons.Check, c.surfaceControl, c.deltaUp, c.ink)
        CalloutKind.Important -> CalloutLook(StretchIcons.Help, c.onFill, c.onEdge, c.onInk)
        CalloutKind.Warning -> CalloutLook(StretchIcons.Warn, c.warnWash, c.warnEdge, c.warnInk)
        CalloutKind.Caution -> CalloutLook(StretchIcons.Error, c.errorWash, c.errorEdge, c.errorInk)
    }
}

@Composable
private fun CalloutView(block: MarkdownBlock, kind: BlockKind.Callout, context: BlockContext) {
    val look = calloutLook(kind.kind)
    val shape = Stretch.shapes.ctlShape
    Row(
        Modifier.fillMaxWidth().clip(shape).background(look.wash).border(Stretch.stroke, look.edge, shape).padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(look.icon, look.ink, contentDescription = kind.kind.name)
        BlockField(block, context, bodyStyle.merge(TextStyle(color = look.ink)), Modifier.weight(1f))
    }
}

@Composable
internal fun CodeView(block: MarkdownBlock, kind: BlockKind.Code, context: BlockContext, error: String? = null) {
    val c = Stretch.colors
    val shape = Stretch.shapes.ctlShape
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surfaceFloat)
            .border(Stretch.stroke, if (error != null) c.errorEdge else c.lineControl, shape)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CodeLanguageField(block, kind, context, error)
        BlockField(block, context, codeStyle, Modifier.fillMaxWidth())
    }
}

@Composable
private fun DividerView(block: MarkdownBlock, context: BlockContext) {
    val c = Stretch.colors
    val state = context.state
    val requester = remember { FocusRequester() }
    LaunchedEffect(state.focusTicket) {
        if (state.focusTicket > 0 && state.focusedBlock == block.id && !context.readOnly && state.selectedBlocks == null) requester.requestFocus()
    }
    val current = state.editingBlock == block.id && !context.readOnly
    Box(
        Modifier
            .fillMaxWidth()
            .then(if (current) Modifier.background(c.surfaceControl) else Modifier)
            .focusRequester(requester)
            .onFocusChanged { if (it.isFocused) state.noteFocused(block.id) }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.chord()?.toString()) {
                    "Backspace", "Delete" -> {
                        state.deleteBlock(block.id)
                        true
                    }
                    "Enter" -> {
                        state.pressEnter(block.id)
                        true
                    }
                    "Up", "Left" -> state.moveToNeighbor(block.id, forward = false)
                    "Down", "Right" -> state.moveToNeighbor(block.id, forward = true)
                    else -> false
                }
            }
            .clickable(enabled = !context.readOnly) { requester.requestFocus() }
            .focusable(enabled = !context.readOnly)
            .padding(vertical = 8.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(Stretch.stroke).background(c.linePanel))
    }
}

/** A marked empty frame in place of a picture that has no address or did not load. */
@Composable
internal fun MissingPicture(icon: ImageVector, description: String) {
    val c = Stretch.colors
    Row(
        Modifier.widthIn(min = 160.dp).clip(Stretch.shapes.ctlShape).border(Stretch.stroke, c.warnEdge, Stretch.shapes.ctlShape).background(c.warnWash).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, c.warnInk, Modifier.size(16.dp))
        Label(description, Stretch.type.bodySm, c.warnInk)
    }
}
