package dev.chieppa.stretch.build

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Writes Compose vector icons from the 16px stroke SVGs in design/icons. Supports path, circle and rect elements;
 * an element with fill="currentColor" is drawn filled, everything else stroked.
 */
abstract class GenerateIcons : GeneratedSource() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val icons: DirectoryProperty

    override fun render(): String {
        val files = icons.get().asFile.listFiles { f -> f.extension == "svg" }!!.sortedBy { it.name }
        val out = StringBuilder()
        out.appendLine("// Generated from design/icons by :stretch-theme:generateIcons. Do not edit.")
        out.appendLine("package dev.chieppa.stretch.theme")
        out.appendLine()
        out.appendLine("import androidx.compose.ui.graphics.vector.ImageVector")
        out.appendLine()
        out.appendLine("object StretchIcons {")
        for (file in files) {
            val name = file.nameWithoutExtension
            out.appendLine("    val ${name.pascalCase()}: ImageVector by lazy {")
            out.appendLine("        icon(\"$name\") {")
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
            val children = doc.documentElement.childNodes
            for (i in 0 until children.length) {
                val el = children.item(i) as? Element ?: continue
                val d = when (el.tagName) {
                    "path" -> el.getAttribute("d")
                    "circle" -> circle(el.num("cx"), el.num("cy"), el.num("r"))
                    "rect" -> rect(el.num("x"), el.num("y"), el.num("width"), el.num("height"), el.num("rx"))
                    else -> error("${file.name}: unsupported element <${el.tagName}>")
                }
                val kind = if (el.getAttribute("fill") == "currentColor") "fill" else "stroke"
                out.appendLine("            $kind(\"$d\")")
            }
            out.appendLine("        }")
            out.appendLine("    }")
        }
        out.appendLine("}")
        return out.toString()
    }

    private fun Element.num(name: String) = getAttribute(name).ifEmpty { "0" }.toDouble()

    private fun circle(cx: Double, cy: Double, r: Double) =
        "M${(cx - r).kt()} ${cy.kt()}a${r.kt()} ${r.kt()} 0 1 0 ${(2 * r).kt()} 0a${r.kt()} ${r.kt()} 0 1 0 ${(-2 * r).kt()} 0z"

    private fun rect(x: Double, y: Double, w: Double, h: Double, rx: Double): String {
        if (rx == 0.0) return "M${x.kt()} ${y.kt()}h${w.kt()}v${h.kt()}h${(-w).kt()}z"
        val r = rx.kt()
        return "M${(x + rx).kt()} ${y.kt()}h${(w - 2 * rx).kt()}a$r $r 0 0 1 $r $r" +
            "v${(h - 2 * rx).kt()}a$r $r 0 0 1 -$r $r" +
            "h${(-(w - 2 * rx)).kt()}a$r $r 0 0 1 -$r -$r" +
            "v${(-(h - 2 * rx)).kt()}a$r $r 0 0 1 $r -${r}z"
    }
}
