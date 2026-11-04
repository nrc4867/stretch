package dev.chieppa.stretch.gallery

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.fail

/**
 * Every control in the gallery says what it is to a screen reader: anything that can be clicked, focused, toggled
 * or set has a name, and toggles and selections report their state.
 */
@OptIn(ExperimentalTestApi::class)
class AccessibilityTest {
    private fun SemanticsNode.all(): List<SemanticsNode> = listOf(this) + children.flatMap { it.all() }

    private fun SemanticsNode.named(): Boolean {
        val c = config
        return !c.getOrNull(SemanticsProperties.ContentDescription).isNullOrEmpty() ||
            !c.getOrNull(SemanticsProperties.Text).isNullOrEmpty() ||
            c.getOrNull(SemanticsProperties.EditableText) != null ||
            children.any { it.named() && !it.config.isMergingSemanticsOfDescendants }
    }

    private fun SemanticsNode.describe(): String {
        val c = config
        return "#$id ${c.getOrNull(SemanticsProperties.Role) ?: ""} at ${boundsInRoot} " +
            c.toList().joinToString { it.key.name }
    }

    private fun ComposeUiTest.problems(page: String): List<String> {
        waitForIdle()
        val nodes = onRoot().fetchSemanticsNode().all()
        return nodes.mapNotNull { node ->
            val c = node.config
            val interactive = SemanticsActions.OnClick in c || SemanticsActions.SetProgress in c ||
                SemanticsProperties.ToggleableState in c || SemanticsActions.SetText in c || SemanticsProperties.Focused in c
            when {
                c.getOrNull(SemanticsProperties.InvisibleToUser) != null -> null
                interactive && !node.named() -> "$page: unnamed control ${node.describe()}"
                SemanticsActions.SetProgress in c && c.getOrNull(SemanticsProperties.ProgressBarRangeInfo) == null &&
                    c.getOrNull(SemanticsProperties.StateDescription) == null -> "$page: adjustable without a value ${node.describe()}"
                else -> null
            }
        }
    }

    @Test
    fun everyControlIsNamed() = runComposeUiTest {
        setContent { Gallery() }
        val found = mutableListOf<String>()
        found += problems("Components")
        for (page in listOf("Data", "Tools", "App frame")) {
            onNodeWithText(page).performClick()
            found += problems(page)
        }
        if (found.isNotEmpty()) fail(found.joinToString("\n"))
    }
}
