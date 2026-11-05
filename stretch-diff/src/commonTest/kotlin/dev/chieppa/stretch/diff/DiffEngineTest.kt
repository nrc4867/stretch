package dev.chieppa.stretch.diff

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DiffEngineTest {
    private fun apply(old: List<String>, new: List<String>, script: List<Edit>): Pair<List<String>, List<String>> {
        val rebuiltOld = ArrayList<String>()
        val rebuiltNew = ArrayList<String>()
        var o = 0
        var n = 0
        for (edit in script) when (edit) {
            Edit.Keep -> {
                assertEquals(old[o], new[n])
                rebuiltOld += old[o++]
                rebuiltNew += new[n++]
            }
            Edit.Remove -> rebuiltOld += old[o++]
            Edit.Add -> rebuiltNew += new[n++]
        }
        return rebuiltOld to rebuiltNew
    }

    @Test
    fun editScriptRebuildsBothSidesAtMinimalCost() {
        val old = "a b c a b b a".split(' ')
        val new = "c b a b a c".split(' ')
        val script = editScript(old, new)
        assertEquals(old to new, apply(old, new, script))
        assertEquals(5, script.count { it != Edit.Keep })
    }

    @Test
    fun editScriptHandlesEmptyAndIdentical() {
        assertEquals(List(2) { Edit.Add }, editScript(emptyList(), listOf("a", "b")))
        assertEquals(List(2) { Edit.Remove }, editScript(listOf("a", "b"), emptyList()))
        assertEquals(List(2) { Edit.Keep }, editScript(listOf("a", "b"), listOf("a", "b")))
    }

    @Test
    fun editScriptGivesUpPastTheLimitButStaysValid() {
        val old = List(50) { "o$it" }
        val new = List(50) { "n$it" }
        val script = editScript(old, new, maxDistance = 10)
        assertEquals(old to new, apply(old, new, script))
    }

    @Test
    fun manyRandomEditsRoundTrip() {
        val random = kotlin.random.Random(7)
        repeat(200) {
            val old = List(random.nextInt(30)) { random.nextInt(5).toString() }
            val new = List(random.nextInt(30)) { random.nextInt(5).toString() }
            assertEquals(old to new, apply(old, new, editScript(old, new)))
        }
    }

    @Test
    fun linesAreNumberedOnEachSide() {
        val lines = diffTexts("a\nb\nc\n", "a\nx\nc\nd\n", "f.txt").hunks.single().lines
        assertEquals(
            listOf(
                DiffLine(LineKind.Context, "a", 1, 1),
                DiffLine(LineKind.Removed, "b", oldNumber = 2),
                DiffLine(LineKind.Added, "x", newNumber = 2),
                DiffLine(LineKind.Context, "c", 3, 3),
                DiffLine(LineKind.Added, "d", newNumber = 4),
            ),
            lines,
        )
    }

    @Test
    fun onlyTheChangedWordIsMarked() {
        val lines = diffTexts("val total = price * 2\n", "val total = price * 3\n", "a.kt").hunks.single().lines
        assertEquals(listOf(CharRange(20, 21)), lines[0].changes)
        assertEquals(listOf(CharRange(20, 21)), lines[1].changes)
    }

    @Test
    fun rewrittenLinesAreNotMarkedWordByWord() {
        val (old, new) = wordChanges("completely different words", "nothing alike at all here")
        assertTrue(old.isEmpty() && new.isEmpty())
    }

    @Test
    fun statusFollowsThePaths() {
        assertEquals(FileStatus.Added, diffTexts("", "a", null, "n").status)
        assertEquals(FileStatus.Deleted, diffTexts("a", "", "o", null).status)
        assertEquals(FileStatus.Renamed, diffTexts("a", "a", "o", "n").status)
        assertEquals(FileStatus.Modified, diffTexts("a", "b", "f").status)
    }

    @Test
    fun longUnchangedRunsFoldAndUnfold() {
        val old = (1..40).joinToString("\n") { "line $it" }
        val new = old.replace("line 20", "changed")
        val file = diffTexts(old, new, "a.txt")
        val rows = diffRows(listOf(file), split = false)
        val folds = rows.filterIsInstance<DiffRow.Fold>()
        assertEquals(listOf(16, 17), folds.map { it.lineCount })
        val opened = diffRows(listOf(file), split = false, unfolded = folds.map { it.id }.toSet())
        assertEquals(0, opened.count { it is DiffRow.Fold })
        assertEquals(41, opened.count { it is DiffRow.Unified })
    }

    @Test
    fun splitPairsRemovalsWithAdditions() {
        val file = diffTexts("a\nb\nc\n", "a\nx\ny\nz\nc\n", "f.txt")
        val rows = diffRows(listOf(file), split = true).filterIsInstance<DiffRow.Split>()
        assertEquals(
            listOf("a" to "a", "b" to "x", null to "y", null to "z", "c" to "c"),
            rows.map { it.left?.text to it.right?.text },
        )
    }

    @Test
    fun rowKeysAreUnique() {
        val file = diffTexts((1..30).joinToString("\n"), (1..30).joinToString("\n") { if (it % 7 == 0) "x$it" else "$it" }, "f.txt")
        for (split in listOf(false, true)) {
            val keys = diffRows(listOf(file, file), split).map { it.key }
            assertEquals(keys.size, keys.toSet().size)
        }
    }

    @Test
    fun changeStartsFindEachRun() {
        val file = diffTexts("a\nb\nc\nd\ne\n", "a\nX\nc\nY\ne\n", "f.txt")
        val rows = diffRows(listOf(file), split = false)
        assertEquals(2, changeStarts(rows).size)
    }

    @Test
    fun collapsedFilesShowOnlyTheirHeader() {
        val file = diffTexts("a", "b", "f.txt")
        assertEquals(1, diffRows(listOf(file), split = false, collapsed = setOf(0)).size)
    }
}
