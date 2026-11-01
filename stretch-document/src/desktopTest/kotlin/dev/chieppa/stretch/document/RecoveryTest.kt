package dev.chieppa.stretch.document

import java.nio.file.Files
import kotlin.io.path.deleteRecursively
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText
import kotlin.io.path.ExperimentalPathApi
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalPathApi::class)
class RecoveryTest {
    @Test
    fun aSlotStillHeldIsNotOfferedAndAnAbandonedOneIs() {
        val dir = Files.createTempDirectory("stretch-recovery")
        try {
            val recovery = Recovery(dir)
            val live = recovery.slot()
            live.write("fox".encodeToByteArray(), "/docs/Fox.txt", "Fox")
            assertTrue(recovery.pending().isEmpty(), "this run still holds it")

            // A run that ended without closing its slot leaves its copy and an unheld lock behind.
            Files.writeString(dir.resolve("gone.properties"), "name=Hare\n")
            Files.write(dir.resolve("gone.data"), "hare".encodeToByteArray())
            val found = recovery.pending().single()
            assertEquals("Hare", found.name)
            assertEquals(null, found.path)
            assertContentEquals("hare".encodeToByteArray(), recovery.read(found))
            recovery.discard(found)
            assertTrue(recovery.pending().isEmpty())

            live.clear()
            assertTrue(dir.listDirectoryEntries("*.data").isEmpty(), "a save clears the copy")
            live.close()
            assertTrue(dir.listDirectoryEntries().isEmpty(), "a clean close leaves nothing behind")
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun fileStorageReplacesTheWholeFile() {
        val dir = Files.createTempDirectory("stretch-files")
        try {
            val path = dir.resolve("Fox.txt").toString()
            FileStorage.write(path, "a long first version".encodeToByteArray())
            FileStorage.write(path, "short".encodeToByteArray())
            assertEquals("short", dir.resolve("Fox.txt").readText())
            assertEquals("Fox", FileStorage.displayName(path))
            assertEquals(listOf("Fox.txt"), dir.listDirectoryEntries().map { it.fileName.toString() }, "no temporary files left")
        } finally {
            dir.deleteRecursively()
        }
    }
}
