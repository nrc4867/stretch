package dev.chieppa.stretch.markdown

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class MarkdownViewTest {
    @Test
    fun sitsInAScrollingColumnAndOpensDetailsOnAClick() = runComposeUiTest {
        val loaded = mutableListOf<String>()
        val images = ImageSource { url -> loaded += url; ImageBitmap(40, 20) }
        setContent {
            StretchTheme(StretchThemes.Ink) {
                Column(Modifier.width(600.dp).verticalScroll(rememberScrollState())) {
                    MarkdownView(dependabotDescription, images = images)
                    MarkdownView("A second comment with *style*.")
                }
            }
        }
        onNodeWithText("Bumps actions/checkout from 3 to 4.", substring = true).assertExists()
        onNodeWithText("Release notes").assertExists()
        onNodeWithText("v4.0.0").assertDoesNotExist()
        onNodeWithText("Release notes").performClick()
        onNodeWithText("v4.0.0").assertExists()
        onNodeWithText("Sourced from actions/checkout's releases.").assertExists()
        onAllNodesWithText("automerge", substring = true).assertCountEquals(0)
        onNodeWithText("you don't alter it", substring = true).assertExists()
        onAllNodesWithContentDescription("Dependabot compatibility score").assertCountEquals(1)
        assertEquals(listOf(DEPENDABOT_BADGE), loaded)
        onNodeWithText("A second comment with style.").assertExists()
    }

    @Test
    fun aClickOnALinkOpensIt() = runComposeUiTest {
        var opened: String? = null
        setContent { StretchTheme(StretchThemes.Ink) { MarkdownView("[docs](https://docs.test/a)", onLinkClick = { opened = it }) } }
        onNodeWithText("docs").performMouseInput { click(centerLeft + Offset(4f, 0f)) }
        assertEquals("https://docs.test/a", opened)
    }

    @Test
    fun htmlListsAndTablesShow() = runComposeUiTest {
        val html = "<ol start=\"3\"><li>three</li><li>four</li></ol>\n<table><tr><th>Name</th></tr><tr><td>Value</td></tr></table>"
        setContent { StretchTheme(StretchThemes.Ink) { MarkdownView(html) } }
        onNodeWithText("3.").assertExists()
        onNodeWithText("4.").assertExists()
        onNodeWithText("four").assertExists()
        onNodeWithText("Name").assertExists()
        onNodeWithText("Value").assertExists()
    }
}
