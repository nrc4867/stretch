package dev.chieppa.stretch.frame

/**
 * A crash of one of the app's windows, from composing, measuring, drawing, input or an effect: its [cause], the tab
 * ids of a [PaneGrid] held back for it, and whether the window [closed] since building it again kept crashing.
 */
data class WindowCrash(val cause: Throwable, val held: List<String>, val closed: Boolean)
