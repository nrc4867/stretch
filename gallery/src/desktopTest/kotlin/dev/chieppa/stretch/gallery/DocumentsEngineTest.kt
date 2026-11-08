package dev.chieppa.stretch.gallery

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.test.runComposeUiTestAwaiting
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.test.runComposeUiTestAwaiting
import dev.chieppa.stretch.theme.StretchThemes
import dev.chieppa.stretch.web.EngineWork
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes

/**
 * Draws the documents page with the real engine and Mermaid into build/documents-pages, each picture once the engine
 * has drawn the page and its diagrams. It runs only with -Pcef. It is how to see the page on an OS no one here has: CI
 * uploads the pictures.
 */
@OptIn(ExperimentalTestApi::class)
class DocumentsEngineTest {
    private val output = File(System.getProperty("stretch.web.output")).also { it.mkdirs() }

    @Test
    fun drawsTheDocumentsPageInEachThemeFamily() = runComposeUiTestAwaiting(EngineWork, testTimeout = 5.minutes) {
        var theme by mutableStateOf(StretchThemes.Ink)
        setContent {
            StretchTheme(theme) {
                Box(Modifier.size(1100.dp, 760.dp).testTag("page")) { DocumentsPage() }
            }
        }
        for (candidate in listOf(StretchThemes.Ink, StretchThemes.WorkshopDark)) {
            theme = candidate
            for (tab in listOf("Markdown", "Diff", "HTML")) {
                onNodeWithText(tab).performClick()
                waitForIdle()
                val image = onNodeWithTag("page").captureToImage().toAwtImage()
                ImageIO.write(image, "png", File(output, "${candidate.id}-${tab.lowercase()}.png"))
            }
        }
    }
}
