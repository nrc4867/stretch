package dev.chieppa.stretch.test

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes

/** Sets [content] inside a Stretch theme. */
@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.setStretchContent(
    theme: StretchTheme = StretchThemes.Ink,
    content: @Composable () -> Unit,
) {
    setContent { StretchTheme(theme, content = content) }
}

/** Composes [content] once, then runs [check] in each of the four themes, switching theme in place. */
@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.inEveryTheme(content: @Composable () -> Unit, check: ComposeUiTest.(StretchTheme) -> Unit) {
    var theme by mutableStateOf(StretchThemes.all.first())
    setContent { StretchTheme(theme, content = content) }
    for (each in StretchThemes.all) {
        theme = each
        waitForIdle()
        check(each)
    }
}
