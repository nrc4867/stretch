package dev.chieppa.stretch.code

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Ctrl F (Cmd F) opens [state]'s find bar and calls [onOpen], F3 and Shift F3 move between matches, Esc closes it. */
internal fun Modifier.codeFindKeys(state: CodeViewerState, scope: CoroutineScope, onOpen: () -> Unit): Modifier =
    onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when {
            event.key == Key.F && (event.isCtrlPressed || event.isMetaPressed) -> {
                state.findOpen = true
                onOpen()
            }
            event.key == Key.F3 -> scope.launch { if (event.isShiftPressed) state.previousMatch() else state.nextMatch() }
            event.key == Key.Escape && state.findOpen -> state.findOpen = false
            else -> return@onPreviewKeyEvent false
        }
        true
    }
