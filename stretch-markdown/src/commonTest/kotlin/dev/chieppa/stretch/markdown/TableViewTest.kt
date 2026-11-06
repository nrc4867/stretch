package dev.chieppa.stretch.markdown

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class TableViewTest {
    private val long = "a long cell that says a great deal more than fits on one line of a narrow table"

    private fun ComposeUiTest.show(markdown: String) {
        setContent {
            StretchTheme(StretchThemes.Ink) {
                MarkdownEditor(rememberMarkdownEditorState(markdown), Modifier.size(width = 420.dp, height = 600.dp), readOnly = true)
            }
        }
        waitForIdle()
    }

    private fun cell(text: String) = SemanticsMatcher("cell '$text'") { node ->
        SemanticsProperties.EditableText in node.config && node.config[SemanticsProperties.EditableText].text == text
    }

    @Test
    fun aLongCellWrapsAndItsRowGrows() = runComposeUiTest {
        show("| Name | Notes |\n|---|---|\n| one | $long |\n")
        val root = onRoot().getUnclippedBoundsInRoot()
        val notes = onNode(cell(long)).getUnclippedBoundsInRoot()
        val name = onNode(cell("Name")).getUnclippedBoundsInRoot()
        assertTrue(notes.right <= root.right, "the long cell stays inside the editor: ${notes.right} > ${root.right}")
        assertTrue(notes.height > name.height * 2, "the long cell wraps over several lines: ${notes.height} vs ${name.height}")
        assertTrue(notes.width > onNode(cell("one")).getUnclippedBoundsInRoot().width, "the long column gets more of the width")
    }

    @Test
    fun aTableWithTooManyColumnsScrollsSideways() = runComposeUiTest {
        val columns = (1..12).joinToString(" | ", "| ", " |") { "c$it" }
        val rule = (1..12).joinToString("|", "|", "|") { "---" }
        show("$columns\n$rule\n")
        val last = onNode(cell("c12")).getUnclippedBoundsInRoot()
        assertTrue(last.right > onRoot().getUnclippedBoundsInRoot().right, "the last column lies past the editor's edge")
    }
}
