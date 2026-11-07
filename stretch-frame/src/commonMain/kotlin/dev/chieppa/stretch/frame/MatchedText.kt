package dev.chieppa.stretch.frame

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch

private val UNDERLINE_GAP = 3.dp
private val UNDERLINE_WIDTH = 1.dp

/**
 * One line of [text] in [style] and [color], with the parts that match a search, [matched], in the strong ink and
 * underlined in the accent, as a palette marks what the query found. [matchRanges] gives the parts for a query.
 */
@Composable
fun MatchedText(text: String, matched: List<IntRange>, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    val c = Stretch.colors
    val ranges = matched.map { it.first.coerceIn(0, text.length)..it.last.coerceIn(-1, text.length - 1) }.filter { !it.isEmpty() }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val annotated = buildAnnotatedString {
        var at = 0
        for (range in ranges.sortedBy { it.first }) {
            if (range.first < at) continue
            append(text.substring(at, range.first))
            withStyle(SpanStyle(color = c.inkStrong)) { append(text.substring(range.first, range.last + 1)) }
            at = range.last + 1
        }
        append(text.substring(at))
    }
    BasicText(
        annotated,
        modifier.drawBehind {
            val result = layout ?: return@drawBehind
            val y = result.getLineBaseline(0) + UNDERLINE_GAP.toPx()
            for (range in ranges) {
                if (range.last >= result.layoutInput.text.length) continue
                val from = result.getHorizontalPosition(range.first, usePrimaryDirection = true)
                val to = result.getHorizontalPosition(range.last + 1, usePrimaryDirection = true)
                drawLine(c.accent, Offset(from, y), Offset(to, y), UNDERLINE_WIDTH.toPx())
            }
        },
        style = style.merge(TextStyle(color = color)),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = { layout = it },
    )
}
