package dev.chieppa.stretch.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.awt.LocalAwtWindow
import androidx.compose.ui.awt.SwingDialog
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.round
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import dev.chieppa.stretch.theme.StretchShadow
import java.awt.AWTEvent
import java.awt.Dialog
import java.awt.Dimension
import java.awt.Point
import java.awt.Toolkit
import java.awt.Window
import java.awt.event.AWTEventListener
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseEvent
import javax.swing.RootPaneContainer
import javax.swing.SwingUtilities
import kotlin.math.roundToInt

/** The system property Compose reads for where it draws popups. Its value [LAYERS_IN_WINDOWS] means OS windows. */
const val LAYERS_PROPERTY = "compose.layers.type"
const val LAYERS_IN_WINDOWS = "WINDOW"

private val popupsInOwnWindows: Boolean
    get() = System.getProperty("os.name").startsWith("Windows") &&
        System.getProperty(LAYERS_PROPERTY).equals(LAYERS_IN_WINDOWS, ignoreCase = true)

/** The window popups are placed in, for a popup opened from inside another popup's window. */
private val LocalPopupRoot = staticCompositionLocalOf<Window?> { null }

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun StretchPopup(
    popupPositionProvider: PopupPositionProvider,
    onDismissRequest: (() -> Unit)?,
    properties: PopupProperties,
    shadow: StretchShadow?,
    content: @Composable () -> Unit,
) {
    val owner = LocalAwtWindow.current
    if (owner == null || !popupsInOwnWindows) {
        Popup(popupPositionProvider, onDismissRequest, properties, content)
    } else {
        PopupWindow(owner, popupPositionProvider, onDismissRequest, properties, shadow, content)
    }
}

@Composable
private fun PopupWindow(
    owner: Window,
    provider: PopupPositionProvider,
    onDismiss: (() -> Unit)?,
    properties: PopupProperties,
    shadow: StretchShadow?,
    content: @Composable () -> Unit,
) {
    val root = LocalPopupRoot.current ?: owner
    val currentContent by rememberUpdatedState(content)
    val currentDismiss by rememberUpdatedState(onDismiss)
    val currentProperties by rememberUpdatedState(properties)
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val margins = PopupMargins.around(shadow)
    val placer = remember(owner, root) { PopupPlacer(owner, root, provider, density) }
    var anchored by remember { mutableStateOf(false) }
    // Only popups that hold their place share windows: a reused window keeps its stacking order, so one that opens over
    // other popups needs a new window to come out on top.
    val reusable = remember { !properties.focusable && !properties.dismissOnClickOutside }
    Layout(
        content = {},
        modifier = Modifier.onGloballyPositioned { coordinates ->
            coordinates.parentCoordinates?.let { parent ->
                placer.anchor = IntRect(parent.positionInWindow().round(), parent.size)
                anchored = true
                placer.place()
            }
        },
    ) { _, _ -> layout(0, 0) {} }
    SideEffect {
        placer.provider = provider
        placer.direction = direction
        placer.density = density
        placer.margins = margins
        placer.place()
    }
    DisposableEffect(placer) {
        val follow = object : ComponentAdapter() {
            override fun componentMoved(e: ComponentEvent) = placer.place()
            override fun componentResized(e: ComponentEvent) = placer.place()
            override fun componentShown(e: ComponentEvent) = placer.place()
        }
        val outside = AWTEventListener { event ->
            if (event is MouseEvent && event.id == MouseEvent.MOUSE_PRESSED && currentProperties.dismissOnClickOutside && placer.isOutside(event)) {
                currentDismiss?.invoke()
            }
        }
        root.addComponentListener(follow)
        if (owner !== root) owner.addComponentListener(follow)
        Toolkit.getDefaultToolkit().addAWTEventListener(outside, AWTEvent.MOUSE_EVENT_MASK)
        onDispose {
            root.removeComponentListener(follow)
            owner.removeComponentListener(follow)
            Toolkit.getDefaultToolkit().removeAWTEventListener(outside)
        }
    }
    if (!anchored) return
    SwingDialog(
        visible = placer.ready,
        onKeyEvent = { event ->
            val dismiss = currentProperties.dismissOnBackPress && event.type == KeyEventType.KeyDown && event.key == Key.Escape
            if (dismiss) currentDismiss?.invoke()
            dismiss
        },
        create = {
            val reused = if (reusable) PopupWindowPool.take(owner) else null
            reused ?: newPopupWindow(owner, currentProperties.focusable)
        },
        dispose = { if (reusable) PopupWindowPool.release(it) else it.dispose() },
        update = placer::attach,
    ) {
        CompositionLocalProvider(LocalPopupRoot provides root) {
            Box(
                Modifier
                    .wrapContentSize(Alignment.TopStart, unbounded = true)
                    .absolutePadding(margins.left, margins.top, margins.right, margins.bottom)
                    .onSizeChanged {
                        placer.contentSize = it
                        placer.place()
                    },
            ) { currentContent() }
        }
    }
}

/** A bare window for a popup. It is not resizable, or Compose would put drag handles that move it along its edges. */
fun newPopupWindow(owner: Window, focusable: Boolean) = ComposeDialog(owner, Dialog.ModalityType.MODELESS).apply {
    isUndecorated = true
    isResizable = false
    isTransparent = true
    type = Window.Type.POPUP
    focusableWindowState = focusable
    isAutoRequestFocus = focusable
}

/** Keeps a popup window where its position provider says, and answers what counts as a click outside it. */
@OptIn(ExperimentalComposeUiApi::class)
private class PopupPlacer(
    private val owner: Window,
    private val root: Window,
    var provider: PopupPositionProvider,
    var density: Density,
) {
    var direction = LayoutDirection.Ltr
    var margins = PopupMargins.None
    var anchor: IntRect? = null
    var contentSize: IntSize? = null
    var ready by mutableStateOf(false)
        private set

    private var window: ComposeDialog? = null

    /** On the first call, measures the content while the window is still hidden, so it opens at its real size. */
    fun attach(dialog: ComposeDialog) {
        val reused = window == null && dialog.isDisplayable
        if (window == null) {
            window = dialog
            dialog.preferredSize = Dimension(root.width, root.height)
            dialog.pack()
            dialog.preferredSize = null
            val measured = checkNotNull(dialog.preferredSize)
            contentSize = IntSize(
                ((measured.width - margins.left.value - margins.right.value) * density.density).roundToInt().coerceAtLeast(0),
                ((measured.height - margins.top.value - margins.bottom.value) * density.density).roundToInt().coerceAtLeast(0),
            )
        }
        place()
        // A reused window still holds its last picture, which must not show when it opens.
        if (reused) {
            Snapshot.withoutReadObservation {
                dialog.validate()
                dialog.renderImmediately()
            }
        }
    }

    fun place() {
        val dialog = window ?: return
        val size = contentSize ?: return
        val from = anchor ?: return
        if (!owner.isShowing || !root.isShowing) return
        val rootOrigin = contentOrigin(root)
        val ownerOrigin = contentOrigin(owner)
        val rootPane = (root as? RootPaneContainer)?.contentPane ?: root
        val rootSize = IntSize((rootPane.width * density.density).roundToInt(), (rootPane.height * density.density).roundToInt())
        val offset = IntOffset(
            ((ownerOrigin.x - rootOrigin.x) * density.density).roundToInt(),
            ((ownerOrigin.y - rootOrigin.y) * density.density).roundToInt(),
        )
        val position = clipToWindow(provider.calculatePosition(from.translate(offset), rootSize, direction, size), size, rootSize)
        val bounds = popupWindowBounds(rootOrigin, position, size, margins, density)
        if (dialog.bounds != bounds) dialog.bounds = bounds
        ready = true
    }

    /** Whether a press landed off this popup: not on its content, and not in a popup opened from it. */
    fun isOutside(event: MouseEvent): Boolean {
        val dialog = window ?: return false
        val target = event.component as? Window ?: SwingUtilities.getWindowAncestor(event.component)
        if (target === dialog) {
            val size = contentSize ?: return false
            return !isOnContent(SwingUtilities.convertPoint(event.component, event.point, dialog), size, margins, density)
        }
        return generateSequence(target) { it.owner }.none { it === dialog }
    }

    private fun contentOrigin(window: Window): Point = ((window as? RootPaneContainer)?.contentPane ?: window).locationOnScreen
}
