package dev.chieppa.stretch.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import dev.chieppa.stretch.theme.StretchShadow

@Composable
actual fun StretchPopup(
    popupPositionProvider: PopupPositionProvider,
    onDismissRequest: (() -> Unit)?,
    properties: PopupProperties,
    shadow: StretchShadow?,
    content: @Composable () -> Unit,
) = Popup(popupPositionProvider, onDismissRequest, properties, content)
