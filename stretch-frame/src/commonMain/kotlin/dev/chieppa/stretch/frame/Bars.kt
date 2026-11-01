package dev.chieppa.stretch.frame

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableIntStateOf
import dev.chieppa.stretch.frame.generated.more_actions
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.frame.generated.Res
import dev.chieppa.stretch.frame.generated.commands
import dev.chieppa.stretch.frame.generated.document_edited
import dev.chieppa.stretch.frame.generated.redo
import dev.chieppa.stretch.frame.generated.settings
import dev.chieppa.stretch.frame.generated.undo
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.theme.StretchSizes
import dev.chieppa.stretch.ui.IconButton
import dev.chieppa.stretch.ui.Tooltip
import org.jetbrains.compose.resources.stringResource

/**
 * The 44px bar across the top of the window: app name, document name and state, the mode switch, then window-level
 * actions. Nothing else goes here. [edited] puts a dot after the document name while it has unsaved changes. When
 * the window narrows, the document name shortens first, then actions from [TopBarActions] fold into a More menu; the
 * app name, the mode switch and that menu always stay.
 */
@Composable
fun TopBar(
    appName: String,
    document: String,
    modifier: Modifier = Modifier,
    edited: Boolean = false,
    modes: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) = TopBarLayout(appName, document, modifier, edited = edited, modes = modes, actions = actions)

/**
 * The [TopBar] with room kept at its [start] and [end] for buttons the OS draws over it, and [barModifier] on the bar
 * itself (not the line under it).
 */
@Composable
internal fun TopBarLayout(
    appName: String,
    document: String,
    modifier: Modifier = Modifier,
    barModifier: Modifier = Modifier,
    start: Dp = 0.dp,
    end: Dp = 0.dp,
    edited: Boolean = false,
    modes: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val c = Stretch.colors
    Column(modifier.fillMaxWidth()) {
        Layout(
            contents = listOf(
                { BasicText(appName, style = Stretch.type.heading.merge(TextStyle(color = c.inkStrong)), maxLines = 1) },
                { DocumentName(document, edited) },
                modes,
                { Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) { actions() } },
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(StretchSizes.header)
                .background(c.surfaceRail)
                .then(barModifier)
                .padding(start = start + 16.dp, end = end + 8.dp),
        ) { (appSlot, documentSlot, modesSlot, actionsSlot), constraints ->
            val gap = 12.dp.roundToPx()
            val width = constraints.maxWidth
            val height = constraints.maxHeight
            fun loose(max: Int) = Constraints(maxWidth = max.coerceAtLeast(0), maxHeight = height)
            val app = appSlot.first().measure(loose(width))
            val modesWanted = modesSlot.sumOf { it.maxIntrinsicWidth(height) }
            val afterApp = width - app.width - gap
            val actions = actionsSlot.first().measure(loose(maxOf(afterApp - modesWanted - gap, StretchSizes.icon.roundToPx())))
            val modes = modesSlot.map { it.measure(loose(afterApp - actions.width - gap)) }
            val modesWidth = modes.sumOf { it.width }
            val documentRoom = afterApp - modesWidth - (if (modesWidth > 0) gap else 0) - actions.width - gap
            val document = documentSlot.first().measure(loose(documentRoom))
            layout(width, height) {
                fun Placeable.centered(x: Int) = placeRelative(x, (height - this.height) / 2)
                var x = 0
                app.centered(x)
                x += app.width + gap
                if (documentRoom >= MIN_DOCUMENT.roundToPx()) {
                    document.centered(x)
                    x += document.width + gap
                }
                modes.forEach { it.centered(x); x += it.width }
                actions.centered(width - actions.width)
            }
        }
        Spacer(Modifier.fillMaxWidth().height(Stretch.stroke).background(c.linePanel))
    }
}

/** The document's name, and a dot after it while it has unsaved changes. */
@Composable
private fun DocumentName(document: String, edited: Boolean) {
    val c = Stretch.colors
    val editedText = stringResource(Res.string.document_edited)
    Row(
        Modifier.semantics(mergeDescendants = true) { if (edited) stateDescription = editedText },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            document,
            Modifier.weight(1f, fill = false),
            style = Stretch.type.body.merge(TextStyle(color = c.inkSubtle)),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (edited) Box(Modifier.size(7.dp).background(c.accent, CircleShape))
    }
}

/** Narrower than this the document name hides rather than showing a sliver. */
private val MIN_DOCUMENT = 48.dp

/**
 * One window-level action for [TopBarActions]: shown as an icon button with a tooltip, or as a menu item once it
 * folds into the More menu. [popup], such as a [Popover], opens from wherever the action is showing.
 */
class BarAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
    val keys: List<String> = emptyList(),
    val popup: @Composable () -> Unit = {},
)

/**
 * The standard top bar actions: undo, redo, the command palette and settings, then the app's [more]. Pass null to
 * leave one out. Put what settings opens, such as a [Popover], in [settingsPopup], so it anchors under the settings
 * button. Actions that do not fit fold, last first, into a More menu at the end.
 */
@Composable
fun TopBarActions(
    onUndo: (() -> Unit)?,
    onRedo: (() -> Unit)?,
    onCommands: (() -> Unit)?,
    onSettings: (() -> Unit)? = null,
    more: List<BarAction> = emptyList(),
    settingsPopup: @Composable () -> Unit = {},
) {
    val undo = stringResource(Res.string.undo)
    val redo = stringResource(Res.string.redo)
    val commands = stringResource(Res.string.commands)
    val settings = stringResource(Res.string.settings)
    val actions = listOfNotNull(
        onUndo?.let { BarAction(StretchIcons.Undo, undo, it, listOf("Ctrl", "Z")) },
        onRedo?.let { BarAction(StretchIcons.Redo, redo, it, listOf("Ctrl", "Shift", "Z")) },
        onCommands?.let { BarAction(StretchIcons.Command, commands, it, listOf("Ctrl", "K")) },
        onSettings?.let { BarAction(StretchIcons.Settings, settings, it, popup = settingsPopup) },
    ) + more
    OverflowActions(actions)
}

/**
 * As many of [actions] as fit as icon buttons; the rest in a More menu after them. Every button, the More button and
 * each action's popup stay composed whatever fits, and layout only chooses what to place, so resizing the window with
 * a popup or the menu open moves it rather than tearing it down. Each popup sits in an anchor placed over its
 * action's button, or over More once the action folds.
 */
@Composable
private fun OverflowActions(actions: List<BarAction>) {
    val moreLabel = stringResource(Res.string.more_actions)
    var shown by remember { mutableIntStateOf(actions.size) }
    Layout(
        contents = listOf(
            {
                actions.forEachIndexed { i, action ->
                    Box(Modifier.hiddenUnless(i < shown)) {
                        Tooltip(action.label, action.keys.ifEmpty { null }) { IconButton(action.icon, action.label, action.onClick) }
                    }
                }
            },
            { Box(Modifier.hiddenUnless(shown < actions.size)) { MoreMenu(moreLabel, actions.drop(shown)) } },
            { actions.forEach { action -> Box(Modifier.size(StretchSizes.icon)) { action.popup() } } },
        ),
    ) { (buttonSlots, moreSlot, anchorSlots), constraints ->
        val slot = StretchSizes.icon.roundToPx()
        val gap = 2.dp.roundToPx()
        fun widthOf(buttons: Int) = (buttons * (slot + gap) - gap).coerceAtLeast(0)
        val fit = if (widthOf(actions.size) <= constraints.maxWidth) {
            actions.size
        } else {
            (actions.size - 1 downTo 0).first { widthOf(it + 1) <= constraints.maxWidth || it == 0 }
        }
        if (fit != shown) shown = fit
        val loose = Constraints(maxHeight = constraints.maxHeight)
        val buttons = buttonSlots.map { it.measure(loose) }
        val more = moreSlot.first().measure(loose)
        val anchors = anchorSlots.map { it.measure(loose) }
        val folds = fit < actions.size
        layout(widthOf(fit + if (folds) 1 else 0), constraints.maxHeight) {
            fun Placeable.at(index: Int) = place(index * (slot + gap), (constraints.maxHeight - height) / 2)
            buttons.take(fit).forEachIndexed { i, button -> button.at(i) }
            if (folds) more.at(fit)
            anchors.forEachIndexed { i, anchor -> anchor.at(minOf(i, fit)) }
        }
    }
}

/** Keeps an unplaced button out of screen readers and tests; unplaced nodes already get no focus. */
private fun Modifier.hiddenUnless(visible: Boolean): Modifier = if (visible) this else clearAndSetSemantics {}

/** The More button and its menu of folded actions. */
@Composable
private fun MoreMenu(label: String, actions: List<BarAction>) {
    var open by remember { mutableStateOf(false) }
    Box {
        Tooltip(label) { IconButton(StretchIcons.More, label, { open = true }) }
        Menu(open && actions.isNotEmpty(), { open = false }) {
            actions.forEach { action ->
                MenuItem(action.label, { open = false; action.onClick() }, icon = action.icon, keys = action.keys.joinToString(" ").ifEmpty { null })
            }
        }
    }
}

/** The floating bar at the top-left of the viewport: tools, then the armed tool's options. */
@Composable
fun Toolbar(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val shape = Stretch.shapes.cardShape
    Row(
        modifier
            .floating(shape, Stretch.shadows.pop)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** A divider between groups in a [Toolbar]. */
@Composable
fun ToolbarSeparator() {
    Box(Modifier.padding(horizontal = 4.dp).width(Stretch.stroke).height(22.dp).background(Stretch.colors.linePanel))
}

/** A small readout over the viewport: angle, size, zoom. */
@Composable
fun Hud(text: String, modifier: Modifier = Modifier) {
    val c = Stretch.colors
    Box(
        modifier.floating(Stretch.shapes.pillShape, shadow = null).padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        BasicText(text, style = Stretch.type.value.merge(TextStyle(color = c.inkMuted)))
    }
}
