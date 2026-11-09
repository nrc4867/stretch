package dev.chieppa.stretch.frame

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import dev.chieppa.stretch.theme.Icon
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.theme.StretchSizes
import dev.chieppa.stretch.theme.StretchSpacing
import dev.chieppa.stretch.ui.Divider
import dev.chieppa.stretch.ui.StretchPopup
import dev.chieppa.stretch.ui.holdsTyping

/** How tall a palette's results grow before they scroll. */
private val PALETTE_RESULTS_MAX = 420.dp

/**
 * A search palette over the window: a query field after [icon], with [trailing] controls such as a result count or a
 * scope picker, the results [content] under it, scrolling past [maxHeight], and what its keys do, [hints], in a footer.
 * It sits [top] down the window, centred. With [scrim] the window behind it dims and a click there closes it. Every key
 * press goes to [onKey] first, for moving through the results; Esc closes. [queryStyle] sets the field's text, such as
 * code type for a pasted link, and [placeholder] names the field. A [query] the palette opens with, or that is set from
 * outside, puts the cursor after it.
 */
@Composable
fun SearchPalette(
    query: String,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    width: Dp = 520.dp,
    top: Dp = 96.dp,
    scrim: Boolean = false,
    icon: ImageVector = StretchIcons.Search,
    queryStyle: TextStyle? = null,
    maxHeight: Dp = PALETTE_RESULTS_MAX,
    hints: List<PaletteHint> = emptyList(),
    onKey: (KeyEvent) -> Boolean = { false },
    trailing: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Stretch.colors
    val focus = remember { FocusRequester() }
    var field by remember { mutableStateOf(TextFieldValue(query, TextRange(query.length))) }
    if (field.text != query) field = TextFieldValue(query, TextRange(query.length))
    val panel = @Composable {
        LaunchedEffect(Unit) { focus.requestFocus() }
        Column(
            modifier
                .width(width)
                .floating(Stretch.shapes.cardShape, Stretch.shadows.float)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                        onDismiss()
                        true
                    } else {
                        onKey(event)
                    }
                },
        ) {
            Row(
                Modifier.fillMaxWidth().height(StretchSizes.header).padding(horizontal = StretchSpacing.space14),
                horizontalArrangement = Arrangement.spacedBy(StretchSpacing.space8),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, c.inkMuted)
                val fieldStyle = Stretch.type.body.merge(TextStyle(color = c.inkStrong, fontSize = Stretch.type.drawerTitle.fontSize)).merge(queryStyle)
                BasicTextField(
                    value = field,
                    onValueChange = {
                        field = it
                        if (it.text != query) onQueryChange(it.text)
                    },
                    modifier = Modifier.weight(1f).holdsTyping().focusRequester(focus).semantics { contentDescription = placeholder },
                    singleLine = true,
                    textStyle = fieldStyle,
                    cursorBrush = SolidColor(c.inkStrong),
                    decorationBox = { inner ->
                        Box {
                            if (query.isEmpty()) BasicText(placeholder, style = fieldStyle.merge(TextStyle(color = c.inkSubtle)), maxLines = 1)
                            inner()
                        }
                    },
                )
                trailing()
            }
            Divider()
            Column(Modifier.heightIn(max = maxHeight).verticalScroll(rememberScrollState()), content = content)
            if (hints.isNotEmpty()) PaletteHints(hints)
        }
    }
    val topPx = with(LocalDensity.current) { top.roundToPx() }
    if (scrim) {
        StretchPopup(WholeWindow, onDismiss, PopupProperties(focusable = true), null) {
            Box(Modifier.fillMaxSize().background(c.scrim).pointerInput(Unit) { detectTapGestures { onDismiss() } }) {
                Box(Modifier.align(Alignment.TopCenter).padding(top = top).pointerInput(Unit) { detectTapGestures { } }) { panel() }
            }
        }
    } else {
        StretchPopup(TopOfWindow(topPx), onDismiss, PopupProperties(focusable = true), Stretch.shadows.float) { panel() }
    }
}

/** Places a popup over the whole window, for one that dims it. */
private object WholeWindow : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset =
        IntOffset.Zero
}

/** Centres a popup across the window, [top] pixels down, keeping it inside. */
private class TopOfWindow(private val top: Int) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset =
        IntOffset(
            ((windowSize.width - popupContentSize.width) / 2).coerceAtLeast(0),
            top.coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0)),
        )
}
