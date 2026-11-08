package dev.chieppa.stretch.test

import androidx.compose.runtime.Composable
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.platform.FrameRecomposer
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.coroutineContext

/** Frames rolled after composing and after each pointer move, enough for hover to settle. */
private const val SETTLE_FRAMES = 3

/** Nanoseconds between the probe's frames, one at 60 frames a second. */
private const val FRAME_NANOS = 16_666_667L

/**
 * Lays out [content] at [size] pixels in a scene with no window, moves the mouse to each of [points] in turn, and
 * returns the pointer icon the content asked for after each move. A test checks what someone would see hovering
 * there, such as a button's arrow inside a draggable header.
 */
@OptIn(InternalComposeUiApi::class)
fun pointerIconsAt(size: IntSize, points: List<Offset>, content: @Composable () -> Unit): List<PointerIcon?> = runBlocking {
    var shown: PointerIcon? = null
    val context = object : PlatformContext by PlatformContext.Empty() {
        override fun setPointerIcon(pointerIcon: PointerIcon) {
            shown = pointerIcon
        }
    }
    val recomposer = FrameRecomposer(coroutineContext)
    val scene = CanvasLayersComposeScene(recomposer, size = size, platformContext = context)
    var frame = 0L
    fun settle() = repeat(SETTLE_FRAMES) {
        recomposer.performFrame(++frame * FRAME_NANOS)
        scene.measureAndLayout()
    }
    try {
        scene.setContent(content = content)
        settle()
        points.map { at ->
            scene.sendPointerEvent(PointerEventType.Move, at)
            settle()
            shown
        }
    } finally {
        scene.close()
        recomposer.close()
    }
}
