package dev.chieppa.stretch.build

import groovy.json.JsonSlurper
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

/** Writes the Kotlin token values from the design system's tokens.json. */
abstract class GenerateTokens : GeneratedSource() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val tokens: RegularFileProperty

    @Suppress("UNCHECKED_CAST")
    override fun render(): String {
        val root = JsonSlurper().parse(tokens.get().asFile) as Map<String, Any>
        val color = root["color"] as Map<String, Any>
        val themes = (color["themes"] as List<Map<String, String>>).map { it["id"]!! }
        val colors = color["tokens"] as List<Map<String, Any>>
        val shadows = (root["shadow"] as Map<String, Any>)["tokens"] as List<Map<String, Any>>

        fun valueFor(token: Map<String, Any>, theme: String): String {
            val value = token["value"]
            return if (value is Map<*, *>) (value[theme] ?: value[themes.first()]) as String else value as String
        }

        fun lengths(family: String, prefix: String) =
            ((root[family] as Map<String, Any>)["tokens"] as List<Map<String, Any>>).map {
                (it["name"] as String).removePrefix(prefix).camelCase() to px(it["value"].toString())
            }

        val out = StringBuilder()
        out.appendLine("// Generated from design/tokens.json by :stretch-theme:generateTokens. Do not edit.")
        out.appendLine("package dev.chieppa.stretch.theme")
        out.appendLine()
        out.appendLine("import androidx.compose.runtime.Immutable")
        out.appendLine("import androidx.compose.ui.graphics.Color")
        out.appendLine("import androidx.compose.ui.unit.Dp")
        out.appendLine("import androidx.compose.ui.unit.dp")
        out.appendLine()
        out.appendLine("@Immutable")
        out.appendLine("data class StretchColors(")
        colors.forEach { out.appendLine("    val ${(it["name"] as String).camelCase()}: Color,") }
        out.appendLine(") {")
        out.appendLine("    /** Every color by its name in the design tokens, such as `surface-canvas`. */")
        out.appendLine("    val byName: Map<String, Color>")
        out.appendLine("        get() = mapOf(")
        colors.forEach { out.appendLine("            \"${it["name"]}\" to ${(it["name"] as String).camelCase()},") }
        out.appendLine("        )")
        out.appendLine("}")
        out.appendLine()
        out.appendLine("@Immutable")
        out.appendLine("data class StretchShadows(")
        shadows.forEach { out.appendLine("    val ${(it["name"] as String).removePrefix("shadow-").camelCase()}: StretchShadow,") }
        out.appendLine(")")
        for (theme in themes) {
            val name = theme.pascalCase()
            out.appendLine()
            out.appendLine("internal val ${name}Colors = StretchColors(")
            colors.forEach { out.appendLine("    ${(it["name"] as String).camelCase()} = ${colorLiteral(valueFor(it, theme))},") }
            out.appendLine(")")
            out.appendLine()
            out.appendLine("internal val ${name}Shadows = StretchShadows(")
            shadows.forEach {
                out.appendLine("    ${(it["name"] as String).removePrefix("shadow-").camelCase()} = ${shadowLiteral(valueFor(it, theme))},")
            }
            out.appendLine(")")
        }
        fun lengthObject(name: String, entries: List<Pair<String, Double>>, visibility: String = "") {
            out.appendLine()
            out.appendLine("${visibility}object $name {")
            entries.forEach { (key, value) -> out.appendLine("    val $key: Dp = ${value.kt()}.dp") }
            out.appendLine("}")
        }
        lengthObject("StretchSpacing", lengths("spacing", "space-").map { "space" + it.first.replaceFirstChar(Char::uppercase) to it.second })
        lengthObject("StretchSizes", lengths("size", "size-"))
        lengthObject("InkRadius", lengths("radius", "radius-"), "internal ")
        lengthObject("InkStroke", lengths("stroke", "stroke-"), "internal ")

        val type = root["type"] as Map<String, Any>
        out.appendLine()
        out.appendLine("internal object TypeTokens {")
        (type["groups"] as List<Map<String, Any>>).forEach { group ->
            (group["styles"] as List<Map<String, Any>>).forEach { style ->
                val tracking = style["letterSpacing"]?.toString()?.removeSuffix("em")?.toDouble() ?: 0.0
                out.appendLine(
                    "    val ${(style["name"] as String).camelCase()} = TypeToken(\"${group["family"]}\", " +
                        "${px(style["fontSize"].toString()).kt()}f, ${px(style["lineHeight"].toString()).kt()}f, " +
                        "${style["fontWeight"]}, ${tracking.kt()}f)",
                )
            }
        }
        out.appendLine("}")
        return out.toString()
    }

    private fun px(value: String) = value.removeSuffix("px").toDouble()

    private fun colorLiteral(value: String): String {
        val v = value.trim().lowercase()
        val argb = when {
            v.startsWith("#") -> {
                val hex = v.drop(1).let { h -> if (h.length == 3) h.map { "$it$it" }.joinToString("") else h }
                if (hex.length == 8) hex.drop(6) + hex.take(6) else "ff$hex"
            }
            v.startsWith("rgba(") || v.startsWith("rgb(") -> {
                val parts = v.substringAfter('(').substringBefore(')').split(',').map { it.trim() }
                val alpha = parts.getOrNull(3)?.toDouble() ?: 1.0
                "%02x".format(Math.round(alpha * 255)) + parts.take(3).joinToString("") { "%02x".format(it.toInt()) }
            }
            else -> error("Unsupported color '$value'")
        }
        return "Color(0x${argb.uppercase()})"
    }

    private fun shadowLiteral(value: String): String {
        val match = Regex("""^(\S+)\s+(\S+)\s+(\S+)\s+(.+)$""").matchEntire(value.trim())
            ?: error("Unsupported shadow '$value'")
        val (x, y, blur, color) = match.destructured
        return "StretchShadow(${px(x).kt()}.dp, ${px(y).kt()}.dp, ${px(blur).kt()}.dp, ${colorLiteral(color)})"
    }
}
