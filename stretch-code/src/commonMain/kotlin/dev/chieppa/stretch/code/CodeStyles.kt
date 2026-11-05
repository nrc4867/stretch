package dev.chieppa.stretch.code

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import dev.chieppa.stretch.theme.Stretch

/** The colours of each [TokenKind], from the theme. */
@Immutable
class CodeColors(
    val keyword: Color,
    val string: Color,
    val number: Color,
    val comment: Color,
    val type: Color,
    val function: Color,
) {
    fun of(kind: TokenKind): Color = when (kind) {
        TokenKind.Keyword -> keyword
        TokenKind.String -> string
        TokenKind.Number -> number
        TokenKind.Comment -> comment
        TokenKind.Type -> type
        TokenKind.Function -> function
    }
}

/** The current theme's [CodeColors]. */
val codeColors: CodeColors
    @Composable
    @ReadOnlyComposable
    get() {
        val c = Stretch.colors
        return CodeColors(c.syntaxKeyword, c.syntaxString, c.syntaxNumber, c.syntaxComment, c.syntaxType, c.syntaxFunction)
    }

/** The span a token of [kind] draws with. Comments are italic as well as coloured, so they read without colour. */
fun CodeColors.spanOf(kind: TokenKind): SpanStyle =
    SpanStyle(color = of(kind), fontStyle = if (kind == TokenKind.Comment) FontStyle.Italic else null)

/** [text] with [tokens] coloured. */
fun highlighted(text: String, tokens: List<CodeToken>, colors: CodeColors): AnnotatedString = buildAnnotatedString {
    append(text)
    for (token in tokens) {
        if (token.end <= text.length) addStyle(colors.spanOf(token.kind), token.start, token.end)
    }
}
