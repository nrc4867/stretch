package dev.chieppa.stretch.console

/**
 * A part of a line someone can click: the characters from [start] up to [end]. [target] is what it points to, such as
 * a path or an address; [line] and [column] count from 1 when it names a place in a file.
 */
data class ConsoleLink(val start: Int, val end: Int, val target: String, val line: Int? = null, val column: Int? = null)

/** Finds the links in one line of output. An app passes its own to [Console] to find what its tools print. */
fun interface ConsoleLinkDetector {
    fun find(text: String): List<ConsoleLink>
}

private const val BACKSLASH = '\\'

private val pathLine = Regex("""((?:[A-Za-z]:[/\\])?(?:[\w.~-]+[/\\])*[\w.-]+):(\d+)(?::(\d+))?""")

/**
 * Finds `path:line` and `path:line:column`, as compilers, test runners and linters print them, with or without a drive
 * letter. The name needs a folder or a dot, so times like `12:30` and hosts like `localhost:8080` are left alone.
 */
val PathLineLinks = ConsoleLinkDetector { text ->
    pathLine.findAll(text).mapNotNull { match ->
        val path = match.groupValues[1]
        val name = path.substringAfterLast('/').substringAfterLast(BACKSLASH)
        if ('/' !in path && BACKSLASH !in path && '.' !in name) return@mapNotNull null
        val line = match.groupValues[2].toIntOrNull() ?: return@mapNotNull null
        ConsoleLink(match.range.first, match.range.last + 1, path, line, match.groupValues[3].toIntOrNull())
    }.toList()
}

/** Finds no links. */
val NoLinks = ConsoleLinkDetector { emptyList() }
