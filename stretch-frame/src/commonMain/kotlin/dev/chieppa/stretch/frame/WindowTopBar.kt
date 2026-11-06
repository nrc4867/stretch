package dev.chieppa.stretch.frame

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The [TopBar] of the app's window or page. [menus], [modes], [leading], [center] and [actions] are app controls. What
 * else the bar does belongs to the platform.
 */
@Composable
expect fun WindowTopBar(
    appName: String,
    document: String,
    modifier: Modifier = Modifier,
    edited: Boolean = false,
    menus: @Composable () -> Unit = {},
    modes: @Composable () -> Unit = {},
    leading: @Composable () -> Unit = {},
    center: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
)
