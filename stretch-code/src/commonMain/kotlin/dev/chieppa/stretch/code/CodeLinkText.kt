package dev.chieppa.stretch.code

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration

/** [text], a line of code as [codeLineText] builds it from [line], with the characters [start] until [end] drawn as a link in [color]. */
fun withCodeLink(text: AnnotatedString, line: String, start: Int, end: Int, color: Color): AnnotatedString {
    val offsets = tabOffsets(line)
    val from = offsets[start.coerceIn(0, line.length)]
    val until = offsets[end.coerceIn(0, line.length)]
    if (until <= from) return text
    return AnnotatedString.Builder(text).apply {
        addStyle(SpanStyle(color = color, textDecoration = TextDecoration.Underline), from, until)
    }.toAnnotatedString()
}
