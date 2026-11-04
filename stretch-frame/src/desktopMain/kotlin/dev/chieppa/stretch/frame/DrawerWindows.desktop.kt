package dev.chieppa.stretch.frame

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.awt.SwingDialog
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
import dev.chieppa.stretch.theme.UiScale
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.ui.focusHome
import dev.chieppa.stretch.ui.newPopupWindow
import kotlinx.coroutines.delay
import com.sun.jna.platform.win32.User32
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import java.awt.MouseInfo
import java.awt.Rectangle
import javax.swing.RootPaneContainer
import kotlin.math.ceil
import kotlin.math.roundToInt

/** The room a drag float's window keeps around its content for the content's shadow. */
private const val DRAG_FLOAT_MARGIN = 32f

/** How far inside the app's window a drawer's window first opens. */
private const val INSET = 56f

/**
 * Popped-out drawers as tool windows of the app's window: modeless, above it and minimized with it, with the drawer's
 * header as their title bar on the JetBrains Runtime.
 */
internal class DesktopDrawerWindows(private val owner: java.awt.Window) : DrawerWindowHost {
    private val rails = mutableMapOf<Side, Rect>()
    private var target by mutableStateOf<DockTarget?>(null)

    override val dockTarget: DockTarget? get() = target

    override fun railBounds(side: Side, bounds: Rect?) {
        if (bounds == null) rails.remove(side) else rails[side] = bounds
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun DragFloat(at: Offset?, size: Dp, content: @Composable () -> Unit) {
        val scale = LocalUiScale.current
        val currentContent by rememberUpdatedState(content)
        SwingDialog(
            visible = at != null,
            create = { newPopupWindow(owner, focusable = false).apply { isAlwaysOnTop = true } },
            dispose = ComposeDialog::dispose,
            update = { dialog -> if (at != null) dialog.bounds = dragFloatBounds(at, size.value, scale) },
        ) {
            UiScale(scale) {
                Box(Modifier.fillMaxSize().padding(DRAG_FLOAT_MARGIN.dp)) { currentContent() }
            }
        }
    }

    override fun screenPosition(point: Offset): Offset {
        val origin = contentOrigin()
        val scale = owner.graphicsConfiguration.defaultTransform.scaleX.toFloat()
        return Offset(point.x / scale + origin.x, point.y / scale + origin.y)
    }

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
        LaunchedEffect(state) {
            snapshotFlow { state.position }.drop(1).collectLatest {
                if (!dragging(System.nanoTime())) return@collectLatest
                val movedAt = System.nanoTime()
                // Wait for the drag to end, following the pointer over the rails.
                while (true) {
                    val over = targetUnderPointer()
                    target = over
                    if (over == null) return@collectLatest
                    if (!dragging(movedAt)) {
                        target = null
                        dock(over.side, over.bottom)
                        return@collectLatest
                    }
                    delay(40)
                }
            }
        }
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
            val titleBar = remember(window) { NativeTitleBar(window) }
            val binding = if (titleBar.supported) {
                val insets = titleBar.installed()
                val regions = remember { TitleBarRegions() }
                TitleBarBinding(regions, titleBar::hitTest, insets.start.dp, insets.end.dp)
            } else {
                null
            }
            UiScale(scale) {
                CompositionLocalProvider(LocalTitleBar provides binding) {
                    Box(Modifier.fillMaxSize().background(Stretch.colors.surfacePanel).focusHome()) { content() }
                }
            }
        }
    }

    /** The rail and group under the pointer, with some slack beside the rail since it is narrow. */
    private fun targetUnderPointer(): DockTarget? {
        if (!owner.isShowing) return null
        val pointer = MouseInfo.getPointerInfo()?.location ?: return null
        val origin = contentOrigin()
        val scale = owner.graphicsConfiguration.defaultTransform.scaleX.toFloat()
        val at = Offset((pointer.x - origin.x) * scale, (pointer.y - origin.y) * scale)
        val slack = 24 * scale
        return rails.entries.firstOrNull { (_, r) -> Rect(r.left - slack, r.top, r.right + slack, r.bottom).contains(at) }
            ?.let { (side, r) -> DockTarget(side, at.y > r.center.y) }
    }

    private fun contentOrigin() = ((owner as? RootPaneContainer)?.contentPane ?: owner).locationOnScreen

    /**
     * Whether a window drag is still going on. Windows reports the mouse button; elsewhere a drag counts as over once
     * the window has not moved for a moment since [movedAt].
     */
    private fun dragging(movedAt: Long): Boolean = if (isWindows) {
        val button = if (User32.INSTANCE.GetSystemMetrics(SM_SWAPBUTTON) != 0) VK_RBUTTON else VK_LBUTTON
        User32.INSTANCE.GetAsyncKeyState(button).toInt() and 0x8000 != 0
    } else {
        System.nanoTime() - movedAt < SETTLE_NANOS
    }

    /** Inside the app's window on the drawer's side, below its top bar. */
    private fun firstPosition(side: Side, width: Float): WindowPosition {
        val at = owner.bounds
        val x = if (side == Side.Left) at.x + INSET else at.x + at.width - INSET - width
        return WindowPosition(x.dp, (at.y + INSET * 2).dp)
    }
}

/** The window of a [DesktopDrawerWindows.DragFloat] whose content is [size] dp at [at], with room for its shadow. */
internal fun dragFloatBounds(at: Offset, size: Float, scale: Float): Rectangle {
    val margin = DRAG_FLOAT_MARGIN * scale
    val side = ceil((size + 2 * DRAG_FLOAT_MARGIN) * scale).toInt()
    return Rectangle((at.x - margin).roundToInt(), (at.y - margin).roundToInt(), side, side)
}

private const val SM_SWAPBUTTON = 23
private const val VK_LBUTTON = 1
private const val VK_RBUTTON = 2
private const val SETTLE_NANOS = 350_000_000L
