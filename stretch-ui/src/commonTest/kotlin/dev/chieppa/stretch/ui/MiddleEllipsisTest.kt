package dev.chieppa.stretch.ui

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class MiddleEllipsisTest {
    @Test
    fun textThatFitsStaysWhole() {
        assertEquals("src/App.kt", middleEllipsis("src/", "App.kt") { it.length <= 20 })
    }

    @Test
    fun theMiddleGoesFirstAndTheSuffixStays() {
        assertEquals("src/…/ui/App.kt", middleEllipsis("src/main/kotlin/ui/", "App.kt") { it.length <= 15 })
    }

    @Test
    fun withNoRoomOnlyTheEllipsisAndSuffixRemain() {
        assertEquals("…App.kt", middleEllipsis("src/main/", "App.kt") { it.length <= 3 })
    }

    @Test
    fun aPathSplitsAtItsLastSeparator() {
        assertEquals("src\\ui/" to "App.kt", splitPath("src\\ui/App.kt"))
        assertEquals("" to "README.md", splitPath("README.md"))
    }

    @Test
    fun aNarrowPathLabelKeepsTheFileName() = runComposeUiTest {
        val path = "stretch-ui/src/commonMain/kotlin/dev/chieppa/stretch/ui/PathLabel.kt"
        setContent { StretchTheme(StretchThemes.Ink) { PathLabel(path, Stretch.type.body, Stretch.colors.ink, Modifier.width(180.dp)) } }
        val node = onNodeWithContentDescription(path).fetchSemanticsNode()
        val shown = node.children.firstNotNullOfOrNull { it.config.getOrNull(SemanticsProperties.Text) }?.joinToString { it.text }
            ?: node.config.getOrNull(SemanticsProperties.Text)?.joinToString { it.text }
        assertTrue(shown != null && shown.endsWith("/PathLabel.kt") && ELLIPSIS in shown, "shows $shown")
    }
}
