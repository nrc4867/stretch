package dev.chieppa.stretch.web

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import dev.chieppa.stretch.theme.StretchTheme
import dev.chieppa.stretch.theme.StretchThemes
import java.awt.Component
import java.awt.EventQueue
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Sends input through a real window into a real page, by events posted to the window, so it never takes the mouse from
 * whoever is at the machine; the window sits off every screen and does not take the focus. The page reports what it saw
 * by trying to go to an address. It runs only with -Pcef.
 */
class HtmlInputEngineTest {
    private val page = """
        <!doctype html><body style="margin:0;height:3000px">
        <button id=b style="position:absolute;left:0;top:0;width:200px;height:100px" onclick="location='https://result/clicked'">x</button>
        <input id=i style="position:absolute;left:0;top:120px;width:200px;height:40px" onfocus="location='https://result/focused'" oninput="if(this.value==='ok')location='https://result/typed'">
        <a id=a style="position:absolute;left:0;top:180px" href="https://result/link">link</a>
        <script>
          addEventListener('scroll', () => { if (scrollY > 50) location = 'https://result/scrolled'; });
          window.open('https://result/popup');
          alert('this must not stop the page');
          location.hash = 'still-here';
          document.title = 'loaded';
          setTimeout(() => { location = 'https://result/loaded'; }, 50);
        </script>
    """.trimIndent()

    private val seen = LinkedBlockingQueue<String>()

    private fun awaitNavigation(url: String) {
        val deadline = System.currentTimeMillis() + TIMEOUT_MILLIS
        val others = ArrayList<String?>()
        while (System.currentTimeMillis() < deadline) {
            val next = seen.poll(deadline - System.currentTimeMillis(), TimeUnit.MILLISECONDS)
            if (next == url) return
            others += next
        }
        kotlin.test.fail("never saw $url, only $others")
    }

    private fun onEvents(block: () -> Unit) = SwingUtilities.invokeAndWait(block)

    private fun deepest(window: ComposeWindow): Component {
        var target: Component? = null
        onEvents {
            val inset = window.insets
            target = SwingUtilities.getDeepestComponentAt(window, inset.left + 10, inset.top + 10)
        }
        return assertNotNull(target)
    }

    private fun mouse(target: Component, id: Int, x: Int, y: Int, button: Int = MouseEvent.BUTTON1, count: Int = 0, modifiers: Int = 0) {
        EventQueue.invokeLater {
            target.dispatchEvent(MouseEvent(target, id, System.currentTimeMillis(), modifiers, x, y, x, y, count, false, button))
        }
    }

    private fun click(target: Component, x: Int, y: Int) {
        mouse(target, MouseEvent.MOUSE_MOVED, x, y, MouseEvent.NOBUTTON)
        mouse(target, MouseEvent.MOUSE_PRESSED, x, y, count = 1, modifiers = MouseEvent.BUTTON1_DOWN_MASK)
        mouse(target, MouseEvent.MOUSE_RELEASED, x, y, count = 1)
    }

    private fun type(target: Component, char: Char) {
        val code = KeyEvent.getExtendedKeyCodeForChar(char.code)
        EventQueue.invokeLater {
            val now = System.currentTimeMillis()
            target.dispatchEvent(KeyEvent(target, KeyEvent.KEY_PRESSED, now, 0, code, char))
            target.dispatchEvent(KeyEvent(target, KeyEvent.KEY_TYPED, now, 0, KeyEvent.VK_UNDEFINED, char))
            target.dispatchEvent(KeyEvent(target, KeyEvent.KEY_RELEASED, now, 0, code, char))
        }
    }

    @Test
    fun aPageTakesMouseKeysAndWheelAndCannotLeave() {
        lateinit var window: ComposeWindow
        onEvents {
            window = ComposeWindow()
            window.isAutoRequestFocus = false
            window.setBounds(OFF_SCREEN, OFF_SCREEN, 500, 400)
            window.setContent {
                StretchTheme(StretchThemes.Ink) {
                    HtmlView(
                        HtmlContent.Inline(page),
                        Modifier.fillMaxSize(),
                        navigation = { url ->
                            seen += url
                            false
                        },
                    )
                }
            }
            window.isVisible = true
        }
        try {
            awaitNavigation("https://result/loaded")
            val target = deepest(window)
            click(target, 50, 50)
            awaitNavigation("https://result/clicked")
            click(target, 50, 140)
            awaitNavigation("https://result/focused")
            type(target, 'o')
            type(target, 'k')
            awaitNavigation("https://result/typed")
            EventQueue.invokeLater {
                target.dispatchEvent(
                    MouseWheelEvent(target, MouseEvent.MOUSE_WHEEL, System.currentTimeMillis(), 0, 300, 300, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, 3),
                )
            }
            awaitNavigation("https://result/scrolled")
        } finally {
            onEvents { window.dispose() }
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 30_000L

        /** Far left of and above any screen, so no one sees the window. */
        const val OFF_SCREEN = -20_000
    }
}
