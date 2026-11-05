package dev.chieppa.stretch.markdown

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.ui.ErrorMark
import dev.chieppa.stretch.ui.Label
import dev.chieppa.stretch.ui.TextField
import dev.chieppa.stretch.ui.holdsTyping
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/** Waits this long after typing stops before drawing a diagram again. */
private const val DIAGRAM_PAUSE_MILLIS = 300L

private const val DARK_LUMINANCE = 0.5f

/** A code block's language, editable over the code, with an error mark when the block has one. */
@Composable
internal fun CodeLanguageField(block: MarkdownBlock, kind: BlockKind.Code, context: BlockContext, error: String?) {
    val c = Stretch.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) {
            if (kind.language.isEmpty() && !context.readOnly) Label(context.strings.language, Stretch.type.eyebrow, c.inkSubtle)
            BasicTextField(
                value = kind.language,
                onValueChange = { typed ->
                    val language = typed.filterNot { it.isWhitespace() || it == '`' }
                    val body = block.source.substringAfter('\n', "")
                    context.state.replaceSource(block.id, kind.fence + language + "\n" + body)
                },
                modifier = Modifier.holdsTyping().fillMaxWidth(),
                readOnly = context.readOnly,
                singleLine = true,
                textStyle = Stretch.type.eyebrow.merge(TextStyle(color = c.inkMuted)),
                cursorBrush = SolidColor(c.inkStrong),
            )
        }
        if (error != null) ErrorMark(error)
    }
}

/** What drawing a diagram last came to: the latest good picture stays up while its source has an error. */
private class DiagramOutcome(val picture: DiagramPicture?, val error: String?)

@Composable
internal fun DiagramView(block: MarkdownBlock, kind: BlockKind.Code, context: BlockContext) {
    val renderer = context.diagrams ?: return
    val state = context.state
    val colors = Stretch.colors
    val density = LocalDensity.current.density
    val style = remember(colors, density) { DiagramStyle(colors, density, colors.surfacePanel.luminance() < DARK_LUMINANCE) }
    val source = codeBody(block.source, kind)
    val outcome by produceState(DiagramOutcome(null, null), source, style) {
        delay(if (value.picture == null) 0 else DIAGRAM_PAUSE_MILLIS)
        value = try {
            DiagramOutcome(renderer.render(source, style), null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            DiagramOutcome(value.picture, e.message ?: e.toString())
        }
    }
    val editing = !context.readOnly && state.editingBlock == block.id
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // Clicking a picture puts the caret in its code; the code stays in composition so it can take the focus.
        if (editing || outcome.picture == null || outcome.error != null) CodeView(block, kind, context, outcome.error)
        outcome.picture?.let { picture ->
            Image(
                picture.image,
                source,
                Modifier
                    .widthIn(max = picture.width)
                    .fillMaxWidth()
                    .then(if (!context.readOnly) Modifier.clickable { state.focusOn(block.id, codeBodyStart(block.source)) } else Modifier),
            )
        }
    }
}

@Composable
internal fun ImageView(block: MarkdownBlock, kind: BlockKind.Image, context: BlockContext) {
    val state = context.state
    val picture: ImageBitmap? by produceState<ImageBitmap?>(null, kind.url, context.images) {
        value = if (kind.url.isEmpty()) null else runCatching { context.images?.load(kind.url) }.getOrNull()
    }
    val editing = !context.readOnly && state.editingBlock == block.id
    val focusIt = if (context.readOnly) Modifier else Modifier.clickable { state.focusOn(block.id, block.source.length) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val shown = picture
        if (shown != null) {
            Image(shown, kind.alt, focusIt.widthIn(max = (shown.width / LocalDensity.current.density).dp).fillMaxWidth())
        } else {
            Box(focusIt) { MissingPicture(StretchIcons.Image, kind.alt.ifEmpty { kind.url }) }
        }
        when {
            !editing -> if (!context.readOnly) ImageFocusCatcher(block, context)
            state.hidesMarkers -> ImageFields(block, kind, context)
            else -> BlockField(block, context, Stretch.type.valueSm.merge(TextStyle(color = Stretch.colors.ink)), Modifier.fillMaxWidth())
        }
    }
}

/** Keeps a field in the block so arrow keys and clicks can bring the caret in. */
@Composable
private fun ImageFocusCatcher(block: MarkdownBlock, context: BlockContext) {
    BlockField(block, context, Stretch.type.valueSm.merge(TextStyle(color = Stretch.colors.inkSubtle)), Modifier.size(0.dp, 0.dp))
}

/** In rich text an image has fields, not `![]()`. */
@Composable
private fun ImageFields(block: MarkdownBlock, kind: BlockKind.Image, context: BlockContext) {
    var alt by remember(block.id) { mutableStateOf(kind.alt) }
    var url by remember(block.id) { mutableStateOf(kind.url) }
    LaunchedEffect(alt, url) {
        val source = "![$alt]($url)"
        if (source != block.source) context.state.replaceSource(block.id, source)
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextField(alt, { alt = it }, Modifier.weight(1f), placeholder = context.strings.imageDescription)
        TextField(url, { url = it }, Modifier.weight(1f), placeholder = context.strings.imageAddress)
    }
}
