package dev.chieppa.stretch.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RequirementsTest {
    @Test
    fun anAppWithoutModulesThatNeedAnythingListsNothing() {
        assertEquals(emptyList(), Requirements.all)
        assertNull(Requirements.find("stretch-web.engine"))
    }

    @Test
    fun progressIsAFractionWhenTheTotalIsKnown() {
        assertEquals(0.25f, RequirementState.Installing(25, 100).fraction)
        assertNull(RequirementState.Installing(25, null).fraction)
    }
}
