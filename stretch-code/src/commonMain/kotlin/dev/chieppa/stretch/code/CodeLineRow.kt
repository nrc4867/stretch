package dev.chieppa.stretch.code

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.StretchSpacing

/** How wide the bar down a changed line's start edge is: wide enough to read at a glance beside the numbers. */
val ChangeBarWidth: Dp = StretchSpacing.space4

/** A line number, right-aligned in the numbers' column, or a blank column when [value] is null. [strong] inks it darker. */
@Composable
fun CodeLineNumber(value: Int?, metrics: CodeMetrics, look: CodeLook, modifier: Modifier = Modifier, strong: Boolean = false) {
    BasicText(
        value?.toString().orEmpty(),
        modifier.width(metrics.numberWidth).padding(horizontal = StretchSpacing.space6),
        style = look.style.merge(TextStyle(color = if (strong) look.strong else look.muted, textAlign = TextAlign.End)),
        maxLines = 1,
    )
}

/**
 * A line's code: wrapped to the room there is when [wrap], or else as wide as the longest line and scrolled sideways by
 * [scroll], which every line of a view shares so they move together.
 */
@Composable
fun CodeLineBody(text: AnnotatedString, metrics: CodeMetrics, look: CodeLook, wrap: Boolean, scroll: ScrollState, modifier: Modifier = Modifier) {
    if (wrap) {
        BasicText(text, modifier, style = look.style)
    } else {
        Box(modifier.clipToBounds().horizontalScroll(scroll)) {
            BasicText(text, Modifier.width(metrics.codeWidth), style = look.style, softWrap = false, overflow = TextOverflow.Clip)
        }
    }
}

/**
 * One line of a code view: the [gutter] column [gutterWidth] wide for marks, the line number (which [numberModifier]
 * can make clickable) and the code, all on the wash of [tone]. A specified [changeBar] draws a bar of that color down
 * the start edge, as tall as the line however it wraps, for a changed line. Only the code can be selected as text.
 */
@Composable
fun CodeLineRow(
    number: Int?,
    text: AnnotatedString,
    metrics: CodeMetrics,
    look: CodeLook,
    wrap: Boolean,
    scroll: ScrollState,
    modifier: Modifier = Modifier,
    tone: LineTone? = null,
    gutterWidth: Dp = StretchSpacing.space16,
    gutter: (@Composable () -> Unit)? = null,
    numberModifier: Modifier = Modifier,
    changeBar: Color = Color.Unspecified,
) {
    val bar = Modifier.drawBehind { if (changeBar.isSpecified) drawRect(changeBar, size = Size(ChangeBarWidth.toPx(), size.height)) }
    val gutterSpan = if (look.washesGutter(tone)) (if (gutter != null) gutterWidth else 0.dp) + metrics.numberWidth else null
    Row(modifier.fillMaxWidth().heightIn(min = metrics.lineHeight).lineWash(look.wash(tone), gutterSpan).then(bar)) {
        DisableSelection {
            if (gutter != null) Box(Modifier.width(gutterWidth).heightIn(min = metrics.lineHeight), contentAlignment = Alignment.Center) { gutter() }
            CodeLineNumber(number, metrics, look, numberModifier, strong = tone == LineTone.Selected)
        }
        CodeLineBody(text, metrics, look, wrap, scroll, Modifier.weight(1f).padding(start = StretchSpacing.space8))
    }
}
