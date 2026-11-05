package dev.chieppa.stretch.mermaid

import kotlin.test.Test
import kotlin.test.assertFalse

/** On a runtime without JCEF, as the build arranges for tests without -Pcef. */
class EnginelessTest {
    @Test
    fun handlesNoDiagramsSoTheyShowAsCode() = assertFalse(MermaidDiagrams().handles("mermaid"))
}
