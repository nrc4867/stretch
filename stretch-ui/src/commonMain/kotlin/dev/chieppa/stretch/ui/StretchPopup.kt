package dev.chieppa.stretch.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import dev.chieppa.stretch.theme.StretchShadow

/**
 * A popup placed by [popupPositionProvider]. Normally a plain Compose popup. Where popups have to show over a native
 * surface it is an OS window of its own, measured and placed before it shows, so it never covers the window or the
 * pointer while it opens, and moving it moves the window without drawing its content again. [shadow] is the shadow the
 * content draws around itself, which that window leaves room for.
 */
@Composable
expect fun StretchPopup(
    popupPositionProvider: PopupPositionProvider,
    onDismissRequest: (() -> Unit)? = null,
    properties: PopupProperties = PopupProperties(),
    shadow: StretchShadow? = null,
    content: @Composable () -> Unit,
)

/**
 * For a menu or popover: it takes the keyboard, and a press outside closes it and still reaches what is under the
 * pointer, so the rest of the window keeps its hover while it is open and one click both closes it and acts.
 */
@OptIn(ExperimentalComposeUiApi::class)
val TransientPopup = PopupProperties(focusable = true, consumePointerInputOutside = false)
