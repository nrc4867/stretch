package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.frame.generated.Res
import dev.chieppa.stretch.frame.generated.more_actions
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.theme.StretchSizes
import dev.chieppa.stretch.ui.BadgedBox
import dev.chieppa.stretch.ui.IconButton
import dev.chieppa.stretch.ui.Tooltip
import dev.chieppa.stretch.ui.opensOnPress
import org.jetbrains.compose.resources.stringResource

/**
 * As many of [actions] as fit as icon buttons; the rest in a More menu after them. Every button, the More button and
 * each action's popup stay composed whatever fits, and layout only chooses what to place, so resizing the window with
 * a popup or the menu open moves it rather than tearing it down. Each popup sits in an anchor placed over its
 * action's button, or over More once the action folds.
 */
@Composable
internal fun OverflowActions(actions: List<BarAction>) {
    val moreLabel = stringResource(Res.string.more_actions)
    var shown by remember { mutableIntStateOf(actions.size) }
    Layout(
        contents = listOf(
            {
                actions.forEachIndexed { i, action ->
                    Box(Modifier.hiddenUnless(i < shown)) {
                        val press = action.popupOpen?.takeIf { action.enabled }?.let { Modifier.opensOnPress(it, action.onClick) } ?: Modifier
                        Tooltip(action.label, action.keys.ifEmpty { null }) {
                            BadgedBox(action.badge, action.badge.orEmpty()) { IconButton(action.icon, action.label, action.onClick, press, enabled = action.enabled) }
                        }
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

/** The More button and its menu of folded actions. */
@Composable
private fun MoreMenu(label: String, actions: List<BarAction>) {
    var open by remember { mutableStateOf(false) }
    Box {
        Tooltip(label) { IconButton(StretchIcons.More, label, { open = true }, Modifier.opensOnPress(open) { open = true }) }
        Menu(open && actions.isNotEmpty(), { open = false }) {
            actions.forEach { action ->
                MenuItem(action.label, { open = false; action.onClick() }, icon = action.icon, keys = action.keys.joinToString(" ").ifEmpty { null }, enabled = action.enabled)
            }
        }
    }
}
