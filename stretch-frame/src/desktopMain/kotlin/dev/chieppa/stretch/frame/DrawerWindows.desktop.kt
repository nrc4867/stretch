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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogModalityType
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.ui.focusHome
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop

private const val DEFAULT_HEIGHT = 520f

/** How far inside the app's window a drawer's window first opens. */
private const val INSET = 56f

/**
 * Popped-out drawers as tool windows of the app's window: modeless, above it and minimized with it, with the drawer's
 * header as their title bar on the JetBrains Runtime.
 */
internal class DesktopDrawerWindows(private val owner: java.awt.Window) : DrawerWindowHost {
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
        content: @Composable () -> Unit,
    ) {
        val state = rememberDialogState(
            position = bounds?.let { WindowPosition(it.x.dp, it.y.dp) } ?: WindowPosition.PlatformDefault,
            size = DpSize((bounds?.width ?: defaultWidth).dp, (bounds?.height ?: DEFAULT_HEIGHT).dp),
        )
        val report by rememberUpdatedState(onBoundsChange)
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
        DialogWindow(onCloseRequest = onClose, state = state, title = title, modalityType = DialogModalityType.Modeless) {
            LaunchedEffect(raise) { if (raise > 0) window.toFront() }
            val titleBar = remember(window) { NativeTitleBar(window) }
            val binding = if (titleBar.supported) {
                val insets = titleBar.installed()
                val regions = remember { TitleBarRegions() }
                TitleBarBinding(regions, titleBar::hitTest, insets.start.dp, insets.end.dp)
            } else {
                null
            }
            CompositionLocalProvider(LocalTitleBar provides binding) {
                Box(Modifier.fillMaxSize().background(Stretch.colors.surfacePanel).focusHome()) { content() }
            }
        }
    }

    /** Inside the app's window on the drawer's side, below its top bar. */
    private fun firstPosition(side: Side, width: Float): WindowPosition {
        val at = owner.bounds
        val x = if (side == Side.Left) at.x + INSET else at.x + at.width - INSET - width
        return WindowPosition(x.dp, (at.y + INSET * 2).dp)
    }
}
