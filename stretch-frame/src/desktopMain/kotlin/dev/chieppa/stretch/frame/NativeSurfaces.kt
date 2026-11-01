package dev.chieppa.stretch.frame

/**
 * Lets a [NativeSurface] share a window with Compose UI. Call [enable] at the start of `main`, before the first window
 * opens. Compose then draws popups, menus, tooltips and dialogs in OS windows of their own, so they show over a native
 * surface, and on Windows draws through OpenGL, the renderer that mode works with there. Without it a native surface
 * still renders, but popups that reach over it are hidden behind it. A property already set on the command line wins.
 */
object NativeSurfaces {
    private const val LAYERS = "compose.layers.type"
    private const val RENDER_API = "skiko.renderApi"

    fun enable() {
        if (System.getProperty(LAYERS) == null) System.setProperty(LAYERS, "WINDOW")
        if (isWindows && System.getProperty(RENDER_API) == null) System.setProperty(RENDER_API, "OPENGL")
    }

    /** Whether popups open in windows of their own, so they show over native surfaces. */
    val enabled: Boolean get() = System.getProperty(LAYERS).equals("WINDOW", ignoreCase = true)
}
