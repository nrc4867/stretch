package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.awt.SwingDialog
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import dev.chieppa.stretch.theme.LocalUiScale
import dev.chieppa.stretch.theme.UiScale
import dev.chieppa.stretch.ui.newPopupWindow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import java.awt.MouseInfo
import java.awt.Rectangle
import javax.swing.RootPaneContainer
import kotlin.math.ceil
import kotlin.math.roundToInt

/** The room a drag float's window keeps around its content for the content's shadow. */
private const val DRAG_FLOAT_MARGIN = 32f

/** How often a window drag looks at the pointer, in milliseconds. */
private const val DRAG_POLL_MILLIS = 40L

/**
 * What the hosts of drawer and pane windows share about the AWT window they belong to: where its content is on
 * screen, and a floating window that follows a drag past it.
 */
internal class OwnerWindow(val window: java.awt.Window) {
    /** The window's pixels per screen dp. */
    val scale: Float get() = window.graphicsConfiguration.defaultTransform.scaleX.toFloat()

    /** Where [point], in the window's pixels, is on screen, in the dp of [WindowBounds]. */
    fun screenPosition(point: Offset): Offset {
        val origin = contentOrigin()
        return Offset(point.x / scale + origin.x, point.y / scale + origin.y)
    }

    /** Where [screen], in the dp of [WindowBounds], is in the window's pixels. */
    fun windowPosition(screen: Offset): Offset {
        val origin = contentOrigin()
        return Offset((screen.x - origin.x) * scale, (screen.y - origin.y) * scale)
    }

    /** [content], a [size] square, in a window of its own above every window at [at] on screen, or hidden for null. */
    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun DragFloat(at: Offset?, size: Dp, content: @Composable () -> Unit) {
        val uiScale = LocalUiScale.current
        val currentContent by rememberUpdatedState(content)
        SwingDialog(
            visible = at != null,
            create = { newPopupWindow(window, focusable = false).apply { isAlwaysOnTop = true } },
            dispose = ComposeDialog::dispose,
            update = { dialog -> if (at != null) dialog.bounds = dragFloatBounds(at, size.value, uiScale) },
        ) {
            UiScale(uiScale) {
                Box(Modifier.fillMaxSize().padding(DRAG_FLOAT_MARGIN.dp)) { currentContent() }
            }
        }
    }

    private fun contentOrigin() = ((window as? RootPaneContainer)?.contentPane ?: window).locationOnScreen
}

/** The pointer on screen, in the dp of [WindowBounds], if the OS tells. */
internal fun pointerOnScreen(): Offset? = MouseInfo.getPointerInfo()?.location?.let { Offset(it.x.toFloat(), it.y.toFloat()) }

/**
 * Follows drags of a window by its title bar, seen as [position] changing while the primary button is held. While
 * the window moves, [over] gets the pointer on screen and says whether a place there takes the window; following
 * stops once it says no, and [over] then gets null. Letting go over such a place calls [over] with null and then
 * [drop] with the pointer.
 */
@Composable
internal fun FollowWindowDrag(position: () -> WindowPosition, over: (screen: Offset?) -> Boolean, drop: (screen: Offset) -> Unit) {
    val currentPosition by rememberUpdatedState(position)
    val currentOver by rememberUpdatedState(over)
    val currentDrop by rememberUpdatedState(drop)
    LaunchedEffect(Unit) {
        watchPrimaryButton()
        snapshotFlow { currentPosition() }.drop(1).collectLatest {
            val movedAt = System.nanoTime()
            if (!windowDragGoingOn(movedAt)) return@collectLatest
            while (true) {
                val pointer = pointerOnScreen()
                if (pointer == null || !currentOver(pointer)) {
                    currentOver(null)
                    return@collectLatest
                }
                if (!windowDragGoingOn(movedAt)) {
                    currentOver(null)
                    currentDrop(pointer)
                    return@collectLatest
                }
                delay(DRAG_POLL_MILLIS)
            }
        }
    }
}

/** The window of an [OwnerWindow.DragFloat] whose content is [size] dp at [at], with room for its shadow. */
internal fun dragFloatBounds(at: Offset, size: Float, scale: Float): Rectangle {
    val margin = DRAG_FLOAT_MARGIN * scale
    val side = ceil((size + 2 * DRAG_FLOAT_MARGIN) * scale).toInt()
    return Rectangle((at.x - margin).roundToInt(), (at.y - margin).roundToInt(), side, side)
}
