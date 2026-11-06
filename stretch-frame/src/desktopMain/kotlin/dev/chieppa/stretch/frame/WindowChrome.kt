package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import dev.chieppa.stretch.ui.focusHome

private val LocalNativeTitleBar = staticCompositionLocalOf<NativeTitleBar?> { null }

/**
 * A system window whose title bar the app draws: put a [WindowTopBar] at the top of [content]. The OS still owns the
 * frame, so maximize, snapping, resizing, the shadow, rounded corners and the window buttons behave like every other
 * window. Needs the JetBrains Runtime on Windows or macOS; elsewhere the system title bar stays above the top bar.
 * Drawers in a [StretchApp] inside it can pop out into windows of their own, and tabs of a [PaneGrid] can tear out
 * into full windows of the app. Clicking empty space takes focus off
 * fields and buttons. Without an [icon] it uses the app's icon from the `dev.chieppa.stretch.app` Gradle plugin, at
 * every size, so the taskbar and Alt+Tab stay sharp; the process is identified to the OS before the window opens.
 */
@Composable
fun StretchWindow(
    onCloseRequest: () -> Unit,
    title: String,
    state: WindowState = rememberWindowState(),
    icon: Painter? = null,
    content: @Composable FrameWindowScope.() -> Unit,
) {
    AppIdentity.applyToProcess()
    Window(onCloseRequest = onCloseRequest, state = state, title = title, icon = icon) {
        if (icon == null) {
            LaunchedEffect(window) { AppIdentity.current?.icons?.takeIf { it.isNotEmpty() }?.let { window.iconImages = it } }
        }
        NativePointerInput(window)
        val titleBar = remember(window) { NativeTitleBar(window) }
        val drawerWindows = remember(window) { DesktopDrawerWindows(window) }
        val paneWindows = remember(window) { DesktopPaneWindows(window) }
        CompositionLocalProvider(LocalNativeTitleBar provides titleBar, LocalDrawerWindows provides drawerWindows, LocalPaneWindows provides paneWindows) {
            Box(Modifier.fillMaxSize().focusHome()) { content() }
        }
    }
}

/**
 * On the desktop, the bar is the window's title bar: its empty space, app name and document drag the window,
 * double-click to maximize and open the system menu, and the OS draws the window buttons; [leading] and [center] are
 * controls that take clicks like the menus. On macOS [menus] stay out of the bar, since [MacMenuBar] puts the same
 * actions in the system's menu bar.
 */
@Composable
actual fun WindowTopBar(
    appName: String,
    document: String,
    modifier: Modifier,
    edited: Boolean,
    menus: @Composable () -> Unit,
    modes: @Composable () -> Unit,
    leading: @Composable () -> Unit,
    center: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    val titleBar = LocalNativeTitleBar.current
    if (titleBar == null || !titleBar.supported) {
        TopBar(appName, document, modifier, edited, if (isMac) ({}) else menus, modes, leading, center, actions)
        return
    }
    val insets = titleBar.installed()
    val regions = remember { TitleBarRegions() }
    TopBarLayout(
        appName,
        document,
        modifier,
        barModifier = Modifier.titleBarHitTest(regions, titleBar::hitTest),
        start = insets.start.dp,
        end = insets.end.dp,
        edited = edited,
        menus = { if (!isMac) Box(Modifier.titleBarControl(regions, "menus")) { menus() } },
        modes = { Box(Modifier.titleBarControl(regions, "modes")) { modes() } },
        leading = { Box(Modifier.titleBarControl(regions, "leading")) { leading() } },
        center = { Box(Modifier.titleBarControl(regions, "center")) { center() } },
        onCenterShown = { shown -> if (!shown) regions.remove("center") },
        actions = {
            Row(
                Modifier.titleBarControl(regions, "actions"),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) { actions() }
        },
    )
}
