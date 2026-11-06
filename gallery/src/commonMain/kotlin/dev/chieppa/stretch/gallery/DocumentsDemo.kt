package dev.chieppa.stretch.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.frame.RequirementRow
import dev.chieppa.stretch.markdown.MarkdownEditor
import dev.chieppa.stretch.markdown.MarkdownMode
import dev.chieppa.stretch.markdown.rememberMarkdownEditorState
import dev.chieppa.stretch.mermaid.MermaidDiagrams
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.ui.Segmented
import dev.chieppa.stretch.web.HtmlContent
import dev.chieppa.stretch.web.HtmlView
import dev.chieppa.stretch.web.WebEngine

private enum class DocumentTab(val label: String) { Markdown("Markdown"), Diff("Diff"), Code("Code"), Html("HTML"), Bridge("Page messages"), Console("Console") }

/** The rich document components: a markdown editor, a diff viewer and an HTML view. */
@Composable
internal fun DocumentsPage() {
    var tab by remember { mutableStateOf(DocumentTab.Markdown) }
    Column(Modifier.fillMaxSize().background(Stretch.colors.surfaceCanvas)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Segmented(DocumentTab.entries, tab, { tab = it }, { it.label })
            // Where the app runs without the engine, pages and diagrams show their source until it is fetched.
            RequirementRow(WebEngine.requirement, "Browser engine")
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
                DocumentTab.Markdown -> MarkdownDemo()
                DocumentTab.Diff -> DiffDemo()
                DocumentTab.Code -> CodeDemo()
                DocumentTab.Html -> HtmlDemo()
                DocumentTab.Bridge -> HtmlBridgeDemo()
                DocumentTab.Console -> ConsoleDemo()
            }
        }
    }
}

@Composable
private fun MarkdownDemo() {
    val state = rememberMarkdownEditorState(SampleDocuments.markdown)
    val diagrams = remember { MermaidDiagrams() }
    MarkdownEditor(state, Modifier.fillMaxSize(), modes = MarkdownMode.entries, diagrams = diagrams)
}

@Composable
private fun HtmlDemo() {
    HtmlView(HtmlContent.Inline(SampleDocuments.html), Modifier.fillMaxSize())
}
