package dev.chieppa.stretch.code

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp

/** How far a wavy underline rises and falls either side of its middle. */
private val WAVE_HEIGHT = 1.5.dp

/** How long one rise or fall of a wavy underline is. */
private val WAVE_STEP = 2.dp

/** How thick an underline is. */
private val UNDERLINE_STROKE = 1.dp

/** How long each dot of a dotted underline is, and the gap after it. */
private val DOT = 1.dp
private val DOT_GAP = 2.dp

/** Draws [underlines] under the text, once [layout] gives where it is; each follows its run across wrapped rows. */
fun Modifier.codeUnderlines(underlines: List<CodeUnderline>, layout: () -> TextLayoutResult?): Modifier =
    if (underlines.isEmpty()) {
        this
    } else {
        drawWithContent {
            drawContent()
            val text = layout() ?: return@drawWithContent
            for (underline in underlines) drawUnderline(text, underline)
        }
    }

private fun DrawScope.drawUnderline(layout: TextLayoutResult, underline: CodeUnderline) {
    val length = layout.layoutInput.text.length
    val start = underline.start.coerceIn(0, length)
    val end = underline.end.coerceIn(0, length)
    if (end <= start) return
    for (row in layout.getLineForOffset(start)..layout.getLineForOffset(end - 1)) {
        val from = maxOf(start, layout.getLineStart(row))
        val until = minOf(end, layout.getLineEnd(row))
        if (until <= from) continue
        val left = layout.getBoundingBox(from).left
        val right = layout.getBoundingBox(until - 1).right
        val middle = layout.getLineBottom(row) - WAVE_HEIGHT.toPx()
        when (underline.shape) {
            UnderlineShape.Wavy -> drawPath(wave(left, right, middle), underline.color, style = Stroke(UNDERLINE_STROKE.toPx()))
            UnderlineShape.Dotted -> drawLine(
                underline.color,
                Offset(left, middle),
                Offset(right, middle),
                UNDERLINE_STROKE.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(DOT.toPx(), DOT_GAP.toPx())),
            )
        }
    }
}

/** A zigzag from [left] to [right] about [middle]. */
private fun DrawScope.wave(left: Float, right: Float, middle: Float): Path {
    val step = WAVE_STEP.toPx()
    val height = WAVE_HEIGHT.toPx()
    return Path().apply {
        moveTo(left, middle)
        var x = left
        var up = true
        while (x < right) {
            x = minOf(x + step, right)
            lineTo(x, if (up) middle - height else middle + height)
            up = !up
        }
    }
}
