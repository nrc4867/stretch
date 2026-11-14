package dev.chieppa.stretch.frame

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class CrashLadderTest {
    private fun facts(
        showing: List<String> = listOf("a", "b"),
        held: Set<String> = emptySet(),
        lastShown: String? = null,
        sinceShown: Duration? = null,
        sinceLastCrash: Duration? = null,
        reloaded: String? = null,
        sinceReloaded: Duration? = null,
    ) = CrashFacts(showing, held, lastShown, sinceShown, sinceLastCrash, reloaded, sinceReloaded)

    @Test
    fun aTabThatJustStartedShowingIsHeld() {
        assertEquals(CrashResponse.Rebuild(listOf("b")), CrashLadder.respond(facts(lastShown = "b", sinceShown = 300.milliseconds)))
    }

    @Test
    fun aTabJustShownIsHeldEvenWhenTheCrashStoppedItsFrame() {
        assertEquals(CrashResponse.Rebuild(listOf("c")), CrashLadder.respond(facts(lastShown = "c", sinceShown = 10.milliseconds)))
    }

    @Test
    fun aFirstCrashLongAfterAnyTabShowedRebuildsAsItWas() {
        assertEquals(CrashResponse.Rebuild(emptyList()), CrashLadder.respond(facts(lastShown = "b", sinceShown = 30.seconds)))
        assertEquals(CrashResponse.Rebuild(emptyList()), CrashLadder.respond(facts(sinceLastCrash = 1.seconds + CrashLadder.REPEAT_WINDOW)))
    }

    @Test
    fun aRepeatedCrashHoldsEveryShowingTab() {
        assertEquals(CrashResponse.Rebuild(listOf("a", "b")), CrashLadder.respond(facts(sinceLastCrash = 1.seconds)))
        assertEquals(CrashResponse.Rebuild(listOf("b")), CrashLadder.respond(facts(held = setOf("a"), sinceLastCrash = 1.seconds)))
    }

    @Test
    fun aRepeatedCrashWithNothingLeftToHoldCloses() {
        assertEquals(CrashResponse.Close, CrashLadder.respond(facts(held = setOf("a", "b"), sinceLastCrash = 1.seconds)))
        assertEquals(CrashResponse.Close, CrashLadder.respond(facts(showing = emptyList(), sinceLastCrash = 1.seconds)))
    }

    @Test
    fun aHeldTabShownLastIsNotHeldAgain() {
        val response = CrashLadder.respond(facts(held = setOf("b"), lastShown = "b", sinceShown = 100.milliseconds, sinceLastCrash = 1.seconds))
        assertEquals(CrashResponse.Rebuild(listOf("a")), response)
    }

    @Test
    fun aCrashSoonAfterAReloadHoldsTheReloadedTabAgain() {
        assertEquals(CrashResponse.Rebuild(listOf("b")), CrashLadder.respond(facts(reloaded = "b", sinceReloaded = 5.seconds)))
        val repeated = facts(reloaded = "b", sinceReloaded = 5.seconds, sinceLastCrash = 5.seconds)
        assertEquals(CrashResponse.Rebuild(listOf("b")), CrashLadder.respond(repeated), "only the reloaded tab, not every showing one")
    }

    @Test
    fun aReloadLongAgoOrOfATabNotShowingCountsForNothing() {
        assertEquals(CrashResponse.Rebuild(emptyList()), CrashLadder.respond(facts(reloaded = "b", sinceReloaded = 1.seconds + CrashLadder.REPEAT_WINDOW)))
        assertEquals(CrashResponse.Rebuild(emptyList()), CrashLadder.respond(facts(reloaded = "c", sinceReloaded = 1.seconds)))
    }

    @Test
    fun aTabJustShownCountsBeforeOneReloaded() {
        assertEquals(CrashResponse.Rebuild(listOf("a")), CrashLadder.respond(facts(lastShown = "a", sinceShown = 100.milliseconds, reloaded = "b", sinceReloaded = 3.seconds)))
    }
}
