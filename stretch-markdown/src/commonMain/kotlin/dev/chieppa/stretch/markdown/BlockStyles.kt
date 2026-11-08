package dev.chieppa.stretch.markdown

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.code.codeColors
import dev.chieppa.stretch.theme.Stretch

@Composable
internal fun rememberMarkupLook(): MarkupLook {
    val c = Stretch.colors
    val mono = Stretch.type.valueSm.fontFamily ?: FontFamily.Monospace
    val code = codeColors
    return remember(c, mono) { MarkupLook(c.inkSubtle, c.syntaxType, c.surfaceControl, mono, code) }
}

internal val bodyStyle: TextStyle
    @Composable @ReadOnlyComposable
    get() = Stretch.type.body.merge(TextStyle(color = Stretch.colors.ink))

internal val codeStyle: TextStyle
    @Composable @ReadOnlyComposable
    get() = Stretch.type.valueSm.merge(TextStyle(color = Stretch.colors.ink))

@Composable
@ReadOnlyComposable
internal fun headingStyle(level: Int): TextStyle {
    val ink = TextStyle(color = Stretch.colors.inkStrong)
    return when (level) {
        1 -> Stretch.type.pageTitle
        2 -> Stretch.type.boardTitle
        3 -> Stretch.type.heading
        4 -> Stretch.type.drawerTitle
        else -> Stretch.type.body.copy(fontWeight = FontWeight.SemiBold)
    }.merge(ink)
}

/** One more than the deepest heading level, so each level up gets a little more room above it. */
private const val HEADING_SPACE_LEVELS = 7

/** The room above a heading of [level]: more for the bigger ones. */
internal fun headingSpace(level: Int): Dp = (HEADING_SPACE_LEVELS - level).coerceAtLeast(0).dp * 2

/** The room between blocks. */
internal val blockGap = 6.dp

/** The room inside a boxed block, such as code. */
internal val boxPadding = 10.dp
