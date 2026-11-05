package dev.chieppa.stretch.mermaid

import dev.chieppa.stretch.theme.StretchThemes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MermaidThemeTest {
    @Test
    fun aStringCannotBreakOutOfItsLiteral() {
        assertEquals("\"a\\\"b\\\\c\\nd\\u003c/script>\"", jsString("a\"b\\c\nd</script>"))
    }

    @Test
    fun themeVariablesUseTheThemesColors() {
        val json = mermaidThemeVariables(StretchThemes.Ink.colors, dark = false)
        assertTrue("\"darkMode\":false" in json)
        assertTrue("\"primaryTextColor\":\"#231c1f\"" in json)
        assertFalse("##" in json)
    }
}
