package dev.chieppa.stretch.web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HtmlBridgeTest {
    @Test
    fun messagesWaitForThePage() {
        val heard = mutableListOf<String>()
        val sent = mutableListOf<String>()
        val bridge = HtmlBridge { heard += it }
        bridge.post("one")
        bridge.post("two")
        assertEquals(emptyList(), sent)
        bridge.attach { sent += it }
        assertEquals(listOf("one", "two"), sent)
        bridge.post("three")
        assertEquals(listOf("one", "two", "three"), sent)
        bridge.detach()
        bridge.post("four")
        assertEquals(3, sent.size)
        bridge.receive("from the page")
        assertEquals(listOf("from the page"), heard)
    }

    @Test
    fun stringsBecomeSafeLiterals() {
        val quote = '"'
        val slash = '\\'
        val raw = "a${quote}b${slash}c" + "\n" + "d</script>"
        val expected = "${quote}a$slash${quote}b$slash${slash}c${slash}nd${slash}u003c/script>$quote"
        assertEquals(expected, jsString(raw))
        assertEquals("$quote${slash}u0001$quote", jsString(1.toChar().toString()))
    }

    @Test
    fun documentsGetTheThemeAndTheBridgeOnlyWithScripts() {
        val look = HtmlLook(dark = true, ink = "#eeeeee", variables = mapOf("--stretch-accent" to "#ff8800"))
        val withScripts = prepareDocument("<p>x</p>", HtmlPolicy(), look = look, script = "window.ok=1")
        assertTrue("--stretch-accent:#ff8800" in withScripts)
        assertTrue("<script>window.ok=1</script>" in withScripts)
        val without = prepareDocument("<p>x</p>", HtmlPolicy(scripts = false), look = look, script = "window.ok=1")
        assertFalse("window.ok" in without)
        assertTrue(bridgeScript("send(m)").contains("stretch-ready"))
    }
}
