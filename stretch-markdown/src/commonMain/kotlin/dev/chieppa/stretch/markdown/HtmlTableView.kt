package dev.chieppa.stretch.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch

/** The room inside a cell. */
private val cellPadding = 6.dp

/** An HTML `<table>`: columns share the width evenly, long cells wrap, and header rows are bold on a fill. */
@Composable
internal fun HtmlTableView(table: ViewNode.Table, context: BlockContext) {
    val c = Stretch.colors
    val shape = Stretch.shapes.ctlShape
    val columns = table.rows.maxOfOrNull { it.size } ?: return
    if (columns == 0) return
    Column(Modifier.fillMaxWidth().clip(shape).border(Stretch.stroke, c.lineControl, shape)) {
        table.rows.forEachIndexed { index, row ->
            val header = index < table.headerRows
            val style = if (header) bodyStyle.merge(TextStyle(fontWeight = FontWeight.SemiBold)) else bodyStyle
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).then(if (header) Modifier.background(c.surfaceControl) else Modifier)) {
                for (column in 0 until columns) {
                    val cell = row.getOrNull(column)
                    val text = cell?.let { clickableLinks(it.text, context.onLinkClick) }
                    Column(Modifier.weight(1f).fillMaxHeight().border(Stretch.stroke / 2, c.linePanel).padding(cellPadding)) {
                        if (text != null) BasicText(text, style = style)
                        cell?.pictures?.forEach { PictureView(it, context.images, context.onLinkClick) }
                    }
                }
            }
        }
    }
}
