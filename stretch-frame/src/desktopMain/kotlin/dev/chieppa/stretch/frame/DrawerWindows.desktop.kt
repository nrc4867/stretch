package dev.chieppa.stretch.frame

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogModalityType
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import dev.chieppa.stretch.theme.LocalUiScale
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.UiScale
import dev.chieppa.stretch.ui.focusHome
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop

/** How far inside the app's window a drawer's window first opens. */
private const val INSET = 56f

/** How far beside a rail, in dp, a drawer window dragged by its title bar still docks on it. */
private const val RAIL_SLACK = 24f

/**
 * Popped-out drawers as tool windows of the app's window: modeless, above it and minimized with it, with the drawer's
 * header as their title bar on the JetBrains Runtime.
 */
internal class DesktopDrawerWindows(private val owner: java.awt.Window) : DrawerWindowHost {
    private val ownerWindow = OwnerWindow(owner)
    private val rails = mutableMapOf<Side, Rect>()
    private var target by mutableStateOf<DockTarget?>(null)

    override val dockTarget: DockTarget? get() = target

    override fun railBounds(side: Side, bounds: Rect?) {
        if (bounds == null) rails.remove(side) else rails[side] = bounds
    }

    @Composable
    override fun DragFloat(at: Offset?, size: Dp, content: @Composable () -> Unit) = ownerWindow.DragFloat(at, size, content)

    override fun screenPosition(point: Offset): Offset = ownerWindow.screenPosition(point)

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun DrawerWindow(
        title: String,
        bounds: WindowBounds?,
        side: Side,
        defaultWidth: Float,
        raise: Int,
        onBoundsChange: (WindowBounds) -> Unit,
        onClose: () -> Unit,
        onDock: (side: Side, bottom: Boolean) -> Unit,
        content: @Composable () -> Unit,
    ) {
        val state = rememberDialogState(
            position = bounds?.let { WindowPosition(it.x.dp, it.y.dp) } ?: WindowPosition.PlatformDefault,
            size = DpSize((bounds?.width ?: defaultWidth).dp, (bounds?.height ?: FrameLayout.DEFAULT_WINDOW_HEIGHT).dp),
        )
        val report by rememberUpdatedState(onBoundsChange)
        val dock by rememberUpdatedState(onDock)
        FollowWindowDrag(
            position = { state.position },
            over = { at -> target = at?.let(::targetAt); target != null },
            drop = { at -> targetAt(at)?.let { dock(it.side, it.bottom) } },
        )
        DisposableEffect(state) { onDispose { target = null } }
        LaunchedEffect(state) {
            if (bounds != null) return@LaunchedEffect
            while (!owner.isShowing) delay(16)
            state.position = firstPosition(side, defaultWidth)
        }
        LaunchedEffect(state) {
            snapshotFlow { state.position to state.size }.drop(1).collect { (position, size) ->
                if (position is WindowPosition.Absolute) {
                    report(WindowBounds(position.x.value, position.y.value, size.width.value, size.height.value))
                }
            }
        }
        val scale = LocalUiScale.current
        DialogWindow(onCloseRequest = onClose, state = state, title = title, modalityType = DialogModalityType.Modeless) {
            LaunchedEffect(raise) { if (raise > 0) window.toFront() }
            NativePointerInput(window)
            val binding = rememberTitleBarBinding(window)
            UiScale(scale) {
                CompositionLocalProvider(LocalTitleBar provides binding) {
                    Box(Modifier.fillMaxSize().background(Stretch.colors.surfacePanel).focusHome()) { content() }
                }
            }
        }
    }

    /** The rail and group under [screen], with some slack beside the rail since it is narrow. */
    private fun targetAt(screen: Offset): DockTarget? {
        if (!owner.isShowing) return null
        val at = ownerWindow.windowPosition(screen)
        val slack = RAIL_SLACK * ownerWindow.scale
        return rails.entries.firstOrNull { (_, r) -> Rect(r.left - slack, r.top, r.right + slack, r.bottom).contains(at) }
            ?.let { (side, r) -> DockTarget(side, at.y > r.center.y) }
    }

    /** Inside the app's window on the drawer's side, below its top bar. */
    private fun firstPosition(side: Side, width: Float): WindowPosition {
        val at = owner.bounds
        val x = if (side == Side.Left) at.x + INSET else at.x + at.width - INSET - width
        return WindowPosition(x.dp, (at.y + INSET * 2).dp)
    }
}
