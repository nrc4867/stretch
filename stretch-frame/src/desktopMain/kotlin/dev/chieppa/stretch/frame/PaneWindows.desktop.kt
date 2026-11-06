package dev.chieppa.stretch.frame

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import dev.chieppa.stretch.theme.LocalUiScale
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.UiScale
import dev.chieppa.stretch.ui.focusHome
import kotlinx.coroutines.flow.drop

/**
 * Pane windows as top-level frames with no owner, so each has its own taskbar button and place in the window list,
 * and the app's window can sit above it. On the JetBrains Runtime the pane window's header is its title bar.
 */
internal class DesktopPaneWindows(owner: java.awt.Window) : PaneWindowHost {
    private val ownerWindow = OwnerWindow(owner)

    @Composable
    override fun DragFloat(at: Offset?, size: Dp, content: @Composable () -> Unit) = ownerWindow.DragFloat(at, size, content)

    override fun screenPosition(point: Offset): Offset = ownerWindow.screenPosition(point)

    @Composable
    override fun PaneWindowFrame(
        title: String,
        bounds: WindowBounds,
        raise: Int,
        onBoundsChange: (WindowBounds) -> Unit,
        onClose: () -> Unit,
        onDragOver: (screen: Offset?) -> Boolean,
        onDrop: (screen: Offset) -> Unit,
        content: @Composable () -> Unit,
    ) {
        val state = rememberWindowState(
            position = WindowPosition(bounds.x.dp, bounds.y.dp),
            size = DpSize(bounds.width.dp, bounds.height.dp),
        )
        val report by rememberUpdatedState(onBoundsChange)
        FollowWindowDrag(position = { state.position }, over = onDragOver, drop = onDrop)
        LaunchedEffect(state) {
            snapshotFlow { state.position to state.size }.drop(1).collect { (position, size) ->
                if (position is WindowPosition.Absolute) {
                    report(WindowBounds(position.x.value, position.y.value, size.width.value, size.height.value))
                }
            }
        }
        val scale = LocalUiScale.current
        Window(onCloseRequest = onClose, state = state, title = title) {
            LaunchedEffect(window) { AppIdentity.current?.icons?.takeIf { it.isNotEmpty() }?.let { window.iconImages = it } }
            LaunchedEffect(raise) { if (raise > 0) window.toFront() }
            NativePointerInput(window)
            val binding = rememberTitleBarBinding(window)
            val inner = remember(window) { DesktopPaneWindows(window) }
            UiScale(scale) {
                CompositionLocalProvider(LocalTitleBar provides binding, LocalPaneWindows provides inner, LocalDrawerWindows provides null) {
                    Box(Modifier.fillMaxSize().background(Stretch.colors.surfacePanel).focusHome()) { content() }
                }
            }
        }
    }
}
