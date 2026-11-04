package dev.chieppa.stretch.gallery

import androidx.compose.runtime.CompositionLocalProvider
import dev.chieppa.stretch.document.Recovery
import dev.chieppa.stretch.document.forApp
import dev.chieppa.stretch.frame.WindowTopBar
import dev.chieppa.stretch.frame.stretchPage

private const val APP_ID = "dev.chieppa.stretch.gallery"

fun main() {
    val host = GalleryHost().apply { recovery = Recovery.forApp(APP_ID) }
    stretchPage("Stretch gallery") {
        CompositionLocalProvider(LocalGalleryHost provides host) {
            Gallery { document, edited, menus, modes, actions ->
                WindowTopBar("Stretch", document, edited = edited, menus = menus, modes = modes, actions = actions)
            }
        }
    }
}
