package dev.chieppa.stretch.frame

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import dev.chieppa.stretch.frame.generated.Res
import dev.chieppa.stretch.frame.generated.close_window
import dev.chieppa.stretch.frame.generated.maximize
import dev.chieppa.stretch.frame.generated.minimize
import dev.chieppa.stretch.frame.generated.restore
import dev.chieppa.stretch.theme.Icon
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchIcons
import dev.chieppa.stretch.theme.StretchSizes
import dev.chieppa.stretch.ui.focusHome
import org.jetbrains.compose.resources.stringResource
import java.awt.Cursor
import java.awt.MouseInfo
import java.awt.Rectangle

/**
 * A window without the system title bar, so the Stretch top bar is the only bar: put a [WindowTopBar] at the top of
 * [content]. The window still resizes from its edges and corners, and the top bar drags it, double-clicks to maximize
 * and holds the minimize, maximize and close buttons. On Windows the corners are rounded by the shell, like every
 * other window, and maximizing stops at the taskbar. Clicking empty space takes focus off fields and buttons.
 */
@Composable
fun StretchWindow(
    onCloseRequest: () -> Unit,
    title: String,
    state: WindowState = rememberWindowState(),
    icon: Painter? = null,
    content: @Composable FrameWindowScope.() -> Unit,
) {
    Window(onCloseRequest = onCloseRequest, state = state, title = title, icon = icon, undecorated = true) {
        DisposableEffect(window) {
            followSystemShell(window)
            onDispose {}
        }
        Box(Modifier.fillMaxSize().focusHome()) {
            content()
            if (state.placement == WindowPlacement.Floating) ResizeEdges(this@Window)
        }
    }
}

/** The [TopBar] as the window's title bar: it drags the window and ends with the window buttons. */
@Composable
fun FrameWindowScope.WindowTopBar(
    appName: String,
    document: String,
    state: WindowState,
    onCloseRequest: () -> Unit,
    modifier: Modifier = Modifier,
    modes: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    WindowDraggableArea(
        modifier.pointerInput(state) {
            detectTapGestures(onDoubleTap = { state.toggleMaximized() })
        },
    ) {
        TopBar(appName, document, modes = modes) {
            actions()
            WindowButtons(state, onCloseRequest)
        }
    }
}

private fun WindowState.toggleMaximized() {
    placement = if (placement == WindowPlacement.Maximized) WindowPlacement.Floating else WindowPlacement.Maximized
}

@Composable
private fun RowScope.WindowButtons(state: WindowState, onClose: () -> Unit) {
    val c = Stretch.colors
    Box(Modifier.width(8.dp))
    WindowButton(StretchIcons.Minus, stringResource(Res.string.minimize), c.surfaceControl, c.inkStrong) { state.isMinimized = true }
    val maximized = state.placement == WindowPlacement.Maximized
    WindowButton(
        if (maximized) StretchIcons.Restore else StretchIcons.Maximize,
        stringResource(if (maximized) Res.string.restore else Res.string.maximize),
        c.surfaceControl,
        c.inkStrong,
    ) { state.toggleMaximized() }
    WindowButton(StretchIcons.Close, stringResource(Res.string.close_window), c.errorWash, c.errorInk, onClose)
}

@Composable
private fun WindowButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, hoverFill: Color, hoverInk: Color, onClick: () -> Unit) {
    val c = Stretch.colors
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    Box(
        Modifier
            .size(width = 44.dp, height = StretchSizes.header)
            .background(if (hovered) hoverFill else Color.Transparent)
            .hoverable(source)
            .clickable(source, null, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, if (hovered) hoverInk else c.inkControl) }
}

private enum class Edge(val left: Boolean = false, val top: Boolean = false, val right: Boolean = false, val bottom: Boolean = false, val cursor: Int) {
    Left(left = true, cursor = Cursor.W_RESIZE_CURSOR),
    Right(right = true, cursor = Cursor.E_RESIZE_CURSOR),
    Top(top = true, cursor = Cursor.N_RESIZE_CURSOR),
    Bottom(bottom = true, cursor = Cursor.S_RESIZE_CURSOR),
    TopLeft(left = true, top = true, cursor = Cursor.NW_RESIZE_CURSOR),
    TopRight(right = true, top = true, cursor = Cursor.NE_RESIZE_CURSOR),
    BottomLeft(left = true, bottom = true, cursor = Cursor.SW_RESIZE_CURSOR),
    BottomRight(right = true, bottom = true, cursor = Cursor.SE_RESIZE_CURSOR),
}

/** Invisible strips along the window's edges and corners that resize it, since an undecorated window has no border. */
@Composable
private fun BoxScope.ResizeEdges(scope: FrameWindowScope) {
    val window = scope.window
    val thick = 5.dp
    val corner = 10.dp
    @Composable
    fun edge(e: Edge, modifier: Modifier) {
        Box(
            modifier
                .pointerHoverIcon(PointerIcon(Cursor(e.cursor)))
                .pointerInput(e) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val start = window.bounds
                        val from = MouseInfo.getPointerInfo().location
                        val min = window.minimumSize
                        drag(down.id) { ch ->
                            ch.consume()
                            val now = MouseInfo.getPointerInfo().location
                            val dx = now.x - from.x
                            val dy = now.y - from.y
                            val r = Rectangle(start)
                            if (e.right) r.width = maxOf(min.width, start.width + dx)
                            if (e.bottom) r.height = maxOf(min.height, start.height + dy)
                            if (e.left) {
                                r.width = maxOf(min.width, start.width - dx)
                                r.x = start.x + start.width - r.width
                            }
                            if (e.top) {
                                r.height = maxOf(min.height, start.height - dy)
                                r.y = start.y + start.height - r.height
                            }
                            window.bounds = r
                        }
                    }
                },
        )
    }
    edge(Edge.Left, Modifier.align(Alignment.CenterStart).width(thick).fillMaxHeight())
    edge(Edge.Right, Modifier.align(Alignment.CenterEnd).width(thick).fillMaxHeight())
    edge(Edge.Top, Modifier.align(Alignment.TopCenter).height(thick).fillMaxWidth())
    edge(Edge.Bottom, Modifier.align(Alignment.BottomCenter).height(thick).fillMaxWidth())
    edge(Edge.TopLeft, Modifier.align(Alignment.TopStart).size(corner))
    edge(Edge.TopRight, Modifier.align(Alignment.TopEnd).size(corner))
    edge(Edge.BottomLeft, Modifier.align(Alignment.BottomStart).size(corner))
    edge(Edge.BottomRight, Modifier.align(Alignment.BottomEnd).size(corner))
}
