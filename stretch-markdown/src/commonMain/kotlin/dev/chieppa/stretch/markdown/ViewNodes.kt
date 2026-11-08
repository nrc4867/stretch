package dev.chieppa.stretch.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch

/** The width of a quote's bar. */
private val quoteBar = 3.dp

/** The room between a quote's bar and its text. */
private val quoteGap = 10.dp

/** The room above and below a rule. */
private val ruleSpace = 8.dp

/** [nodes] one under another, as a [MarkdownView] shows them. */
@Composable
internal fun ViewNodes(nodes: List<ViewNode>, context: BlockContext, modifier: Modifier = Modifier) {
    val c = Stretch.colors
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(blockGap)) {
        for (node in nodes) {
            when (node) {
                is ViewNode.Block -> BlockView(node.block, context)
                is ViewNode.Inline -> BlockText(node.block, context, bodyStyle, Modifier.fillMaxWidth())
                is ViewNode.Text -> {
                    val style = if (node.heading > 0) headingStyle(node.heading) else bodyStyle
                    val space = if (node.heading > 0) Modifier.padding(top = headingSpace(node.heading)) else Modifier
                    val text = remember(node, context.onLinkClick) { clickableLinks(node.text.text, context.onLinkClick) }
                    BasicText(text, space.fillMaxWidth(), style)
                }
                is ViewNode.Details -> DetailsView(node, context)
                is ViewNode.Items -> ItemsView(node, context)
                is ViewNode.Quote -> Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    Box(Modifier.width(quoteBar).fillMaxHeight().background(c.lineControl))
                    ViewNodes(node.children, context, Modifier.padding(start = quoteGap))
                }
                is ViewNode.Pre -> {
                    val shape = Stretch.shapes.ctlShape
                    Box(Modifier.fillMaxWidth().clip(shape).background(c.surfaceFloat).border(Stretch.stroke, c.lineControl, shape).padding(boxPadding)) {
                        BasicText(node.code, style = codeStyle)
                    }
                }
                ViewNode.Rule -> Box(Modifier.fillMaxWidth().padding(vertical = ruleSpace).height(Stretch.stroke).background(c.linePanel))
                is ViewNode.Image -> PictureView(node.picture, context.images, context.onLinkClick)
                is ViewNode.Table -> HtmlTableView(node, context)
            }
        }
    }
}
