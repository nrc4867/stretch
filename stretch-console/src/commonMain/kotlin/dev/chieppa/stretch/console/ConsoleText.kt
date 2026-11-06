package dev.chieppa.stretch.console

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import dev.chieppa.stretch.theme.Stretch

/** How faint dim text is, against its own color. */
private const val DIM_ALPHA = 0.6f

/** The six steps of each channel in the 6×6×6 color cube of the 256 numbered colors. */
private val CUBE_STEPS = intArrayOf(0, 95, 135, 175, 215, 255)
private const val CUBE_START = 16
private const val GRAY_START = 232
private const val GRAY_BASE = 8
private const val GRAY_STEP = 10

/**
 * The colors a console draws with, from the theme. The first 16 numbered colors, which programs use most, follow the
 * theme so output reads in light and dark; the rest of the 256, and exact colors, are the program's own.
 */
@Immutable
class ConsolePalette(
    val ansi: List<Color>,
    val ink: Color,
    val errorInk: Color,
    val background: Color,
    val link: Color,
    val match: Color,
    val currentMatch: Color,
    val onCurrentMatch: Color,
) {
    fun of(color: AnsiColor): Color = when (color) {
        is AnsiColor.Rgb -> Color(color.red, color.green, color.blue)
        is AnsiColor.Indexed -> when {
            color.index < CUBE_START -> ansi[color.index]
            color.index < GRAY_START -> {
                val n = color.index - CUBE_START
                Color(CUBE_STEPS[n / 36], CUBE_STEPS[n / 6 % 6], CUBE_STEPS[n % 6])
            }
            else -> (GRAY_BASE + (color.index - GRAY_START) * GRAY_STEP).let { Color(it, it, it) }
        }
    }
}

@Composable
fun rememberConsolePalette(): ConsolePalette {
    val c = Stretch.colors
    return remember(c) {
        ConsolePalette(
            ansi = listOf(
                c.inkSubtle, c.errorInk, c.deltaUp, c.warnInk, c.syntaxKeyword, c.syntaxNumber, c.syntaxType, c.inkControl,
                c.inkMuted, c.deltaDown, c.diffAddInk, c.syntaxString, c.accent, c.syntaxFunction, c.chart3, c.inkStrong,
            ),
            ink = c.ink,
            errorInk = c.errorInk,
            background = c.surfacePanel,
            link = c.accent,
            match = c.warnWash,
            currentMatch = c.accent,
            onCurrentMatch = c.onAccent,
        )
    }
}

/** The span a run of [style] from [stream] draws with. */
fun ConsolePalette.spanOf(style: AnsiStyle, stream: ConsoleStream): SpanStyle {
    val ownInk = if (stream == ConsoleStream.Err) errorInk else ink
    var fore = style.foreground?.let(::of) ?: ownInk
    var back = style.background?.let(::of)
    if (style.inverse) {
        val swapped = back ?: background
        back = fore
        fore = swapped
    }
    if (style.dim) fore = fore.copy(alpha = fore.alpha * DIM_ALPHA)
    val decorations = listOfNotNull(TextDecoration.Underline.takeIf { style.underline }, TextDecoration.LineThrough.takeIf { style.strikethrough })
    return SpanStyle(
        color = fore,
        background = back ?: Color.Unspecified,
        fontWeight = if (style.bold) FontWeight.Bold else null,
        fontStyle = if (style.italic) FontStyle.Italic else null,
        textDecoration = if (decorations.isEmpty()) null else TextDecoration.combine(decorations),
    )
}

/**
 * [line] styled with [palette]: its colors, [links] that call [onLink] when clicked, and find [matches] marked, the one
 * at [current] most strongly.
 */
fun consoleLineText(
    line: ConsoleLine,
    palette: ConsolePalette,
    links: List<ConsoleLink>,
    onLink: ((ConsoleLink) -> Unit)?,
    matches: List<ConsoleMatch> = emptyList(),
    current: ConsoleMatch? = null,
): AnnotatedString = buildAnnotatedString {
    for (span in line.spans) {
        val start = length
        append(span.text)
        addStyle(palette.spanOf(span.style, span.stream), start, length)
    }
    if (onLink != null) {
        for (link in links) {
            val style = TextLinkStyles(SpanStyle(color = palette.link, textDecoration = TextDecoration.Underline))
            addLink(LinkAnnotation.Clickable(link.target, style) { onLink(link) }, link.start, link.end)
        }
    }
    for (match in matches) {
        val strong = match == current
        addStyle(
            SpanStyle(background = if (strong) palette.currentMatch else palette.match, color = if (strong) palette.onCurrentMatch else Color.Unspecified),
            match.start,
            match.end,
        )
    }
}
