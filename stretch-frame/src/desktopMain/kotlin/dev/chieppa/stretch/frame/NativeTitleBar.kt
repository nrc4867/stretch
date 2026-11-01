package dev.chieppa.stretch.frame

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.jetbrains.JBR
import com.jetbrains.WindowDecorations
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchColors
import dev.chieppa.stretch.theme.StretchSizes
import java.awt.Dialog
import java.awt.Frame
import java.awt.Window

private val os = System.getProperty("os.name").orEmpty()

/** Room the OS's own title bar buttons take, in dp: on the left on macOS, on the right on Windows. */
internal data class TitleBarInsets(val start: Float, val end: Float)

/**
 * The native frame's title area, hidden so the app draws the title bar while the OS keeps maximize, snapping,
 * resizing, shadow, rounding and its window buttons. Works for frames and dialogs. Needs the JetBrains Runtime on
 * Windows or macOS; elsewhere [supported] is false and the window keeps the system title bar.
 */
internal class NativeTitleBar(private val window: Window) {
    val supported: Boolean = (window is Frame || window is Dialog) &&
        (os.startsWith("Windows") || os.startsWith("Mac")) &&
        runCatching { JBR.isAvailable() && JBR.isWindowDecorationsSupported() }.getOrDefault(false)

    private var bar: WindowDecorations.CustomTitleBar? = null

    /** Hides the system title area above a bar [height] dp tall and tints the OS buttons to the theme. */
    fun install(height: Float, dark: Boolean, colors: StretchColors): TitleBarInsets {
        val next = JBR.getWindowDecorations().createCustomTitleBar()
        next.height = height
        next.putProperty("controls.dark", dark)
        next.putProperty("controls.foreground.normal", colors.inkControl.awt())
        next.putProperty("controls.foreground.hovered", colors.inkStrong.awt())
        next.putProperty("controls.foreground.pressed", colors.inkStrong.awt())
        next.putProperty("controls.foreground.inactive", colors.inkMuted.awt())
        next.putProperty("controls.background.hovered", colors.surfaceControl.awt())
        next.putProperty("controls.background.pressed", colors.surfaceControlHi.awt())
        set(next)
        bar = next
        return TitleBarInsets(next.leftInset, next.rightInset)
    }

    /** Brings back the system title bar. */
    fun remove() {
        if (bar == null) return
        set(null)
        bar = null
    }

    /** Tells the OS whether the pointer is over an app control (true) or over bar it should handle (false). */
    fun hitTest(control: Boolean) {
        bar?.forceHitTest(control)
    }

    private fun set(titleBar: WindowDecorations.CustomTitleBar?) {
        val decorations = JBR.getWindowDecorations()
        when (window) {
            is Frame -> decorations.setCustomTitleBar(window, titleBar)
            is Dialog -> decorations.setCustomTitleBar(window, titleBar)
        }
    }
}

/**
 * Keeps the custom title bar installed while this is composed, tinted to the current theme, and returns the room
 * the OS's buttons take. The system title bar comes back when it leaves.
 */
@Composable
internal fun NativeTitleBar.installed(): TitleBarInsets {
    val theme = Stretch.theme
    var insets by remember { mutableStateOf(TitleBarInsets(0f, 0f)) }
    DisposableEffect(this) { onDispose { remove() } }
    DisposableEffect(this, theme) {
        insets = install(StretchSizes.header.value, theme.isDark, theme.colors)
        onDispose {}
    }
    return insets
}

private fun Color.awt() = java.awt.Color(toArgb(), true)
