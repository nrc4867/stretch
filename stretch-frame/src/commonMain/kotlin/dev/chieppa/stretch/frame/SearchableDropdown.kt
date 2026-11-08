package dev.chieppa.stretch.frame

import dev.chieppa.stretch.ui.controlPointer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.ui.StretchPopup
import dev.chieppa.stretch.ui.TransientPopup
import dev.chieppa.stretch.frame.generated.Res
import dev.chieppa.stretch.frame.generated.no_matches
import dev.chieppa.stretch.frame.generated.search
import dev.chieppa.stretch.theme.Icon
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.theme.StretchSizes
import dev.chieppa.stretch.ui.Divider
import dev.chieppa.stretch.ui.Scrollbar
import dev.chieppa.stretch.ui.TextField
import org.jetbrains.compose.resources.stringResource

private val panelMinWidth = 200.dp
private val listMaxHeight = 280.dp

/**
 * A [Dropdown] for long lists: opening it puts a search field over the options. Up and Down move, Enter picks, Esc
 * closes. Matches rank as in the command palette. It only picks from [options]; text that matches none picks nothing.
 */
@Composable
fun <T> SearchableDropdown(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val c = Stretch.colors
    var open by remember { mutableStateOf(false) }
    var anchorWidth by remember { mutableIntStateOf(0) }
    val shape = Stretch.shapes.ctlShape
    Box(modifier.onSizeChanged { anchorWidth = it.width }) {
        Row(
            Modifier
                .height(StretchSizes.ctl)
                .clip(shape)
                .background(c.surfaceFloat)
                .border(Stretch.stroke, c.lineControl, shape)
                .controlPointer().clickable(role = Role.DropdownList) { open = true }
                .semantics { this.contentDescription = contentDescription }
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(label(selected), Modifier.weight(1f), style = Stretch.type.body.merge(TextStyle(color = c.inkStrong)), maxLines = 1)
            Icon(StretchIcons.ChevronDown, c.inkMuted)
        }
        if (open) {
            val width = with(LocalDensity.current) { anchorWidth.toDp() }.coerceAtLeast(panelMinWidth)
            SearchPanel(options, selected, label, width, { open = false }) {
                onSelect(it)
                open = false
            }
        }
    }
}

@Composable
private fun <T> SearchPanel(options: List<T>, selected: T, label: (T) -> String, width: Dp, onDismiss: () -> Unit, onPick: (T) -> Unit) {
    val c = Stretch.colors
    var query by remember { mutableStateOf("") }
    var index by remember { mutableIntStateOf(options.indexOf(selected).coerceAtLeast(0)) }
    val matches = searchLabels(query, options, label)
    val list = rememberLazyListState(index)
    val focus = remember { FocusRequester() }
    LaunchedEffect(index) {
        val visible = list.layoutInfo.visibleItemsInfo.map { it.index }
        if (index !in visible.dropLast(1)) list.scrollToItem(index)
    }
    StretchPopup(popupPositionProvider = BelowAnchor(4), onDismissRequest = onDismiss, properties = TransientPopup, shadow = Stretch.shadows.pop) {
        LaunchedEffect(Unit) { focus.requestFocus() }
        Column(
            Modifier
                .width(width)
                .floating(Stretch.shapes.cardShape, Stretch.shadows.pop)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionDown -> index = (index + 1).coerceAtMost(matches.lastIndex.coerceAtLeast(0))
                        Key.DirectionUp -> index = (index - 1).coerceAtLeast(0)
                        Key.Enter -> matches.getOrNull(index)?.let(onPick)
                        Key.Escape -> onDismiss()
                        else -> return@onPreviewKeyEvent false
                    }
                    true
                },
        ) {
            val placeholder = stringResource(Res.string.search)
            TextField(
                query,
                {
                    query = it
                    index = 0
                },
                Modifier.fillMaxWidth().padding(6.dp).focusRequester(focus),
                placeholder = placeholder,
                icon = StretchIcons.Search,
            )
            Divider()
            Box {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = listMaxHeight).padding(4.dp), state = list) {
                    itemsIndexed(matches) { i, option ->
                        MenuItem(label(option), { onPick(option) }, checked = option == selected, highlighted = i == index)
                    }
                }
                Scrollbar(list, Modifier.matchParentSize().wrapContentWidth(Alignment.End))
            }
            if (matches.isEmpty()) {
                BasicText(stringResource(Res.string.no_matches), Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp), style = Stretch.type.body.merge(TextStyle(color = c.inkSubtle)))
            }
        }
    }
}
