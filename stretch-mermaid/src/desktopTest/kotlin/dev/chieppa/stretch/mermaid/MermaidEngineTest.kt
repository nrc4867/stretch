package dev.chieppa.stretch.mermaid

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.luminance
import dev.chieppa.stretch.markdown.DiagramStyle
import dev.chieppa.stretch.theme.StretchThemes
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/** Draws real diagrams in the real engine, so it runs only with -Pcef. It writes them to build/engine-test. */
class MermaidEngineTest {
    private val output = File(System.getProperty("stretch.web.output")).also { it.mkdirs() }
    private val diagrams = mapOf(
        "flowchart" to "flowchart LR\n  A[Start here] --> B{Is it good?}\n  B -- yes --> C((Ship))\n  B -- no --> D[Fix it]\n  D --> B",
        "sequence" to "sequenceDiagram\n  Alice->>Bob: Hello\n  Note right of Bob: Thinks\n  Bob-->>Alice: Hi back",
        "class" to "classDiagram\n  Animal <|-- Duck\n  Animal : +int age\n  Duck : +swim()",
        "state" to "stateDiagram-v2\n  [*] --> Still\n  Still --> Moving\n  Moving --> [*]",
        "er" to "erDiagram\n  CUSTOMER ||--o{ ORDER : places",
        "gantt" to "gantt\n  title Plan\n  dateFormat YYYY-MM-DD\n  section A\n  Task :a1, 2024-01-01, 3d",
        "pie" to "pie title Pets\n  \"Dogs\" : 3\n  \"Cats\" : 5",
        "mindmap" to "mindmap\n  root((Root))\n    One\n    Two",
        "git" to "gitGraph\n  commit\n  branch dev\n  commit\n  checkout main\n  merge dev",
    )

    @Test
    fun drawsEveryCommonDiagramInEveryTheme() = runBlocking {
        withTimeout(120_000) {
            val renderer = MermaidDiagrams()
            for (theme in StretchThemes.all) {
                val dark = theme.colors.surfacePanel.luminance() < 0.5f
                val style = DiagramStyle(theme.colors, 2f, dark)
                for ((name, source) in diagrams) {
                    val picture = try {
                        renderer.render(source, style)
                    } catch (e: Exception) {
                        fail("$name in ${theme.id}: ${e.message}")
                    }
                    assertTrue(picture.image.width > 0, name)
                    ImageIO.write(picture.image.toAwtImage(), "png", File(output, "${theme.id}-$name.png"))
                }
            }
            renderer.close()
        }
    }

    @Test
    fun aBadDiagramSaysWhatIsWrong() = runBlocking {
        withTimeout(60_000) {
            val renderer = MermaidDiagrams()
            try {
                renderer.render("flowchart LR\n  A[[[ -->", DiagramStyle(StretchThemes.Ink.colors, 1f, false))
                fail("no error")
            } catch (e: Exception) {
                assertTrue(e.message.orEmpty().isNotEmpty())
            } finally {
                renderer.close()
            }
        }
    }
}
