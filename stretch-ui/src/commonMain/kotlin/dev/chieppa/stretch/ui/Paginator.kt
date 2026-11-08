package dev.chieppa.stretch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Icon
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.theme.StretchSizes


/**
 * The pages a paginator shows, zero-based, with null for a gap: always the first and last, [page] and [siblings] on
 * each side. A gap never hides a single page, since showing the page takes the same room.
 */
fun pageWindow(page: Int, pageCount: Int, siblings: Int = 1): List<Int?> {
    val shown = buildSet {
        add(0)
        add(pageCount - 1)
        for (p in (page - siblings)..(page + siblings)) if (p in 0 until pageCount) add(p)
    }.sorted()
    val result = mutableListOf<Int?>()
    var last = -1
    for (p in shown) {
        when (p - last) {
            1 -> Unit
            2 -> result += p - 1
            else -> result += null
        }
        result += p
        last = p
    }
    return result
}

/** Moves through pages of results: back, the page numbers, forward. [page] is zero-based and shows one higher. */
@Composable
fun Paginator(
    page: Int,
    pageCount: Int,
    onPage: (Int) -> Unit,
    previousDescription: String,
    nextDescription: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        PageArrow(StretchIcons.ChevronLeft, previousDescription, page > 0) { onPage(page - 1) }
        for (entry in pageWindow(page, pageCount)) {
            if (entry == null) {
                Box(Modifier.size(StretchSizes.icon), contentAlignment = Alignment.Center) { Icon(StretchIcons.More, Stretch.colors.inkSubtle) }
            } else {
                PageButton(entry, entry == page) { onPage(entry) }
            }
        }
        PageArrow(StretchIcons.ChevronRight, nextDescription, page < pageCount - 1) { onPage(page + 1) }
    }
}

@Composable
private fun PageArrow(icon: ImageVector, description: String, enabled: Boolean, onClick: () -> Unit) {
    val c = Stretch.colors
    val state = rememberControlState()
    val shape = Stretch.shapes.ctlShape
    Box(
        Modifier
            .focusRing(state, shape)
            .size(StretchSizes.icon)
            .alpha(if (enabled) 1f else disabledAlpha)
            .clip(shape)
            .background(if (enabled) Fills.borderless(state) else Color.Transparent)
            .hoverable(state.source, enabled)
            .controlPointer().clickable(state.source, null, enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, c.inkControl)
    }
}

@Composable
private fun PageButton(page: Int, selected: Boolean, onClick: () -> Unit) {
    val c = Stretch.colors
    val state = rememberControlState()
    val shape = Stretch.shapes.ctlShape
    val fill = if (selected) Fills.on(state) else Fills.borderless(state)
    Box(
        Modifier
            .focusRing(state, shape, fill)
            .height(StretchSizes.icon)
            .widthIn(min = StretchSizes.icon)
            .clip(shape)
            .background(fill)
            .border(Stretch.stroke, if (selected) c.onEdge else Color.Transparent, shape)
            .hoverable(state.source)
            .controlPointer().clickable(state.source, null, role = Role.Button, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Label((page + 1).toString(), Stretch.type.value, if (selected) c.onInk else c.inkControl)
    }
}
