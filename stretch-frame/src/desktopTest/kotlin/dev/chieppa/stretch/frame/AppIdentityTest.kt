package dev.chieppa.stretch.frame

import kotlin.test.Test
import kotlin.test.assertEquals

class AppIdentityTest {
    @Test
    fun readsTheIdentityThePluginWrites() {
        val identity = AppIdentity.current!!
        assertEquals("Test App", identity.name)
        assertEquals("dev.chieppa.test", identity.id)
        assertEquals(listOf(16, 32), identity.icons.map { it.getWidth(null) })
    }
}
