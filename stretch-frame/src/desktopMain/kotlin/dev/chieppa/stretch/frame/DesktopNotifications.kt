package dev.chieppa.stretch.frame

import java.awt.AWTException
import java.awt.Color
import java.awt.GraphicsEnvironment
import java.awt.Image
import java.awt.RenderingHints
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage

/**
 * Notifications the OS shows outside the app's windows, such as a toast on Windows or a banner on macOS. The first one
 * puts the app's icon in the system tray (the menu bar's status area on macOS), with the app's name as its tooltip, and
 * it stays until [close]. A click on the notification or the icon runs the `onClick` of the latest one shown. Where the
 * OS offers no tray, as on a machine without a display, nothing shows and [show] returns false.
 */
object DesktopNotifications {
    private const val ICON_SIZE = 16
    private const val ICON_INSET = 2
    private val ICON_FILL = Color(0x3987e5)

    private var icon: TrayIcon? = null
    private var onClick: (() -> Unit)? = null

    /** Whether notifications can show here. */
    val available: Boolean
        get() = !GraphicsEnvironment.isHeadless() && SystemTray.isSupported()

    /** Shows [title] and [text] as a notification; false when the OS has nowhere to show it. */
    @Synchronized
    fun show(title: String, text: String, onClick: (() -> Unit)? = null): Boolean {
        val tray = trayIcon() ?: return false
        this.onClick = onClick
        tray.displayMessage(title, text, TrayIcon.MessageType.NONE)
        return true
    }

    /** Takes the app's icon out of the tray. */
    @Synchronized
    fun close() {
        icon?.let { SystemTray.getSystemTray().remove(it) }
        icon = null
        onClick = null
    }

    private fun trayIcon(): TrayIcon? {
        icon?.let { return it }
        if (!available) return null
        val identity = AppIdentity.current
        val tray = TrayIcon(identity?.icons?.firstOrNull() ?: plainIcon(), identity?.name.orEmpty()).apply {
            isImageAutoSize = true
            addActionListener { onClick?.invoke() }
        }
        return try {
            SystemTray.getSystemTray().add(tray)
            tray.also { icon = it }
        } catch (_: AWTException) {
            null
        }
    }

    /** A filled dot, for an app that has no icon of its own. */
    private fun plainIcon(): Image = BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB).apply {
        val g = createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.color = ICON_FILL
        g.fillOval(ICON_INSET, ICON_INSET, ICON_SIZE - 2 * ICON_INSET, ICON_SIZE - 2 * ICON_INSET)
        g.dispose()
    }
}
