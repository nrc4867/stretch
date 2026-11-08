package dev.chieppa.stretch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import dev.chieppa.stretch.theme.Icon
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.theme.StretchSizes
import dev.chieppa.stretch.theme.StretchSpacing
import kotlin.math.roundToInt

/** How far each level of a tree steps in. */
private val TreeIndent = StretchSpacing.space16

/** How faint the rows of a node being dragged are. */
private const val DRAGGED_ALPHA = 0.4f

/**
 * One row of a [Tree], for the node keyed [rowKey]. [check] is its checkbox when [checkable], null being mixed. [drag]
 * is null when rows cannot be moved; [onDrop] gets where the pointer let go, down the whole list. [onRenamed] gets the
 * new name, or null when renaming was cancelled. [onContextPress] gets a right-click or long press, in the row's
 * pixels, and [modifier] is the app's own for the row, around the tree's handling of the pointer.
 */
@Composable
internal fun <T> TreeRowView(
    row: TreeRow<T>,
    rowKey: Any,
    index: Int,
    label: String,
    icon: ImageVector?,
    open: Boolean,
    selected: Boolean,
    focused: Boolean,
    checkable: Boolean,
    check: Boolean?,
    faded: Boolean,
    renaming: Boolean,
    stateText: (Boolean) -> String,
    slot: Float,
    drag: TreeDragState?,
    onPress: () -> Unit,
    onClick: (PointerKeyboardModifiers) -> Unit,
    onDoubleClick: (() -> Unit)?,
    onToggleOpen: () -> Unit,
    onToggleCheck: () -> Unit,
    onDrop: (y: Float) -> Unit,
    onRenamed: (String?) -> Unit,
    onContextPress: ((Offset) -> Unit)?,
    modifier: Modifier,
    leading: @Composable RowScope.() -> Unit,
    trailing: @Composable RowScope.() -> Unit,
) {
    val c = Stretch.colors
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val ink = if (selected) c.onInk else c.ink
    val parentRow = row.node.children.isNotEmpty()
    Row(
        modifier
            .fillMaxWidth()
            .height(StretchSizes.ctl)
            .alpha(if (faded) DRAGGED_ALPHA else 1f)
            .clip(Stretch.shapes.rowShape)
            .background(if (selected) c.onFill else if (hovered) c.surfaceControl else Color.Transparent)
            .then(if (focused) Modifier.border(Stretch.theme.focusStroke, c.focus, Stretch.shapes.rowShape) else Modifier)
            .hoverable(source)
            .semantics {
                this.selected = selected
                if (parentRow) stateDescription = stateText(open)
            }
            .then(if (onContextPress != null) Modifier.onContextPress(onContextPress) else Modifier)
            .treeRowPointer(rowKey, index, slot, drag, onPress, onClick, onDoubleClick, onDrop)
            .padding(start = TreeIndent * row.depth + StretchSpacing.space4, end = StretchSpacing.space8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(StretchSpacing.space4),
    ) {
        Box(
            Modifier
                .size(StretchSizes.chip)
                .then(
                    if (parentRow) {
                        Modifier.clip(Stretch.shapes.segShape).controlPointer().clickable(role = Role.Button, onClick = onToggleOpen)
                            .semantics { contentDescription = "$label · ${stateText(!open)}" }
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (parentRow) Icon(if (open) StretchIcons.ChevronDown else StretchIcons.ChevronRight, if (selected) c.onInk else c.inkMuted, size = StretchSpacing.space12)
        }
        if (checkable) TreeCheck(check, label, onToggleCheck)
        icon?.let { Icon(it, if (selected) c.onInk else c.inkControl) }
        leading()
        if (renaming) {
            RenameField(label, { onRenamed(it) }) { onRenamed(null) }
        } else {
            Label(label, Stretch.type.body, ink, Modifier.weight(1f))
        }
        trailing()
    }
}

/** A row's checkbox. Its own click target, so checking never changes the selection. */
@Composable
private fun TreeCheck(check: Boolean?, label: String, onToggle: () -> Unit) {
    val state = rememberControlState()
    Box(
        Modifier
            .size(StretchSizes.chip)
            .hoverable(state.source)
            .controlPointer().triStateToggleable(
                state = when (check) {
                    true -> ToggleableState.On
                    false -> ToggleableState.Off
                    null -> ToggleableState.Indeterminate
                },
                interactionSource = state.source,
                indication = null,
                role = Role.Checkbox,
                onClick = onToggle,
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { CheckBoxFace(check, state) }
}

/**
 * Where a dragged row would land: a ring around the parent it would go into, or a line between rows. The list has
 * [scrolled] pixels above its top edge.
 */
@Composable
internal fun <T> TreeDropMark(target: TreeDrop, rows: List<TreeRow<T>>, key: (T) -> Any, dragging: Any?, slot: Float, scrolled: Float, modifier: Modifier) {
    val c = Stretch.colors
    if (target.into) {
        val r = rows.indexOfFirst { key(it.node.item) == target.parent }
        Box(
            modifier
                .offset { IntOffset(0, (r * slot - scrolled).roundToInt()) }
                .fillMaxWidth()
                .height(StretchSizes.ctl)
                .border(Stretch.theme.focusStroke, c.ringSelected, Stretch.shapes.rowShape),
        )
        return
    }
    val siblings = rows.filter { (it.parent?.let { p -> key(p.item) }) == target.parent && key(it.node.item) != dragging }
    val y = when {
        target.index < siblings.size -> rows.indexOf(siblings[target.index]) * slot
        else -> {
            val last = siblings.lastOrNull()
            if (last == null) 0f else {
                val i = rows.indexOf(last)
                ((i + 1 until rows.size).firstOrNull { rows[it].depth <= last.depth } ?: rows.size) * slot
            }
        }
    }
    val depth = target.parent?.let { p -> (rows.firstOrNull { key(it.node.item) == p }?.depth ?: -1) + 1 } ?: 0
    val line = Stretch.theme.focusStroke
    Row(modifier.offset { IntOffset(0, (y - scrolled - line.toPx() / 2).roundToInt()) }.fillMaxWidth().height(line)) {
        Spacer(Modifier.width(TreeIndent * depth + StretchSpacing.space24))
        Box(Modifier.weight(1f).fillMaxHeight().clip(Stretch.shapes.pillShape).background(c.ringSelected))
    }
}

@Composable
private fun RenameField(initial: String, onDone: (String) -> Unit, onCancel: () -> Unit) {
    var value by remember { mutableStateOf(TextFieldValue(initial, TextRange(0, initial.length))) }
    val focus = remember { FocusRequester() }
    var hadFocus by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { focus.requestFocus() }
    BasicTextField(
        value = value,
        onValueChange = { value = it },
        modifier = Modifier
            .holdsTyping()
            .focusRequester(focus)
            .onFocusChanged { if (it.isFocused) hadFocus = true else if (hadFocus) onDone(value.text) }
            .onPreviewKeyEvent {
                if (it.type == KeyEventType.KeyDown && it.key == Key.Escape) {
                    hadFocus = false
                    onCancel()
                    true
                } else {
                    false
                }
            }
            .background(Stretch.colors.surfaceFloat, Stretch.shapes.segShape)
            .border(Stretch.stroke, Stretch.colors.lineControl, Stretch.shapes.segShape)
            .padding(horizontal = StretchSpacing.space6, vertical = StretchSpacing.space2),
        singleLine = true,
        textStyle = Stretch.type.body.merge(TextStyle(color = Stretch.colors.inkStrong)),
        cursorBrush = SolidColor(Stretch.colors.inkStrong),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            hadFocus = false
            onDone(value.text)
        }),
    )
}
