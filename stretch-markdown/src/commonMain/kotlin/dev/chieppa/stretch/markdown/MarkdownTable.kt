package dev.chieppa.stretch.markdown

enum class ColumnAlign { Start, Center, End }

/** A table read out of its source. Rows hold inline markdown; the first row is the header. */
data class MarkdownTable(val rows: List<List<String>>, val aligns: List<ColumnAlign>) {
    val columns: Int get() = aligns.size

    fun withCell(row: Int, column: Int, text: String): MarkdownTable =
        copy(rows = rows.mapIndexed { r, cells -> if (r == row) cells.mapIndexed { c, cell -> if (c == column) text else cell } else cells })

    fun withRow(after: Int): MarkdownTable = copy(rows = rows.take(after + 1) + listOf(List(columns) { "" }) + rows.drop(after + 1))

    fun withoutRow(row: Int): MarkdownTable = if (row == 0 || rows.size <= 2) this else copy(rows = rows.filterIndexed { r, _ -> r != row })

    fun withColumn(after: Int): MarkdownTable = copy(
        rows = rows.map { it.take(after + 1) + "" + it.drop(after + 1) },
        aligns = aligns.take(after + 1) + ColumnAlign.Start + aligns.drop(after + 1),
    )

    fun withoutColumn(column: Int): MarkdownTable = if (columns <= 1) this else copy(
        rows = rows.map { cells -> cells.filterIndexed { c, _ -> c != column } },
        aligns = aligns.filterIndexed { c, _ -> c != column },
    )

    /** The table as markdown, padded so its columns line up in the file too. */
    fun toSource(): String {
        val widths = List(columns) { c -> maxOf(MIN_WIDTH, rows.maxOf { it[c].length }) }
        val line = { cells: List<String> -> cells.mapIndexed { c, cell -> cell.padEnd(widths[c]) }.joinToString(" | ", "| ", " |") }
        val delimiter = aligns.mapIndexed { c, align ->
            when (align) {
                ColumnAlign.Start -> "-".repeat(widths[c])
                ColumnAlign.Center -> ":" + "-".repeat(widths[c] - 2) + ":"
                ColumnAlign.End -> "-".repeat(widths[c] - 1) + ":"
            }
        }.joinToString(" | ", "| ", " |")
        return (listOf(line(rows[0]), delimiter) + rows.drop(1).map(line)).joinToString("\n")
    }

    private companion object {
        const val MIN_WIDTH = 3
    }
}

/** Splits a table line at its unescaped pipes outside code spans. */
private fun cellsOf(line: String): List<String> {
    val cells = ArrayList<String>()
    val cell = StringBuilder()
    var inCode = false
    var at = 0
    val text = line.trim().removePrefix("|")
    while (at < text.length) {
        val char = text[at]
        when {
            char == '\\' && at + 1 < text.length -> {
                cell.append(char).append(text[at + 1])
                at++
            }
            char == '`' -> {
                inCode = !inCode
                cell.append(char)
            }
            char == '|' && !inCode -> {
                cells += cell.toString().trim()
                cell.clear()
            }
            else -> cell.append(char)
        }
        at++
    }
    if (cell.isNotBlank()) cells += cell.toString().trim()
    return cells
}

/** The table in [source]. Rows are padded or cut to the header's width. */
fun parseTable(source: String): MarkdownTable {
    val lines = source.split('\n')
    val header = cellsOf(lines[0])
    val aligns = cellsOf(lines.getOrElse(1) { "" }).map {
        when {
            it.startsWith(":") && it.endsWith(":") -> ColumnAlign.Center
            it.endsWith(":") -> ColumnAlign.End
            else -> ColumnAlign.Start
        }
    }.let { found -> List(header.size) { found.getOrElse(it) { ColumnAlign.Start } } }
    val body = lines.drop(2).map { line -> cellsOf(line).let { cells -> List(header.size) { cells.getOrElse(it) { "" } } } }
    return MarkdownTable(listOf(header) + body, aligns)
}
