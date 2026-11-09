package dev.chieppa.stretch.frame

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FuzzyMatchTest {
    private val title = "Rotate refresh tokens on every use"

    private fun score(query: String, label: String = title) = assertNotNull(fuzzyMatch(query, label)).score

    @Test
    fun wholeQueryMatchesRankFirst() {
        assertEquals(0, score("rot"))
        assertEquals(1, score("tok"))
        assertEquals(2, score("oken"))
        assertTrue(score("oken") < score("tokens rotate"))
    }

    @Test
    fun wordsMatchInAnyOrder() {
        val match = assertNotNull(fuzzyMatch("tokens rotate", title))
        assertEquals(listOf(0..5, 15..20), match.ranges)
    }

    @Test
    fun aSlippedLetterStillMatches() {
        assertNotNull(fuzzyMatch("refersh", title))
        assertNotNull(fuzzyMatch("tokns", title))
        assertNotNull(fuzzyMatch("rotate refrseh", title))
        assertTrue(score("refresh") < score("refersh"))
    }

    @Test
    fun shortWordsMustMatchExactly() {
        assertNull(fuzzyMatch("rxt zzz", title))
        assertNull(fuzzyMatch("tak", "token"))
        assertNull(fuzzyMatch("#402", "#401"))
        assertNull(fuzzyMatch("demo@c", "demo@aaaa"))
    }

    @Test
    fun capitalsStartWords() {
        assertEquals(1, score("session", "useSession"))
        assertEquals(listOf(3..9), fuzzyMatch("session", "useSession")!!.ranges)
    }

    @Test
    fun lettersInOrderPreferCloseTogether() {
        val near = score("rfrsh", "refresh")
        val far = score("rfrsh", "rotate from rush")
        assertTrue(near < far)
        assertTrue(near >= 1000)
    }

    @Test
    fun blankMatchesEverythingAndNothingMatchesNull() {
        assertEquals(FuzzyMatch(0, emptyList()), fuzzyMatch("  ", title))
        assertNull(fuzzyMatch("qqq", title))
    }

    @Test
    fun searchRanksItemsByTheirBestLabel() {
        data class Pull(val title: String, val branch: String)
        val pulls = listOf(Pull("Fix login", "feature/token-binding"), Pull("Token refresh", "main"), Pull("Docs", "docs"))
        assertEquals(listOf("Token refresh", "Fix login"), fuzzySearch("token", pulls) { listOf(it.title, it.branch) }.map { it.title })
    }

    @Test
    fun editDistanceCountsASwapOnce() {
        assertEquals(1, EditDistance.of("refcator", "refactor", 2))
        assertEquals(3, EditDistance.of("abc", "xyz", 2))
    }
}
