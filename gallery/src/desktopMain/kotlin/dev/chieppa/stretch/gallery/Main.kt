package dev.chieppa.stretch.gallery

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.chieppa.stretch.devtools.DevTools
import dev.chieppa.stretch.document.Recovery
import dev.chieppa.stretch.document.forApp
import dev.chieppa.stretch.frame.NativeSurfaces
import dev.chieppa.stretch.frame.StretchWindow
import dev.chieppa.stretch.frame.WindowTopBar
import dev.chieppa.stretch.gallery.gpu.GpuPage
import dev.chieppa.stretch.ui.SystemFileDialogs
import dev.chieppa.stretch.web.WebEngine

fun main(args: Array<String>) {
    // On a Java without the browser engine, run on the one the documents page fetched, once there is one.
    WebEngine.startOnInstalledRuntime(args)
    // -Pgpu on the run task: show the WebGPU page, which needs popups in windows of their own.
    val gpu = System.getProperty("stretch.gpu").toBoolean()
    if (gpu) NativeSurfaces.enable()
    DevTools.startIfRequested()
    application {
        val state = rememberWindowState(width = 1200.dp, height = 820.dp)
        val host = remember { GalleryHost().apply {
            recovery = Recovery.forApp()
            chooseFolder = { start -> SystemFileDialogs.chooseFolder("Choose a folder", start) }
        } }
        StretchWindow(
            onCloseRequest = { host.closeGuard?.invoke(::exitApplication) ?: exitApplication() },
            title = "Stretch gallery",
            state = state,
        ) {
            CompositionLocalProvider(LocalGalleryHost provides host) {
                Gallery(gpuPage = if (gpu) ({ GpuPage() }) else null, startOnGpu = gpu, startOnDocuments = System.getProperty("stretch.documents").toBoolean()) { document, edited, menus, modes, center, actions ->
                    WindowTopBar("Stretch", document, edited = edited, menus = menus, modes = modes, center = center, actions = actions)
                }
            }
        }
    }
}
