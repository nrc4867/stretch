package dev.chieppa.stretch.mermaid

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import dev.chieppa.stretch.theme.StretchColors

private const val RGB_MASK = 0xFFFFFF
private const val HEX_DIGITS = 6

/** A system face, since a picture of an SVG can only use installed fonts. Mermaid measures with the same one. */
internal const val DIAGRAM_FONT = "'Segoe UI', -apple-system, 'Helvetica Neue', Arial, sans-serif"

private fun Color.css(): String = "#" + (toArgb() and RGB_MASK).toString(16).padStart(HEX_DIGITS, '0')

private fun quote(text: String) = "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

/** Mermaid's theme variables, as JSON, so a diagram takes the colors of the app's theme. */
internal fun mermaidThemeVariables(c: StretchColors, dark: Boolean): String {
    val variables = linkedMapOf(
        "darkMode" to dark.toString(),
        "background" to c.surfacePanel.css(),
        "fontFamily" to DIAGRAM_FONT,
        "fontSize" to "14px",
        "primaryColor" to c.surfaceControl.css(),
        "primaryTextColor" to c.ink.css(),
        "primaryBorderColor" to c.lineStrong.css(),
        "secondaryColor" to c.surfaceFloat.css(),
        "secondaryTextColor" to c.ink.css(),
        "secondaryBorderColor" to c.lineControl.css(),
        "tertiaryColor" to c.surfacePanel.css(),
        "tertiaryTextColor" to c.ink.css(),
        "tertiaryBorderColor" to c.lineControl.css(),
        "lineColor" to c.inkMuted.css(),
        "textColor" to c.ink.css(),
        "mainBkg" to c.surfaceControl.css(),
        "nodeBorder" to c.lineStrong.css(),
        "clusterBkg" to c.surfaceFloat.css(),
        "clusterBorder" to c.lineControl.css(),
        "titleColor" to c.inkStrong.css(),
        "edgeLabelBackground" to c.surfacePanel.css(),
        "actorBkg" to c.surfaceControl.css(),
        "actorBorder" to c.lineStrong.css(),
        "actorTextColor" to c.ink.css(),
        "actorLineColor" to c.inkMuted.css(),
        "signalColor" to c.inkMuted.css(),
        "signalTextColor" to c.ink.css(),
        "labelBoxBkgColor" to c.surfaceFloat.css(),
        "labelBoxBorderColor" to c.lineControl.css(),
        "labelTextColor" to c.ink.css(),
        "loopTextColor" to c.ink.css(),
        "noteBkgColor" to c.warnWash.css(),
        "noteTextColor" to c.warnInk.css(),
        "noteBorderColor" to c.warnEdge.css(),
        "activationBkgColor" to c.onFill.css(),
        "activationBorderColor" to c.onEdge.css(),
        "pie1" to c.chart1.css(),
        "pie2" to c.chart2.css(),
        "pie3" to c.chart3.css(),
        "pie4" to c.chart4.css(),
        "pie5" to c.chart5.css(),
        "pie6" to c.chart6.css(),
        "pie7" to c.chart7.css(),
        "pie8" to c.chart8.css(),
        "git0" to c.chart1.css(),
        "git1" to c.chart2.css(),
        "git2" to c.chart3.css(),
        "git3" to c.chart4.css(),
    )
    return variables.entries.joinToString(",", "{", "}") { (name, value) ->
        quote(name) + ":" + if (name == "darkMode") value else quote(value)
    }
}

/** A string as a JavaScript string literal. */
internal fun jsString(text: String): String = buildString {
    append('"')
    for (char in text) {
        when (char) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            ' ' -> append("\\u2028")
            ' ' -> append("\\u2029")
            '<' -> append("\\u003c")
            else -> append(char)
        }
    }
    append('"')
}

/**
 * Sets up `stretchMermaid(source, variables, scale)` in the page. It draws a diagram to a PNG, returning
 * `width|height|base64`, with width and height in points. Labels are SVG text, not HTML: an SVG holding HTML would
 * taint the canvas and block reading the picture back. `strict` keeps a document's diagram from running script.
 */
internal const val MERMAID_BRIDGE = """
window.stretchMermaidCount = 0;
window.stretchMermaid = async (source, variables, scale) => {
  mermaid.initialize({
    startOnLoad: false, securityLevel: 'strict', theme: 'base', themeVariables: variables, suppressErrorRendering: true,
    htmlLabels: false, flowchart: { htmlLabels: false }, class: { htmlLabels: false }, state: { htmlLabels: false }, mindmap: { htmlLabels: false },
  });
  const id = 'stretch-diagram-' + (++window.stretchMermaidCount);
  const { svg } = await mermaid.render(id, source);
  const doc = new DOMParser().parseFromString(svg, 'image/svg+xml');
  const root = doc.documentElement;
  const box = (root.getAttribute('viewBox') || '').split(/[\s,]+/).map(Number);
  const width = Math.ceil(box[2] || parseFloat(root.getAttribute('width')) || 300);
  const height = Math.ceil(box[3] || parseFloat(root.getAttribute('height')) || 150);
  root.setAttribute('width', width);
  root.setAttribute('height', height);
  root.removeAttribute('style');
  const text = new XMLSerializer().serializeToString(doc);
  const image = new Image();
  image.src = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(text);
  await image.decode();
  const canvas = document.createElement('canvas');
  canvas.width = Math.ceil(width * scale);
  canvas.height = Math.ceil(height * scale);
  canvas.getContext('2d').drawImage(image, 0, 0, canvas.width, canvas.height);
  return width + '|' + height + '|' + canvas.toDataURL('image/png').split(',')[1];
};
"""
