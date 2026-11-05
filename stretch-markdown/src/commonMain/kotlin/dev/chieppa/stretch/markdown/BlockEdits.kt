package dev.chieppa.stretch.markdown

/**
 * The separator to keep between [before] and [after]. Only list items sit on adjacent lines; anything else needs a blank
 * line to stay two blocks, unless the file already had them closer and neither has changed kind.
 */
internal fun fittingSeparator(before: MarkdownBlock, after: MarkdownBlock): String = when {
    before.separator.count { it == '\n' } >= 2 -> before.separator
    before.kind is BlockKind.ListItem && after.kind is BlockKind.ListItem -> before.separator
    else -> MarkdownBlock.DEFAULT_SEPARATOR
}

private fun List<MarkdownBlock>.withFittingSeparators(from: Int, to: Int): List<MarkdownBlock> {
    val fitted = toMutableList()
    for (index in from.coerceAtLeast(0) until to.coerceAtMost(lastIndex)) {
        fitted[index] = fitted[index].copy(separator = fittingSeparator(fitted[index], fitted[index + 1]))
    }
    return fitted
}

/** Puts [replacement] in place of the block at [index], keeping what surrounds it. */
private fun MarkdownDocument.replacing(index: Int, replacement: List<MarkdownBlock>, caret: Caret?): MarkdownDocument {
    val blocks = this.blocks.take(index) + replacement + this.blocks.drop(index + 1)
    return with(blocks.withFittingSeparators(index - 1, index + replacement.size), caret)
}

/**
 * Sets [id]'s source to [source]. Text that reads as several blocks, as a paste can, becomes several; an unclosed fence
 * gets its closing fence, with the caret left on the opening line.
 */
internal fun MarkdownDocument.editBlock(id: Long, source: String, start: Int, end: Int = start): MarkdownDocument {
    val index = indexOf(id)
    if (index < 0) return this
    val block = blocks[index]
    val kind = classifyBlock(source)
    if (kind is BlockKind.Code && !kind.closed && '\n' !in source) {
        val edited = block.copy(source = source + "\n" + kind.fence)
        return replacing(index, listOf(edited), Caret(id, start, end))
    }
    val parsed = parseBlocks(source)
    val single = parsed.blocks.size <= 1 && parsed.leading.isEmpty() && parsed.blocks.firstOrNull()?.separator.isNullOrEmpty()
    if (single || kind is BlockKind.Code) return replacing(index, listOf(block.copy(source = source)), Caret(id, start, end))
    val pieces = parsed.blocks.mapIndexed { at, piece ->
        val keepId = if (at == 0) id else piece.id
        val last = at == parsed.blocks.lastIndex
        MarkdownBlock(keepId, piece.source, if (last) block.separator else piece.separator.ifEmpty { MarkdownBlock.DEFAULT_SEPARATOR })
    }
    // The caret follows the end of what was typed or pasted: count back from the end of the block.
    val fromEnd = source.length - end
    val lastPiece = pieces.last()
    val offset = (lastPiece.source.length - fromEnd).coerceIn(0, lastPiece.source.length)
    return replacing(index, pieces, Caret(lastPiece.id, offset))
}

/** What pressing Enter at [at] in [id] does. List items and quotes continue, and an empty one ends; everything else starts a paragraph. */
internal fun MarkdownDocument.splitBlock(id: Long, at: Int): MarkdownDocument {
    val index = indexOf(id)
    if (index < 0) return this
    val block = blocks[index]
    val kind = block.kind
    val source = block.source
    val offset = at.coerceIn(0, source.length)
    when (kind) {
        is BlockKind.ListItem -> if (source.length <= kind.markerEnd) return endList(index, kind)
        BlockKind.Quote, is BlockKind.Callout -> {
            val lineStart = source.lastIndexOf('\n', offset - 1) + 1
            val prefix = quoteLine.find(source.substring(lineStart))?.value ?: "> "
            val line = source.substring(lineStart).substringBefore('\n')
            if (line.trimEnd() == prefix.trimEnd() && offset >= lineStart + line.length) return endQuote(index, lineStart)
            val inserted = "\n" + prefix.ifBlank { "> " }
            return editBlock(id, source.replaceRange(offset, offset, inserted), offset + inserted.length)
        }
        else -> Unit
    }
    val head = source.substring(0, offset)
    val tail = source.substring(offset)
    val prefix = when (kind) {
        is BlockKind.ListItem -> nextMarker(source, kind)
        else -> ""
    }
    val newSource = prefix + if (kind is BlockKind.Heading && kind.setext) tail.substringBefore('\n') else tail
    val newBlock = MarkdownBlock(BlockIds.next(), newSource, block.separator)
    val first = block.copy(source = head, separator = if (kind is BlockKind.ListItem) MarkdownBlock.LIST_SEPARATOR else MarkdownBlock.DEFAULT_SEPARATOR)
    val blocks = this.blocks.take(index) + first + newBlock + this.blocks.drop(index + 1)
    return with(renumbered(blocks, index), Caret(newBlock.id, prefix.length))
}

private fun nextMarker(source: String, kind: BlockKind.ListItem): String {
    val marker = source.substring(0, kind.markerEnd)
    return when (kind.style) {
        ListStyle.Bullet -> marker
        ListStyle.Ordered -> marker.replaceFirst(kind.number.toString(), (kind.number + 1).toString())
        ListStyle.Task -> marker.replace("[x]", "[ ]").replace("[X]", "[ ]")
    }
}

/** Numbers the ordered items following [from] on from it, at its depth, until the list ends. */
private fun renumbered(blocks: List<MarkdownBlock>, from: Int): List<MarkdownBlock> {
    val list = blocks.toMutableList()
    val first = list[from].kind as? BlockKind.ListItem ?: return list
    if (first.style != ListStyle.Ordered) return list
    var number = first.number
    for (index in from + 1 until list.size) {
        val kind = list[index].kind as? BlockKind.ListItem ?: break
        if (kind.indent < first.indent) break
        if (kind.indent > first.indent) continue
        if (kind.style != ListStyle.Ordered) break
        number++
        if (kind.number != number) {
            list[index] = list[index].copy(source = list[index].source.replaceFirst(kind.number.toString(), number.toString()))
        }
    }
    return list
}

/** An empty nested item moves out a level; an empty top-level item becomes an empty paragraph. */
private fun MarkdownDocument.endList(index: Int, kind: BlockKind.ListItem): MarkdownDocument {
    val block = blocks[index]
    if (kind.indent > 0) return indentBlock(block.id, -1)
    return replacing(index, listOf(block.copy(source = "")), Caret(block.id, 0))
}

private fun MarkdownDocument.endQuote(index: Int, lineStart: Int): MarkdownDocument {
    val block = blocks[index]
    if (lineStart == 0) return replacing(index, listOf(block.copy(source = "")), Caret(block.id, 0))
    val kept = block.copy(source = block.source.substring(0, lineStart - 1), separator = MarkdownBlock.DEFAULT_SEPARATOR)
    val paragraph = MarkdownBlock(BlockIds.next(), "", block.separator)
    return with(blocks.take(index) + kept + paragraph + blocks.drop(index + 1), Caret(paragraph.id, 0))
}

/** Backspace at the very start of [id]: joins it to the block before, when that holds text. */
internal fun MarkdownDocument.mergeWithPrevious(id: Long): MarkdownDocument {
    val index = indexOf(id)
    if (index <= 0) return this
    val block = blocks[index]
    val previous = blocks[index - 1]
    val joinable = previous.kind.holdsPlainText()
    if (!joinable) {
        // A divider, table or image above stays; an empty block just goes away.
        return if (block.source.isEmpty()) {
            with(blocks.filterIndexed { at, _ -> at != index }.mapIndexed { at, it -> if (at == index - 1) it.copy(separator = block.separator) else it }, Caret(previous.id, previous.source.length))
        } else {
            with(caret = Caret(previous.id, previous.source.length))
        }
    }
    val text = block.source.substring(textStart(block.source, block.kind))
    val joined = previous.copy(source = previous.source + text, separator = block.separator)
    return with(blocks.take(index - 1) + joined + blocks.drop(index + 1), Caret(previous.id, previous.source.length))
}

internal fun BlockKind.holdsPlainText() = when (this) {
    BlockKind.Paragraph, is BlockKind.Heading, is BlockKind.ListItem, BlockKind.Quote, is BlockKind.Callout -> true
    else -> false
}

/** Backspace at the start of a block's visible text: it stops being a heading, a list item or a quote and keeps its text. */
internal fun MarkdownDocument.stripMarker(id: Long): MarkdownDocument {
    val index = indexOf(id)
    if (index < 0) return this
    val block = blocks[index]
    return replacing(index, listOf(block.copy(source = plainTextOf(block))), Caret(id, 0))
}

/** [block]'s text without its markers, for turning it into something else. */
internal fun plainTextOf(block: MarkdownBlock): String = when (val kind = block.kind) {
    is BlockKind.Code -> codeBody(block.source, kind)
    BlockKind.Quote, is BlockKind.Callout -> {
        val lines = block.source.split('\n').drop(if (kind is BlockKind.Callout) 1 else 0)
        lines.joinToString("\n") { line -> line.removePrefix(quoteLine.find(line)?.value.orEmpty()) }
    }
    is BlockKind.Heading -> if (kind.setext) block.source.substringBefore('\n') else block.source.substring(textStart(block.source, kind))
    is BlockKind.ListItem -> block.source.substring(kind.markerEnd)
    else -> block.source
}

/** Tab or Shift Tab on a list item: nests it [levels] deeper, in steps of its marker's width. */
internal fun MarkdownDocument.indentBlock(id: Long, levels: Int): MarkdownDocument {
    val index = indexOf(id)
    if (index < 0) return this
    val block = blocks[index]
    val kind = block.kind as? BlockKind.ListItem ?: return this
    val step = (kind.markerEnd - kind.indent).coerceAtLeast(2).let { if (kind.style == ListStyle.Task) it - 4 else it }.coerceAtLeast(2)
    val indent = (kind.indent + levels * step).coerceAtLeast(0)
    val source = " ".repeat(indent) + block.source.trimStart()
    val delta = source.length - block.source.length
    val caret = caret?.takeIf { it.block == id }
    return replacing(index, listOf(block.copy(source = source)), Caret(id, (caret?.start ?: source.length) + delta, (caret?.end ?: source.length) + delta))
}

/** Turns [id] into a block of [type], keeping its text. A divider or an image goes after a block with text in it. */
internal fun MarkdownDocument.convertBlock(id: Long, type: BlockType): MarkdownDocument {
    val index = indexOf(id)
    if (index < 0) return this
    val block = blocks[index]
    val text = if (block.kind.holdsPlainText() || block.kind is BlockKind.Code) plainTextOf(block) else block.source
    if (type == BlockType.Divider && text.isNotEmpty()) {
        val divider = MarkdownBlock(BlockIds.next(), sourceOf(type, ""), block.separator)
        val blocks = blocks.take(index) + block.copy(separator = MarkdownBlock.DEFAULT_SEPARATOR) + divider + blocks.drop(index + 1)
        return with(blocks, Caret(divider.id, divider.source.length))
    }
    val source = sourceOf(type, text)
    val edited = block.copy(source = source)
    return replacing(index, listOf(edited), Caret(id, textStart(source, edited.kind) + if (edited.kind.holdsPlainText() || edited.kind is BlockKind.Code) text.length else 0))
}

/** Removes the blocks [from] to [to]. A document always keeps one block. */
internal fun MarkdownDocument.removeBlocks(from: Int, to: Int): MarkdownDocument {
    val kept = blocks.filterIndexed { index, _ -> index !in from..to }
    if (kept.isEmpty()) {
        val blank = MarkdownBlock(BlockIds.next(), "", "")
        return with(listOf(blank), Caret(blank.id, 0))
    }
    val after = kept.getOrNull(from) ?: kept.last()
    val adjusted = if (to == blocks.lastIndex && from > 0) {
        kept.mapIndexed { index, block -> if (index == kept.lastIndex) block.copy(separator = blocks.last().separator) else block }
    } else {
        kept
    }
    return with(adjusted, Caret(after.id, 0))
}

/** The markdown of the blocks [from] to [to], for the clipboard. */
internal fun MarkdownDocument.sourceOfRange(from: Int, to: Int): String =
    blocks.subList(from, to + 1).let { range -> range.dropLast(1).joinToString("") { it.source + it.separator } + range.last().source }

/** Sets or clears a task item's check box. */
internal fun MarkdownDocument.setChecked(id: Long, checked: Boolean): MarkdownDocument {
    val index = indexOf(id)
    if (index < 0) return this
    val block = blocks[index]
    val mark = if (checked) "[x]" else "[ ]"
    return with(blocks.take(index) + block.copy(source = block.source.replaceFirst(Regex("""\[[ xX]\]"""), mark)) + blocks.drop(index + 1))
}
