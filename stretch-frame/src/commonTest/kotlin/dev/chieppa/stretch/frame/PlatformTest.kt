package dev.chieppa.stretch.frame

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.chieppa.stretch.core.KeyChord
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import dev.chieppa.stretch.test.setStretchContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class PlatformTest {
    @Test
    fun theInterfaceSizeScalesEveryDp() = runComposeUiTest {
        var normal = 0
        var scaled = 0
        var nested = 0
        var window = 0
        var display = 1f
        setContent {
            display = LocalDensity.current.density
            StretchTheme(StretchThemes.Ink) {
                Box(Modifier.size(100.dp).onSizeChanged { normal = it.width })
            }
            StretchTheme(StretchThemes.Ink, scale = 1.5f) {
                Box(Modifier.size(100.dp).onSizeChanged { scaled = it.width })
                StretchTheme(StretchThemes.InkDark, scale = 2f) { Box(Modifier.size(100.dp).onSizeChanged { nested = it.width }) }
                // A new window resets the density to its display's.
                CompositionLocalProvider(LocalDensity provides Density(display)) {
                    StretchTheme(StretchThemes.Ink) { Box(Modifier.size(100.dp).onSizeChanged { window = it.width }) }
                }
            }
        }
        waitForIdle()
        assertEquals((100 * display).toInt(), normal)
        assertEquals((150 * display).toInt(), scaled)
        assertEquals((200 * display).toInt(), nested, "a nested size replaces the outer one")
        assertEquals((150 * display).toInt(), window, "a new window keeps the size")
    }

    private class Layers : ClipboardHandler {
        val names = mutableListOf("Body")
        override val types = setOf("test/layer")
        override fun copy() = ClipData(names.last(), mapOf("test/layer" to names.last()))
        override fun cut() = copy().also { names.removeAt(names.lastIndex) }
        override fun paste(data: ClipData): Boolean {
            names += (data.custom["test/layer"] ?: data.text ?: return false) + " pasted"
            return true
        }
    }

    @Test
    fun clipboardKeysGoToTheFocusedTarget() = runComposeUiTest {
        val clipboard = MemoryClipboard()
        val layers = Layers()
        val focus = FocusRequester()
        lateinit var router: ClipboardRouter
        setStretchContent {
            CompositionLocalProvider(LocalClipboardAccess provides clipboard) {
                router = rememberClipboardRouter()
                Box(Modifier.size(100.dp).testTag("layers").focusRequester(focus).clipboardTarget(router, layers).focusable())
            }
        }
        assertTrue(!router.active, "nothing is focused yet")
        runOnIdle { focus.requestFocus() }
        waitForIdle()
        assertTrue(router.active)
        onNodeWithTag("layers").performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(Key.C) } }
        assertEquals("Body", clipboard.content?.custom?.get("test/layer"))
        onNodeWithTag("layers").performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(Key.V) } }
        assertEquals(listOf("Body", "Body pasted"), layers.names)

        clipboard.write(ClipData(text = "Plain"))
        assertTrue(router.paste(), "menus and the palette paste through the router")
        assertEquals("Plain pasted", layers.names.last())
        assertEquals(listOf("copy", "cut", "paste"), router.actions().map { it.id.value })
        assertTrue(router.actions().all { it.isAvailable() })
    }

    @Test
    fun keyCapsKeepTheirNamesOffMacOs() {
        assertEquals(listOf("Ctrl", "Shift", "Z"), KeyChord.parse("Ctrl Shift Z").caps())
    }

    @Test
    fun anEditedDocumentIsMarkedInTheTopBar() = runComposeUiTest {
        setStretchContent { TopBar("App", "Fox", edited = true) }
        onNodeWithText("Fox", useUnmergedTree = true).assertExists()
        onNodeWithText("Fox").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Unsaved changes"))
    }

    @Test
    fun viewportCornersFollowTheReadingDirection() = runComposeUiTest {
        setStretchContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box(Modifier.size(300.dp, 200.dp)) {
                    Viewport(topStart = { Box(Modifier.size(40.dp).testTag("tools")) }, topEnd = { Box(Modifier.size(40.dp).testTag("view")) }) {}
                }
            }
        }
        val tools = onNodeWithTag("tools").fetchSemanticsNode().boundsInRoot
        val view = onNodeWithTag("view").fetchSemanticsNode().boundsInRoot
        assertTrue(tools.left > view.left, "the toolbar is at the start, on the right: $tools $view")
    }

    @Test
    fun aToastRisesAboveTheReadoutItWouldCover() = runComposeUiTest {
        setStretchContent {
            Box(Modifier.size(300.dp, 200.dp)) {
                Viewport(
                    bottomStart = { Box(Modifier.size(200.dp, 30.dp).testTag("hud")) },
                    bottomCenter = { Box(Modifier.size(120.dp, 40.dp).testTag("toast")) },
                ) {}
            }
        }
        val hud = onNodeWithTag("hud").fetchSemanticsNode().boundsInRoot
        val toast = onNodeWithTag("toast").fetchSemanticsNode().boundsInRoot
        assertTrue(toast.bottom <= hud.top, "toast $toast sits above the readout $hud")
    }
}
