package dev.chieppa.stretch.frame

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.theme.Stretch

/**
 * Follows a drag on a handle that moves as it resizes things. Positions are taken in the window, so the handle
 * moving under the pointer does not eat the drag. Reports the distance from where the drag started.
 */
internal fun Modifier.windowDrag(vertical: Boolean, onStart: () -> Unit, onEnd: () -> Unit = {}, onDrag: (Float) -> Unit): Modifier = composed {
    var origin by remember { mutableStateOf(Offset.Zero) }
    var startAt by remember { mutableFloatStateOf(0f) }
    val start by rememberUpdatedState(onStart)
    val drag by rememberUpdatedState(onDrag)
    val end by rememberUpdatedState(onEnd)
    onGloballyPositioned { origin = it.positionInWindow() }.pointerInput(vertical) {
        fun along(local: Offset) = if (vertical) origin.y + local.y else origin.x + local.x
        detectDragGestures(
            onDragStart = { at ->
                startAt = along(at)
                start()
            },
            onDragEnd = { end() },
            onDragCancel = { end() },
        ) { change, _ ->
            change.consume()
            drag(along(change.position) - startAt)
        }
    }
}

/**
 * The drag strip on a panel's inner edge: across the panel when [vertical] (dragged up and down), along it
 * otherwise. Reports the distance dragged in pixels since [onStart].
 */
@Composable
internal fun EdgeHandle(vertical: Boolean, description: String, onStart: () -> Unit, onDrag: (Float) -> Unit) {
    val c = Stretch.colors
    val drag by rememberUpdatedState(onDrag)
    var dragging by remember { mutableStateOf(false) }
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val active = hovered || dragging
    val line = if (active) 2.dp else Stretch.stroke
    Box(
        (if (vertical) Modifier.fillMaxWidth().height(6.dp) else Modifier.width(6.dp).fillMaxHeight())
            .background(c.surfacePanel)
            .hoverable(source)
            .pointerHoverIcon(if (vertical) resizeUpDown else resizeSideways)
            .windowDrag(vertical, onStart = { onStart(); dragging = true }, onEnd = { dragging = false }) { drag(it) }
            .semantics { contentDescription = description },
    ) {
        Box(
            (if (vertical) Modifier.fillMaxWidth().height(line) else Modifier.width(line).fillMaxHeight())
                .align(Alignment.Center)
                .background(if (active) c.ringSelected else c.linePanel),
        )
    }
}
