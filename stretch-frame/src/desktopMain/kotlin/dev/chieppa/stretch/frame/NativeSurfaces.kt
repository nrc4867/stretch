package dev.chieppa.stretch.frame

import dev.chieppa.stretch.ui.LAYERS_IN_WINDOWS
import dev.chieppa.stretch.ui.LAYERS_PROPERTY

/**
 * Lets a [NativeSurface] share a window with Compose UI. Call [enable] at the start of `main`, before the first window
 * opens. Stretch's menus, tooltips, popovers and dialogs then open in OS windows of their own (see
 * [dev.chieppa.stretch.ui.StretchPopup]), so they show over a native surface, and on Windows Compose draws through
 * OpenGL, the renderer that mode works with there. Without it a native surface still renders, but popups that reach
 * over it are hidden behind it. A property already set on the command line wins.
 */
object NativeSurfaces {
    private const val RENDER_API = "skiko.renderApi"

    fun enable() {
        if (System.getProperty(LAYERS_PROPERTY) == null) System.setProperty(LAYERS_PROPERTY, LAYERS_IN_WINDOWS)
        if (isWindows && System.getProperty(RENDER_API) == null) System.setProperty(RENDER_API, "OPENGL")
    }

    /** Whether popups open in windows of their own, so they show over native surfaces. */
    val enabled: Boolean get() = System.getProperty(LAYERS_PROPERTY).equals(LAYERS_IN_WINDOWS, ignoreCase = true)
}
