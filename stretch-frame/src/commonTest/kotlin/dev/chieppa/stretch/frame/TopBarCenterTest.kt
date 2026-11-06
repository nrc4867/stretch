package dev.chieppa.stretch.frame

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TopBarCenterTest {
    @Test
    fun centresWhenThereIsRoom() = assertEquals(CenterSpan(400, 200), centerSpan(1000, 500, 100, 900, 200, 160))

    @Test
    fun movesClearOfTheLeftGroup() = assertEquals(CenterSpan(450, 200), centerSpan(1000, 500, 450, 900, 200, 160))

    @Test
    fun movesClearOfTheRightGroup() = assertEquals(CenterSpan(300, 200), centerSpan(1000, 500, 100, 500, 200, 160))

    @Test
    fun narrowsToTheRoomBetween() = assertEquals(CenterSpan(400, 180), centerSpan(1000, 500, 400, 580, 300, 160))

    @Test
    fun hidesBelowItsMinimum() = assertNull(centerSpan(1000, 500, 400, 550, 300, 160))

    @Test
    fun aSmallItemKeepsItsOwnWidthAsTheMinimum() = assertEquals(CenterSpan(400, 100), centerSpan(1000, 500, 400, 500, 100, 160))

    @Test
    fun anEmptySlotTakesNoRoom() = assertNull(centerSpan(1000, 500, 0, 1000, 0, 160))
}
