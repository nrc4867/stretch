package dev.chieppa.stretch.diff

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UnifiedPatchTest {
    private val gitPatch = """
        commit 1234
        Author: someone

            Message mentioning --- nothing

        diff --git a/src/Main.kt b/src/Main.kt
        index 83db48f..bf2a2a3 100644
        --- a/src/Main.kt
        +++ b/src/Main.kt
        @@ -1,4 +1,4 @@ fun main() {
         fun main() {
        -    println("hello")
        +    println("hello, world")
             run()
         }
        @@ -10,2 +10,3 @@
         a
        +b
         c
        \ No newline at end of file
        diff --git a/new.txt b/new.txt
        new file mode 100644
        index 0000000..ce01362
        --- /dev/null
        +++ b/new.txt
        @@ -0,0 +1 @@
        +hello
        diff --git a/old.txt b/old.txt
        deleted file mode 100644
        --- a/old.txt
        +++ /dev/null
        @@ -1 +0,0 @@
        -bye
        diff --git a/from.txt b/to.txt
        similarity index 100%
        rename from from.txt
        rename to to.txt
        diff --git a/pic.png b/pic.png
        Binary files a/pic.png and b/pic.png differ
    """.trimIndent()

    @Test
    fun readsEveryKindOfFileChange() {
        val files = parseUnifiedDiff(gitPatch)
        assertEquals(listOf("src/Main.kt", "new.txt", "old.txt", "to.txt", "pic.png"), files.map { it.path })
        assertEquals(
            listOf(FileStatus.Modified, FileStatus.Added, FileStatus.Deleted, FileStatus.Renamed, FileStatus.Modified),
            files.map { it.status },
        )
        assertEquals("from.txt", files[3].oldPath)
        assertTrue(files[4].binary)
    }

    @Test
    fun readsHunksWithTheirNumbersAndHeading() {
        val main = parseUnifiedDiff(gitPatch).first()
        assertEquals(2, main.hunks.size)
        assertEquals("fun main() {", main.hunks[0].heading)
        assertEquals(2, main.added)
        assertEquals(1, main.removed)
        val added = main.hunks[1].lines.single { it.kind == LineKind.Added }
        assertEquals(DiffLine(LineKind.Added, "b", newNumber = 11), added)
        assertEquals(12, main.hunks[1].lines.last().newNumber)
    }

    @Test
    fun marksChangedWordsInPatches() {
        val removed = parseUnifiedDiff(gitPatch).first().hunks[0].lines.first { it.kind == LineKind.Added }
        assertTrue(removed.changes.isNotEmpty())
    }

    @Test
    fun aRemovedLineThatLooksLikeAHeaderStaysALine() {
        val patch = """
            --- a/notes.md
            +++ b/notes.md
            @@ -1,2 +1,1 @@
            --- old rule
             keep
        """.trimIndent()
        val file = parseUnifiedDiff(patch).single()
        assertEquals(listOf("-- old rule", "keep"), file.hunks.single().lines.map { it.text })
    }

    @Test
    fun readsPlainDiffOutputWithSeveralFiles() {
        val patch = """
            --- one.txt	2024-01-01
            +++ one.txt	2024-01-02
            @@ -1 +1 @@
            -a
            +b
            --- two.txt
            +++ two.txt
            @@ -1 +1 @@
            -c
            +d
        """.trimIndent()
        assertEquals(listOf("one.txt", "two.txt"), parseUnifiedDiff(patch).map { it.path })
    }

    @Test
    fun readsWindowsLineEndings() {
        val patch = "--- a\r\n+++ b\r\n@@ -1 +1 @@\r\n-x\r\n+y\r\n"
        assertEquals(listOf("x", "y"), parseUnifiedDiff(patch).single().hunks.single().lines.map { it.text })
    }

    @Test
    fun emptyInputHasNoFiles() {
        assertEquals(emptyList(), parseUnifiedDiff(""))
    }
}
