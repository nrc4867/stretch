package dev.chieppa.stretch.ui

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import java.awt.AWTEvent
import java.awt.Toolkit
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent

private val keyboard = mutableStateOf(false)

private val focusKeys = setOf(
    KeyEvent.VK_TAB, KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
    KeyEvent.VK_ENTER, KeyEvent.VK_SPACE, KeyEvent.VK_ESCAPE, KeyEvent.VK_HOME, KeyEvent.VK_END,
)

internal actual val keyboardInput: State<Boolean> = keyboard.also {
    runCatching {
        Toolkit.getDefaultToolkit().addAWTEventListener(
            { event ->
                when (event.id) {
                    KeyEvent.KEY_PRESSED -> {
                        val key = event as KeyEvent
                        if (key.keyCode in focusKeys && !key.isControlDown && !key.isAltDown && !key.isMetaDown) keyboard.value = true
                    }
                    MouseEvent.MOUSE_PRESSED -> keyboard.value = false
                }
            },
            AWTEvent.KEY_EVENT_MASK or AWTEvent.MOUSE_EVENT_MASK,
        )
    }
}
