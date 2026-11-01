package dev.chieppa.stretch.frame

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.jetbrains.JBR
import com.jetbrains.WindowDecorations
import dev.chieppa.stretch.theme.StretchColors
import java.awt.Frame

private val os = System.getProperty("os.name").orEmpty()

/** Room the OS's own title bar buttons take, in dp: on the left on macOS, on the right on Windows. */
internal data class TitleBarInsets(val start: Float, val end: Float)

/**
 * The native frame's title area, hidden so the app draws the title bar while the OS keeps maximize, snapping,
 * resizing, shadow, rounding and its window buttons. Needs the JetBrains Runtime on Windows or macOS; elsewhere
 * [supported] is false and the window keeps the system title bar.
 */
internal class NativeTitleBar(private val frame: Frame) {
    val supported: Boolean = (os.startsWith("Windows") || os.startsWith("Mac")) &&
        runCatching { JBR.isAvailable() && JBR.isWindowDecorationsSupported() }.getOrDefault(false)

    private var bar: WindowDecorations.CustomTitleBar? = null

    /** Hides the system title area above a bar [height] dp tall and tints the OS buttons to the theme. */
    fun install(height: Float, dark: Boolean, colors: StretchColors): TitleBarInsets {
        val decorations = JBR.getWindowDecorations()
        val next = decorations.createCustomTitleBar()
        next.height = height
        next.putProperty("controls.dark", dark)
        next.putProperty("controls.foreground.normal", colors.inkControl.awt())
        next.putProperty("controls.foreground.hovered", colors.inkStrong.awt())
        next.putProperty("controls.foreground.pressed", colors.inkStrong.awt())
        next.putProperty("controls.foreground.inactive", colors.inkMuted.awt())
        next.putProperty("controls.background.hovered", colors.surfaceControl.awt())
        next.putProperty("controls.background.pressed", colors.surfaceControlHi.awt())
        decorations.setCustomTitleBar(frame, next)
        bar = next
        return TitleBarInsets(next.leftInset, next.rightInset)
    }

    /** Brings back the system title bar. */
    fun remove() {
        if (bar == null) return
        JBR.getWindowDecorations().setCustomTitleBar(frame, null)
        bar = null
    }

    /** Tells the OS whether the pointer is over an app control (true) or over bar it should handle (false). */
    fun hitTest(control: Boolean) {
        bar?.forceHitTest(control)
    }
}

private fun Color.awt() = java.awt.Color(toArgb(), true)
