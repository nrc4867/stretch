package dev.chieppa.stretch.ui

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import java.awt.AWTEvent
import java.awt.Toolkit
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent

private val keyboard = mutableStateOf(false)

internal actual val keyboardInput: State<Boolean> = keyboard.also {
    runCatching {
        Toolkit.getDefaultToolkit().addAWTEventListener(
            { event ->
                when (event.id) {
                    KeyEvent.KEY_PRESSED -> keyboard.value = true
                    MouseEvent.MOUSE_PRESSED -> keyboard.value = false
                }
            },
            AWTEvent.KEY_EVENT_MASK or AWTEvent.MOUSE_EVENT_MASK,
        )
    }
}
