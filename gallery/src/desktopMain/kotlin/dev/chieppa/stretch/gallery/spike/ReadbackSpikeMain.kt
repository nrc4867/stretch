package dev.chieppa.stretch.gallery.spike

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.chieppa.stretch.frame.NativeSurfaces
import dev.chieppa.stretch.frame.StretchWindow
import dev.chieppa.stretch.frame.WindowTopBar
import dev.chieppa.stretch.theme.Stretch
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes

/**
 * The readback spike on its own. `-Dspike.heavy=true` draws the viewport with today's native surface, popups in windows
 * of their own; otherwise the scene is read back into Compose. `-Dspike.bench=true` measures, prints and quits.
 */
fun main() {
    val heavy = System.getProperty(SpikeProperties.HEAVY).toBoolean()
    val bench = System.getProperty(SpikeProperties.BENCH).toBoolean()
    if (heavy) NativeSurfaces.enable()
    application {
        val state = rememberWindowState(width = 1200.dp, height = 820.dp)
        StretchWindow(onCloseRequest = ::exitApplication, title = "Readback spike", state = state) {
            StretchTheme(StretchThemes.Ink) {
                Column(Modifier.background(Stretch.colors.surfaceCanvas)) {
                    WindowTopBar("Stretch", "Readback spike")
                    ReadbackSpikePage(heavy, bench, renderApi = window.renderApi.toString(), onBenchDone = ::exitApplication)
                }
            }
        }
    }
}
