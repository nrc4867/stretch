package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * The showing tab [id]'s content, or [HeldTab] while its window holds it back after a crash. The content is noted on
 * the window's [WindowTabs] before it composes, so a crash it causes is put on this tab.
 */
@Composable
internal fun PaneTabBody(id: String, grid: PaneGridContext) {
    val holds = LocalTabHolds.current
    if (holds != null && holds.isHeld(id)) {
        HeldTab(onReload = { holds.release(id) })
        return
    }
    val tabs = LocalWindowTabs.current
    if (tabs != null) {
        val showing = remember(tabs) { tabs.shown(id) }
        DisposableEffect(tabs) { onDispose { tabs.hidden(id, showing) } }
    }
    grid.holder.SaveableStateProvider(id) { Box(Modifier.fillMaxSize()) { grid.content(this, id) } }
}
