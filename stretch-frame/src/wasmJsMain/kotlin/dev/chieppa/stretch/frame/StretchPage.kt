package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.ComposeViewport
import dev.chieppa.stretch.ui.focusHome
import kotlinx.browser.document

private const val FULL = "100%"
private const val NO_SPACE = "0"
private const val HIDDEN = "hidden"

/** Compose pixels per CSS pixel at the page's root, whatever interface size a theme puts on the content inside. */
internal val LocalPageDensity = staticCompositionLocalOf { 1f }

/**
 * Runs the app as this page: [content] fills the browser's viewport and the tab is titled [title]. The page's
 * counterpart of a window; there are no pop-out windows here, so drawers stay docked. Clicking empty space takes focus
 * off fields and buttons.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun stretchPage(title: String, content: @Composable () -> Unit) {
    document.title = title
    val root = checkNotNull(document.documentElement as? org.w3c.dom.HTMLElement)
    val body = checkNotNull(document.body)
    root.style.height = FULL
    body.style.height = FULL
    body.style.margin = NO_SPACE
    body.style.overflowX = HIDDEN
    body.style.overflowY = HIDDEN
    ComposeViewport(body) {
        CompositionLocalProvider(LocalPageDensity provides LocalDensity.current.density) {
            Box(Modifier.fillMaxSize().focusHome()) { content() }
        }
    }
}

/** The plain [TopBar]: a page has no title bar to stand in for, and its menus stay in the bar on every OS. */
@Composable
actual fun WindowTopBar(
    appName: String,
    document: String,
    modifier: Modifier,
    edited: Boolean,
    menus: @Composable () -> Unit,
    modes: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
) = TopBar(appName, document, modifier, edited, menus, modes, actions)
