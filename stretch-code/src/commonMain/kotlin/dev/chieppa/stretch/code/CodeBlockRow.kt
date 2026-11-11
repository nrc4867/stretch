package dev.chieppa.stretch.code

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchSpacing

/** How far the bracket down a block's numbers sits in from their end. */
private val BracketInset = StretchSpacing.space2

/**
 * A run of lines drawn as [content] in place of their code, such as a doc comment rendered: [numbers] draws the first
 * line's gutter and numbers as a line of the same view has them, a bracket runs down beside them to the block's foot,
 * and [content] takes the room the code would. All of it sits on [wash].
 */
@Composable
fun CodeBlockRow(
    metrics: CodeMetrics,
    modifier: Modifier = Modifier,
    wash: Color = Color.Unspecified,
    numbers: @Composable RowScope.() -> Unit,
    content: @Composable () -> Unit,
) {
    val bracket = Stretch.colors.lineStrong
    val stroke = Stretch.stroke
    var numbersWidth by remember { mutableIntStateOf(0) }
    val drawn = Modifier.drawBehind {
        val x = numbersWidth - BracketInset.toPx()
        if (x > 0f) drawLine(bracket, Offset(x, metrics.lineHeight.toPx()), Offset(x, size.height), stroke.toPx())
    }
    Row(modifier.fillMaxWidth().heightIn(min = metrics.lineHeight).lineWash(wash).then(drawn), verticalAlignment = Alignment.Top) {
        DisableSelection {
            Row(Modifier.heightIn(min = metrics.lineHeight).onSizeChanged { numbersWidth = it.width }, content = numbers)
        }
        Row(Modifier.weight(1f).padding(start = StretchSpacing.space8, top = BlockPadding, bottom = BlockPadding)) { content() }
    }
}

/** The room above and below a block's content. */
private val BlockPadding: Dp = 2.dp
