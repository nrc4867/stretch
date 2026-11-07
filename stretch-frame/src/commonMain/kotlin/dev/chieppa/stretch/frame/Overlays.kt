package dev.chieppa.stretch.frame

import dev.chieppa.stretch.ui.opensOnPress
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.ui.StretchPopup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import dev.chieppa.stretch.core.Action
import dev.chieppa.stretch.core.Keymap
import dev.chieppa.stretch.frame.generated.Res
import dev.chieppa.stretch.frame.generated.close
import dev.chieppa.stretch.frame.generated.no_matches
import dev.chieppa.stretch.frame.generated.search_commands
import dev.chieppa.stretch.theme.Icon
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.theme.StretchSizes
import dev.chieppa.stretch.ui.Divider
import dev.chieppa.stretch.ui.holdsTyping
import dev.chieppa.stretch.ui.Button
import dev.chieppa.stretch.ui.ButtonKind
import dev.chieppa.stretch.ui.IconButton
import dev.chieppa.stretch.ui.Kbd
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/** Places a popup under its anchor, flipping above or shifting left when it would leave the window. */
class BelowAnchor(private val gap: Int) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val x = anchorBounds.left.coerceAtMost(windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val below = anchorBounds.bottom + gap
        val y = if (below + popupContentSize.height <= windowSize.height) below else (anchorBounds.top - gap - popupContentSize.height).coerceAtLeast(0)
        return IntOffset(x, y)
    }
}

/**
 * Centers a popup in the window, [top] pixels down, or in the middle when [top] is null, then moves it by [moved]
 * while keeping it inside the window.
 */
private class InWindow(private val top: Int?, private val moved: IntOffset = IntOffset.Zero) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val x = (windowSize.width - popupContentSize.width) / 2 + moved.x
        val y = (top ?: ((windowSize.height - popupContentSize.height) / 2)) + moved.y
        return IntOffset(
            x.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
            y.coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0)),
        )
    }
}

private const val DISABLED_ITEM_ALPHA = 0.4f

/** A dropdown or context menu under its anchor. Put [MenuItem], [MenuDivider] and [MenuLabel] inside. */
@Composable
fun Menu(expanded: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    if (!expanded) return
    StretchPopup(popupPositionProvider = BelowAnchor(4), onDismissRequest = onDismiss, properties = PopupProperties(focusable = true), shadow = Stretch.shadows.pop) {
        MenuPanel(content)
    }
}

/** The floating list a menu or context menu shows: as wide as its widest item, 200 to 360dp. */
@Composable
fun MenuPanel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .width(IntrinsicSize.Max)
            .widthIn(min = 200.dp, max = 360.dp)
            .floating(Stretch.shapes.cardShape, Stretch.shadows.pop)
            .padding(4.dp),
        content = content,
    )
}

/**
 * A popover: a small panel of live controls anchored to the thing that opened it, like a brush's settings or a
 * color picker. Unlike a [Menu] it stays open while you work in it; a click outside or Esc closes it. Changes in it
 * apply at once, so it has no OK button.
 */
@Composable
fun Popover(
    open: Boolean,
    onDismiss: () -> Unit,
    title: String? = null,
    width: Dp = 280.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!open) return
    val c = Stretch.colors
    StretchPopup(popupPositionProvider = BelowAnchor(6), onDismissRequest = onDismiss, properties = PopupProperties(focusable = true), shadow = Stretch.shadows.pop) {
        Column(
            Modifier
                .width(width)
                .floating(Stretch.shapes.cardShape, Stretch.shadows.pop)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                        onDismiss()
                        true
                    } else {
                        false
                    }
                },
        ) {
            if (title != null) {
                Row(
                    Modifier.fillMaxWidth().height(StretchSizes.row).padding(start = 12.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicText(title, Modifier.weight(1f), style = Stretch.type.body.merge(TextStyle(color = c.inkStrong)), maxLines = 1)
                    IconButton(StretchIcons.Close, stringResource(Res.string.close), onDismiss, size = StretchSizes.seg)
                }
                Divider()
            }
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
        }
    }
}

/** One command in a menu: icon, label and the key on the right. [checked] makes it a check item. One not [enabled] shows faint and takes no clicks. */
@Composable
fun MenuItem(
    text: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    keys: String? = null,
    checked: Boolean? = null,
    highlighted: Boolean = false,
    enabled: Boolean = true,
) {
    val c = Stretch.colors
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val faint = if (enabled) 1f else DISABLED_ITEM_ALPHA
    val pressed by source.collectIsPressedAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .height(StretchSizes.icon)
            .clip(Stretch.shapes.segShape)
            .background(if (pressed) c.surfaceControlPress else if (hovered || highlighted) c.surfaceControlHi else Color.Transparent)
            .hoverable(source)
            .clickable(source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            checked != null -> Box(Modifier.size(16.dp)) { if (checked) Icon(StretchIcons.Check, c.inkControl.copy(alpha = faint)) }
            icon != null -> Icon(icon, c.inkControl.copy(alpha = faint))
        }
        BasicText(text, Modifier.weight(1f), style = Stretch.type.body.merge(TextStyle(color = (if (hovered) c.inkStrong else c.ink).copy(alpha = faint))), maxLines = 1)
        if (keys != null) BasicText(keys, style = Stretch.type.valueSm.merge(TextStyle(color = c.inkSubtle.copy(alpha = faint))))
    }
}

/** A menu item for a registered action: its label, icon key and keys come from the registry and keymap. */
@Composable
fun ActionMenuItem(action: Action, keymap: Keymap, icons: (String) -> ImageVector? = { null }, onDone: () -> Unit = {}) {
    MenuItem(
        text = LocalActionText.current(action.textKey),
        onClick = {
            onDone()
            action.run()
        },
        icon = action.icon?.let(icons),
        keys = keymap.keysFor(action.id).firstOrNull()?.caps()?.joinToString(" "),
        enabled = action.isAvailable(),
    )
}

@Composable
fun MenuDivider() {
    Divider(Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
}

@Composable
fun MenuLabel(text: String) {
    BasicText(
        text.uppercase(),
        Modifier.padding(start = 8.dp, top = 6.dp, bottom = 2.dp),
        style = Stretch.type.eyebrow.merge(TextStyle(color = Stretch.colors.inkMuted)),
    )
}

/** A field that opens a menu of options, with an optional [icon] before the selected option's label. */
@Composable
fun <T> Dropdown(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val c = Stretch.colors
    var open by remember { mutableStateOf(false) }
    val shape = Stretch.shapes.ctlShape
    Box(modifier) {
        Row(
            Modifier
                .opensOnPress(open) { open = true }
                .height(StretchSizes.ctl)
                .clip(shape)
                .background(c.surfaceFloat)
                .border(Stretch.stroke, c.lineControl, shape)
                .clickable(role = Role.DropdownList) { open = true }
                .semantics { this.contentDescription = contentDescription }
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) Icon(icon, c.inkMuted)
            BasicText(label(selected), Modifier.weight(1f), style = Stretch.type.body.merge(TextStyle(color = c.inkStrong)), maxLines = 1)
            Icon(StretchIcons.ChevronDown, c.inkMuted)
        }
        Menu(open, { open = false }) {
            for (option in options) {
                MenuItem(label(option), { onSelect(option); open = false }, checked = option == selected)
            }
        }
    }
}

/**
 * The command palette: search every available action and run it. Up and Down move, Enter runs, Esc closes.
 * The list is exactly the registry's available actions, so a command missing here is missing everywhere.
 */
@Composable
fun CommandPalette(
    visible: Boolean,
    actions: List<Action>,
    keymap: Keymap,
    onDismiss: () -> Unit,
    icons: (String) -> ImageVector? = { null },
) {
    if (!visible) return
    val c = Stretch.colors
    val text = LocalActionText.current
    var query by remember { mutableStateOf("") }
    var index by remember { mutableIntStateOf(0) }
    val matches = searchLabels(query, actions) { text(it.textKey) }
    fun run(action: Action) {
        onDismiss()
        action.run()
    }
    SearchPalette(
        query = query,
        onQueryChange = {
            query = it
            index = 0
        },
        onDismiss = onDismiss,
        placeholder = stringResource(Res.string.search_commands),
        maxHeight = 360.dp,
        onKey = { event ->
            if (event.type != KeyEventType.KeyDown) return@SearchPalette false
            when (event.key) {
                Key.DirectionDown -> index = (index + 1).coerceAtMost(matches.lastIndex.coerceAtLeast(0))
                Key.DirectionUp -> index = (index - 1).coerceAtLeast(0)
                Key.Enter -> matches.getOrNull(index)?.let(::run)
                else -> return@SearchPalette false
            }
            true
        },
    ) {
        Column(Modifier.padding(6.dp)) {
            if (matches.isEmpty()) {
                BasicText(stringResource(Res.string.no_matches), Modifier.padding(8.dp), style = Stretch.type.body.merge(TextStyle(color = c.inkSubtle)))
            }
            matches.take(12).forEachIndexed { i, action ->
                MenuItem(
                    text = text(action.textKey),
                    onClick = { run(action) },
                    icon = action.icon?.let(icons),
                    keys = keymap.keysFor(action.id).firstOrNull()?.toString(),
                    highlighted = i == index,
                )
            }
        }
    }
}

/**
 * A modal panel for the few tasks that are not live edits: export, open, preferences. Never for "Are you sure?".
 * Drag its header to move it off what it covers; it stays inside the window and opens centered again next time.
 * Esc closes it. Like [Menu] and [Popover], compose it in a Box with the control that opens it: as a direct child of
 * a spaced Row or Column its zero-size anchor would add a gap. It opens after the frame it is composed in, so one open
 * from the window's first frame, as a startup dialog is, gets its size like any other.
 */
@Composable
fun StretchDialog(
    title: String,
    onDismiss: () -> Unit,
    icon: ImageVector? = null,
    width: Dp = 420.dp,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Stretch.colors
    var framed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withFrameNanos {}
        framed = true
    }
    if (!framed) return
    var moved by remember { mutableStateOf(IntOffset.Zero) }
    StretchPopup(popupPositionProvider = InWindow(top = null, moved), onDismissRequest = onDismiss, properties = PopupProperties(focusable = true), shadow = Stretch.shadows.float) {
        val shape = Stretch.shapes.cardShape
        var header by remember { mutableStateOf<LayoutCoordinates?>(null) }
        Column(
            Modifier
                .width(width)
                .floating(shape, Stretch.shadows.float, fill = c.surfacePanel),
        ) {
            Box(
                Modifier
                    .onGloballyPositioned { header = it }
                    .pointerHoverIcon(moveAround)
                    .pointerInput(Unit) {
                        var start = Offset.Zero
                        var from = IntOffset.Zero
                        detectDragGestures(
                            onDragStart = { at -> header?.let { start = it.localToScreen(at) }; from = moved },
                        ) { change, _ ->
                            val now = header?.localToScreen(change.position) ?: return@detectDragGestures
                            change.consume()
                            val d = now - start
                            moved = IntOffset(from.x + d.x.roundToInt(), from.y + d.y.roundToInt())
                        }
                    },
            ) {
                DrawerHeader(title, icon) { IconButton(StretchIcons.Close, stringResource(Res.string.close), onDismiss, size = StretchSizes.seg) }
            }
            content()
            Divider()
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
}

/** One toast at a time: what a command did, with Undo one click away. */
@Stable
class ToastState {
    internal var current by mutableStateOf<Toast?>(null)

    /** Whether a toast is up, for a host that gives it a place only while it needs one. */
    val showing: Boolean get() = current != null

    fun show(message: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
        current = Toast(message, actionLabel, onAction, (current?.serial ?: 0) + 1)
    }

    fun dismiss() {
        current = null
    }

    internal data class Toast(val message: String, val actionLabel: String?, val onAction: () -> Unit, val serial: Int)
}

/** Shows the current toast and hides it after a few seconds or when replaced. Place it bottom-center of the viewport. */
@Composable
fun ToastHost(state: ToastState, modifier: Modifier = Modifier) {
    val toast = state.current ?: return
    LaunchedEffect(toast.serial) {
        delay(5000)
        if (state.current?.serial == toast.serial) state.dismiss()
    }
    val c = Stretch.colors
    Row(
        modifier
            .floating(Stretch.shapes.cardShape, Stretch.shadows.pop)
            .height(StretchSizes.row)
            .padding(start = 12.dp, end = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(StretchIcons.Undo, c.inkControl)
        BasicText(toast.message, style = Stretch.type.body.merge(TextStyle(color = c.ink)))
        if (toast.actionLabel != null) {
            Button(
                toast.actionLabel,
                {
                    toast.onAction()
                    state.dismiss()
                },
                kind = ButtonKind.Quiet,
            )
        } else {
            Spacer(Modifier.width(8.dp))
        }
    }
}
