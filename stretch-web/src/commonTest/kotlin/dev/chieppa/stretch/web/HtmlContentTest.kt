package dev.chieppa.stretch.web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HtmlContentTest {
    @Test
    fun aPermissivePolicyAddsNoRestriction() {
        assertNull(contentSecurityPolicy(HtmlPolicy()))
    }

    @Test
    fun blockingScriptsKeepsTheNetwork() {
        val csp = contentSecurityPolicy(HtmlPolicy(scripts = false)).orEmpty()
        assertTrue("script-src 'none'" in csp)
        assertTrue("default-src *" in csp)
    }

    @Test
    fun blockingTheNetworkKeepsWhatTheDocumentHolds() {
        val csp = contentSecurityPolicy(HtmlPolicy(network = false)).orEmpty()
        assertFalse("*" in csp)
        assertTrue("data:" in csp && "'unsafe-inline'" in csp)
        assertFalse("script-src" in csp)
    }

    @Test
    fun thePolicyGoesBeforeAnythingTheDocumentSays() {
        val document = prepareDocument("<!doctype html><html lang=en><head><script>x()</script></head></html>", HtmlPolicy(scripts = false))
        assertTrue(document.indexOf("Content-Security-Policy") in 1 until document.indexOf("<script>"))
        assertTrue(document.startsWith("<!doctype html><html lang=en><head><meta"))
    }

    @Test
    fun aFragmentGainsAHead() {
        val document = prepareDocument("<p>hi</p>", HtmlPolicy(network = false))
        assertTrue(document.startsWith("<head>"))
        assertTrue(document.endsWith("</head><p>hi</p>"))
    }

    @Test
    fun aDocumentWithoutHeadGainsOneInsideHtml() {
        assertTrue(prepareDocument("<html><body>x</body></html>", HtmlPolicy()).startsWith("<html><head>"))
    }

    @Test
    fun aDoctypeStaysFirst() {
        assertTrue(prepareDocument("<!DOCTYPE html><p>x", HtmlPolicy()).startsWith("<!DOCTYPE html><head>"))
    }

    @Test
    fun theBaseAddressIsEscaped() {
        val document = prepareDocument("<p>", HtmlPolicy(), baseUrl = "https://a/\"><script>")
        assertFalse("<script>" in document)
        assertTrue("&quot;&gt;".replace("&gt;", ">") in document)
    }

    @Test
    fun theLookSetsTheStartingColors() {
        val document = prepareDocument("<p>", HtmlPolicy(), look = HtmlLook(true, "#eeeeee"))
        assertTrue("color-scheme:dark" in document && "#eeeeee" in document)
    }

    @Test
    fun anchorsStayOnThePage() {
        assertTrue(isSamePage("data:text/html;base64,AA", "data:text/html;base64,AA#top"))
        assertFalse(isSamePage("data:text/html;base64,AA", "https://example.com/"))
    }

    @Test
    fun scriptOutcomesReadBack() {
        assertEquals("7", outcomeOf("ok:7"))
        assertEquals("boom", assertFailsWith<ScriptException> { outcomeOf("err:boom") }.message)
    }
}
