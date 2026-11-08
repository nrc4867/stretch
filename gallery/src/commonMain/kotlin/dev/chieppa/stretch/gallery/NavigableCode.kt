package dev.chieppa.stretch.gallery

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import dev.chieppa.stretch.code.CodeDecoration
import dev.chieppa.stretch.code.CodeDecorationStyle
import dev.chieppa.stretch.code.CodeLanguages
import dev.chieppa.stretch.code.CodePosition
import dev.chieppa.stretch.code.CodeRange
import dev.chieppa.stretch.code.CodeSymbolClick
import dev.chieppa.stretch.code.CodeText
import dev.chieppa.stretch.code.CodeViewer
import dev.chieppa.stretch.code.CodeViewerSlots
import dev.chieppa.stretch.code.CodeViewerState
import dev.chieppa.stretch.code.LineTone
import dev.chieppa.stretch.frame.Menu
import dev.chieppa.stretch.frame.MenuItem
import dev.chieppa.stretch.frame.MenuLabel
import kotlinx.coroutines.launch

/** How many usages the menu lists at most. */
private const val MAX_USAGES_LISTED = 12

/**
 * A code viewer whose names click through, as an app with a parser makes one: Ctrl or Cmd and a click on a name goes to
 * its definition, and on a definition lists its usages under it; F12 or Ctrl B does the same from the caret. The name
 * clicked is decorated with its definition and usages, beside marks in the other decoration styles.
 */
@Composable
internal fun NavigableCode(text: CodeText, state: CodeViewerState, modifier: Modifier, tones: (Int) -> LineTone?) {
    val symbols = remember(text) { SampleSymbols(text.lines) }
    val scope = rememberCoroutineScope()
    var focus by remember { mutableStateOf<CodeRange?>(null) }
    var usages by remember { mutableStateOf<CodeSymbolClick?>(null) }
    fun goTo(range: CodeRange) {
        state.caret = CodePosition(range.line, range.start)
        scope.launch { state.scrollToMatch(range) }
    }
    val slots = CodeViewerSlots(
        symbolAt = { symbols.nameAt(it) },
        onSymbolClick = { click ->
            focus = click.range
            val definition = symbols.definitionOf(click.range)
            if (definition == null || definition == click.range) usages = click else goTo(definition)
        },
    )
    val marks = remember(text) { sampleMarks(text) }
    val decorations = remember(focus, marks) { focus?.let { symbols.decorate(it) }.orEmpty() + marks }
    Box(
        modifier.onPreviewKeyEvent { event ->
            val navigate = event.type == KeyEventType.KeyDown &&
                (event.key == Key.F12 || (event.key == Key.B && (event.isCtrlPressed || event.isMetaPressed)))
            if (navigate) scope.launch { state.clickSymbolAtCaret() }
            navigate
        },
    ) {
        CodeViewer(text, Modifier.fillMaxSize(), state, CodeLanguages.named("kotlin"), tones, slots = slots, decorations = decorations)
        val open = usages
        Menu(open != null, { usages = null }, anchor = open?.bounds) {
            val found = open?.let { symbols.usagesOf(it.range) }.orEmpty()
            MenuLabel(if (found.isEmpty()) "No usages" else "${found.size} usages")
            for (usage in found.take(MAX_USAGES_LISTED)) {
                MenuItem("Line ${usage.line}: ${symbols.line(usage.line).trim()}", {
                    usages = null
                    goTo(usage)
                })
            }
        }
    }
}

/** Marks in the styles a clicked name does not show, as a plugin might add: a warning, an error and an accent. */
private fun sampleMarks(text: CodeText): List<CodeDecoration> = buildList {
    val marked = listOf(
        Triple("helper13", CodeDecorationStyle.Warning, "Never called"),
        Triple("20 * 2", CodeDecorationStyle.Error, "Doubles a value no one reads"),
        Triple("\"Hello, ", CodeDecorationStyle.Accent, "A plugin's note: greets by name"),
    )
    text.lines.forEachIndexed { index, line ->
        for ((word, style, tip) in marked) {
            val at = line.indexOf(word)
            if (at >= 0) add(CodeDecoration(CodeRange(index + 1, at, at + word.length), style, tip))
        }
    }
}
