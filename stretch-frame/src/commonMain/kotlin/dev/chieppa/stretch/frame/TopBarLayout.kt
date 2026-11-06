package dev.chieppa.stretch.frame

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchSizes
import dev.chieppa.stretch.theme.StretchSpacing
import dev.chieppa.stretch.ui.Divider

/** Room between the bar's groups. */
private val GAP = StretchSpacing.space12

/** Room before the app name and after the actions, inside any room kept for the OS's buttons. */
private val PAD_START = StretchSpacing.space16
private val PAD_END = StretchSpacing.space8

/** Narrower than this the document name hides rather than showing a sliver. */
private val MIN_DOCUMENT = 48.dp

/** The centre item narrows down to this, or to its own width if less, before it hides. */
private val MIN_CENTER = 160.dp

/**
 * The [TopBar] with room kept at its [start] and [end] for buttons the OS draws over it, and [barModifier] on the bar
 * itself (not the line under it). [onCenterShown] hears whether the centre item is showing each time that changes.
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
    menus: @Composable () -> Unit = {},
    modes: @Composable () -> Unit = {},
    leading: @Composable () -> Unit = {},
    center: @Composable () -> Unit = {},
    onCenterShown: (Boolean) -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val c = Stretch.colors
    var centerShown by remember { mutableStateOf(true) }
    var documentShown by remember { mutableStateOf(true) }
    val reportCenter by rememberUpdatedState(onCenterShown)
    Column(modifier.fillMaxWidth()) {
        Layout(
            contents = listOf(
                { BasicText(appName, style = Stretch.type.heading.merge(TextStyle(color = c.inkStrong)), maxLines = 1) },
                menus,
                leading,
                { Box(Modifier.hiddenUnless(documentShown)) { DocumentName(document, edited) } },
                modes,
                { Box(Modifier.hiddenUnless(centerShown)) { center() } },
                { Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) { actions() } },
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(StretchSizes.header)
                .background(c.surfaceRail)
                .then(barModifier)
                .padding(start = start + PAD_START, end = end + PAD_END),
        ) { slots, constraints ->
            val (appSlot, menusSlot, leadingSlot, documentSlot, modesSlot) = slots
            val centerSlot = slots[CENTER]
            val actionsSlot = slots[ACTIONS]
            val gap = GAP.roundToPx()
            val width = constraints.maxWidth
            val height = constraints.maxHeight
            fun loose(max: Int) = Constraints(maxWidth = max.coerceAtLeast(0), maxHeight = height)
            fun spaced(groupWidth: Int) = if (groupWidth > 0) groupWidth + gap else 0
            val app = appSlot.first().measure(loose(width))
            val menus = menusSlot.map { it.measure(loose(width - app.width - gap)) }
            val menusWidth = menus.sumOf { it.width }
            val leadings = leadingSlot.map { it.measure(loose(width - app.width - gap - spaced(menusWidth))) }
            val leadingWidth = leadings.sumOf { it.width }
            val modesWanted = modesSlot.sumOf { it.maxIntrinsicWidth(height) }
            val afterApp = width - app.width - gap - spaced(menusWidth) - spaced(leadingWidth)
            val actions = actionsSlot.first().measure(loose(maxOf(afterApp - modesWanted - gap, StretchSizes.icon.roundToPx())))
            val modes = modesSlot.map { it.measure(loose(afterApp - actions.width - gap)) }
            val modesWidth = modes.sumOf { it.width }
            // Where the left group ends without the document name, which gives way first, and where the right starts.
            val leftEnd = width - afterApp + spaced(modesWidth)
            val rightStart = width - actions.width - gap
            val padStart = (start + PAD_START).roundToPx()
            val padEnd = (end + PAD_END).roundToPx()
            val span = centerSpan(
                width = width,
                center = (width + padEnd - padStart) / 2,
                leftEnd = leftEnd,
                rightStart = rightStart,
                wanted = centerSlot.sumOf { it.maxIntrinsicWidth(height) },
                min = MIN_CENTER.roundToPx(),
            )
            val centerItems = if (span != null) centerSlot.map { it.measure(loose(span.width)) } else emptyList()
            val shown = span != null && centerItems.any { it.width > 0 }
            if (shown != centerShown) {
                centerShown = shown
                reportCenter(shown)
            }
            val documentRoom = (if (span != null) span.x - gap else rightStart) - leftEnd
            val document = documentSlot.first().measure(loose(documentRoom))
            val showsDocument = documentRoom >= MIN_DOCUMENT.roundToPx()
            if (showsDocument != documentShown) documentShown = showsDocument
            layout(width, height) {
                fun Placeable.centered(x: Int) = placeRelative(x, (height - this.height) / 2)
                var x = 0
                app.centered(x)
                x += app.width + gap
                for (group in listOf(menus, leadings)) {
                    if (group.sumOf { it.width } > 0) {
                        group.forEach { it.centered(x); x += it.width }
                        x += gap
                    }
                }
                if (showsDocument) {
                    document.centered(x)
                    x += document.width + gap
                }
                modes.forEach { it.centered(x); x += it.width }
                if (span != null) {
                    val itemsWidth = centerItems.sumOf { it.width }
                    var cx = span.x + (span.width - itemsWidth) / 2
                    centerItems.forEach { it.centered(cx); cx += it.width }
                }
                actions.centered(width - actions.width)
            }
        }
        Divider()
    }
}

private const val CENTER = 5
private const val ACTIONS = 6
