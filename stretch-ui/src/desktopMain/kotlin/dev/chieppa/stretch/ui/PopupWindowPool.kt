package dev.chieppa.stretch.ui

import androidx.compose.ui.awt.ComposeDialog
import java.awt.Window
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

/** How many hidden popup windows each window keeps. */
private const val MOST_KEPT = 16

/**
 * Hidden popup windows kept for reuse, per owner window, because making a window and its GPU context is slow. A kept
 * window is empty and goes away with its owner. Used on the UI thread only.
 */
internal object PopupWindowPool {
    private val kept = HashMap<Window, ArrayDeque<ComposeDialog>>()

    fun take(owner: Window): ComposeDialog? = kept[owner]?.removeLastOrNull()

    /** Hides and empties [dialog] to reuse it, or disposes it when its owner is gone or keeps enough already. */
    fun release(dialog: ComposeDialog) {
        val owner = dialog.owner
        if (!owner.isDisplayable || (kept[owner]?.size ?: 0) >= MOST_KEPT) {
            dialog.dispose()
            return
        }
        dialog.isVisible = false
        dialog.setContent {}
        kept.getOrPut(owner) { ArrayDeque<ComposeDialog>().also { forgetWhenClosed(owner) } }.addLast(dialog)
    }

    private fun forgetWhenClosed(owner: Window) {
        owner.addWindowListener(object : WindowAdapter() {
            override fun windowClosed(e: WindowEvent) {
                owner.removeWindowListener(this)
                kept.remove(owner)
            }
        })
    }
}
