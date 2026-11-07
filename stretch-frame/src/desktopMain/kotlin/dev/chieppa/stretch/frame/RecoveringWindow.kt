package dev.chieppa.stretch.frame

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.LocalWindowExceptionHandlerFactory

/** Where the app's windows send their crashes. */
internal val LocalCrashReport = staticCompositionLocalOf<(WindowCrash) -> Unit> { { it.cause.printStackTrace() } }

/**
 * Builds [window], a `Window` call, again whenever it crashes, through a [WindowRecovery]: tabs held back go on
 * [holds], each crash goes to [report], and [close] closes the window once building it again stops helping. The
 * window's content calls [WindowSettles].
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun RecoveringWindow(holds: TabHolds, report: (WindowCrash) -> Unit, close: () -> Unit, window: @Composable () -> Unit) {
    val reportNow by rememberUpdatedState(report)
    val closeNow by rememberUpdatedState(close)
    val recovery = remember(holds) { WindowRecovery(WindowTabs(holds), { reportNow(it) }, { closeNow() }) }
    CompositionLocalProvider(LocalWindowExceptionHandlerFactory provides recovery.factory, LocalWindowTabs provides recovery.tabs) {
        key(recovery.generation) { window() }
    }
}

/** Ends the window's settling after its first frame, from when tabs starting to show are noted. */
@Composable
internal fun WindowSettles() {
    val tabs = LocalWindowTabs.current ?: return
    LaunchedEffect(tabs) {
        withFrameNanos { }
        tabs.settling = false
    }
}
