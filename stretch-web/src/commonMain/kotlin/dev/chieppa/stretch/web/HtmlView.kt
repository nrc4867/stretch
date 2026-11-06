package dev.chieppa.stretch.web

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.core.Requirement
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.ui.ScrollColumn
import kotlin.math.roundToInt

private const val RGB_MASK = 0xFFFFFF
private const val HEX_DIGITS = 6
private const val DARK_LUMINANCE = 0.5f
private const val CHANNEL = 255f

/** Where a page tried to go: a link someone clicked, or a window it tried to open. */
fun interface HtmlNavigation {
    /** Return true to let the view itself go to [url]. Otherwise it stays put, and the app can open [url] elsewhere. */
    fun onNavigate(url: String): Boolean
}

/** Whether this runtime has a browser engine: always in a browser; on the desktop, a JetBrains Runtime with JCEF. */
expect object WebEngine {
    val available: Boolean

    /**
     * The engine as a [Requirement]: ready where [available], and otherwise something the app can fetch and then
     * restart onto. On the desktop it is also in [dev.chieppa.stretch.core.Requirements].
     */
    val requirement: Requirement
}

/**
 * A real web page inside the app, with its full CSS and scripts. On the desktop it runs in Chromium through JCEF and
 * draws into Compose, so popups and menus show over it; in a browser it is a sandboxed iframe, which popups cannot
 * cover. Pages cannot open windows, download files, show dialogs or ask for permissions.
 *
 * Links go to [navigation], which by default keeps the view where it is. An [HtmlContent.Inline] document starts from
 * the theme's background and text color until its own styles say otherwise. [unavailable] shows where there is no
 * engine (see [WebEngine]); by default it is the document's source or address.
 *
 * The theme's colors, radii and stroke are CSS custom properties on the page's root, named [HTML_THEME_PREFIX] and the
 * token's name, such as `var(--stretch-surface-canvas)`. A [bridge] carries messages between the page and the app; it
 * needs scripts. In a browser, a [HtmlContent.Url] page is not Stretch's to change: it gets neither, and talks to the
 * app itself by posting `stretch-message:` and the text to its parent, and hearing messages that start the same way.
 */
@Composable
expect fun HtmlView(
    content: HtmlContent,
    modifier: Modifier = Modifier,
    policy: HtmlPolicy = HtmlPolicy(),
    navigation: HtmlNavigation = HtmlNavigation { false },
    bridge: HtmlBridge? = null,
    unavailable: @Composable () -> Unit = { HtmlSource(content, modifier) },
)

/** The document's source, or its address, for where it cannot be drawn. */
@Composable
internal fun HtmlSource(content: HtmlContent, modifier: Modifier) {
    val c = Stretch.colors
    ScrollColumn(modifier.background(c.surfaceFloat)) {
        BasicText(
            text = when (content) {
                is HtmlContent.Inline -> content.html
                is HtmlContent.Url -> content.url
            },
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            style = Stretch.type.valueSm.merge(TextStyle(color = c.inkMuted)),
        )
    }
}

internal fun Color.toCss(): String =
    if (alpha >= 1f) "#" + (toArgb() and RGB_MASK).toString(16).padStart(HEX_DIGITS, '0') else "rgba(${(red * CHANNEL).roundToInt()},${(green * CHANNEL).roundToInt()},${(blue * CHANNEL).roundToInt()},$alpha)"

/** The prefix of every custom property a page gets from the theme. */
const val HTML_THEME_PREFIX = "--stretch-"

/** The current theme as a document's starting look. */
internal val currentHtmlLook: HtmlLook
    @Composable
    @ReadOnlyComposable
    get() {
        val c = Stretch.colors
        val shapes = Stretch.shapes
        val variables = c.byName.entries.associate { (name, color) -> "$HTML_THEME_PREFIX$name" to color.toCss() } +
            mapOf(
                "${HTML_THEME_PREFIX}radius-seg" to "${shapes.seg.value}px",
                "${HTML_THEME_PREFIX}radius-ctl" to "${shapes.ctl.value}px",
                "${HTML_THEME_PREFIX}radius-row" to "${shapes.row.value}px",
                "${HTML_THEME_PREFIX}radius-card" to "${shapes.card.value}px",
                "${HTML_THEME_PREFIX}stroke" to "${Stretch.stroke.value}px",
            )
        return HtmlLook(c.surfaceFloat.luminance() < DARK_LUMINANCE, c.ink.toCss(), variables)
    }
