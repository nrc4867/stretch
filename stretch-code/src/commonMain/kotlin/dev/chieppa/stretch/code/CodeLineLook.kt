package dev.chieppa.stretch.code

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchSpacing

/** The wash behind a whole line, for lines an app wants to set apart. Each tone takes its color from the theme. */
enum class LineTone { Added, Removed, Warning, Error, Emphasis, Selected }

/** A run of characters on one line drawn on [background], in [color] when given. */
@Immutable
data class CodeHighlight(val start: Int, val end: Int, val background: Color, val color: Color? = null)

/** The colors and text style lines draw with, gathered once for all the lines of a view. */
@Immutable
class CodeLook(
    val code: CodeColors,
    val style: TextStyle,
    val ink: Color,
    val muted: Color,
    val strong: Color,
    val match: Color,
    val currentMatch: Color,
    val onCurrentMatch: Color,
    private val washes: Map<LineTone, Color>,
) {
    fun wash(tone: LineTone?): Color = tone?.let { washes[it] } ?: Color.Transparent
}

@Composable
fun rememberCodeLook(): CodeLook {
    val c = Stretch.colors
    val code = codeColors
    val style = Stretch.type.valueSm
    return remember(c, style) {
        CodeLook(
            code = code,
            style = style.merge(TextStyle(color = c.ink)),
            ink = c.ink,
            muted = c.inkSubtle,
            strong = c.inkStrong,
            match = c.warnWash,
            currentMatch = c.accent,
            onCurrentMatch = c.onAccent,
            washes = mapOf(
                LineTone.Added to c.diffAddWash,
                LineTone.Removed to c.diffDelWash,
                LineTone.Warning to c.warnWash,
                LineTone.Error to c.errorWash,
                LineTone.Emphasis to c.surfaceControl,
                LineTone.Selected to c.surfaceControlHi,
            ),
        )
    }
}

/** Room for code: one character's width, a line's height, the line numbers' width and the longest line's width. */
@Immutable
class CodeMetrics(val charWidth: Dp, val lineHeight: Dp, val numberWidth: Dp, val codeWidth: Dp)

/** Metrics for lines up to [longest] characters, tabs expanded, numbered up to [highest], in [style]. */
@Composable
fun rememberCodeMetrics(longest: Int, highest: Int, style: TextStyle = Stretch.type.valueSm): CodeMetrics {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(longest, highest, style, density) {
        val digit = measurer.measure("0", style).size
        with(density) {
            CodeMetrics(
                charWidth = digit.width.toDp(),
                lineHeight = digit.height.toDp() + 2.dp,
                numberWidth = (digit.width * highest.toString().length).toDp() + StretchSpacing.space12,
                codeWidth = (digit.width * (longest + 2)).toDp(),
            )
        }
    }
}

/** The longest of [lines] once tabs are expanded, for [rememberCodeMetrics]. */
fun longestLine(lines: List<String>): Int = lines.maxOfOrNull { expandTabs(it).length } ?: 0

/** [line] with tabs expanded, [tokens] colored and then [highlights] drawn over them. Offsets are in [line] before expanding. */
fun codeLineText(line: String, tokens: List<CodeToken>, colors: CodeColors, highlights: List<CodeHighlight> = emptyList()): AnnotatedString {
    val offsets = tabOffsets(line)
    fun at(offset: Int) = offsets[offset.coerceIn(0, line.length)]
    return buildAnnotatedString {
        append(expandTabs(line))
        for (token in tokens) addStyle(colors.spanOf(token.kind), at(token.start), at(token.end))
        for (mark in highlights) addStyle(SpanStyle(background = mark.background, color = mark.color ?: Color.Unspecified), at(mark.start), at(mark.end))
    }
}
