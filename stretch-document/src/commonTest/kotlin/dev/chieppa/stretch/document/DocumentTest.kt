package dev.chieppa.stretch.document

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import dev.chieppa.stretch.core.Applied
import dev.chieppa.stretch.core.Command
import dev.chieppa.stretch.core.History
import dev.chieppa.stretch.test.setStretchContent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private object TextFormat : DocumentFormat<String> {
    override val extension = "txt"
    override val description = "Text"
    override fun encode(document: String) = document.encodeToByteArray()
    override fun decode(bytes: ByteArray) = bytes.decodeToString()
}

private class MemoryStorage : DocumentStorage {
    val files = mutableMapOf<String, ByteArray>()
    override suspend fun read(path: String) = files.getValue(path)
    override suspend fun write(path: String, bytes: ByteArray) {
        files[path] = bytes
    }
    override suspend fun exists(path: String) = path in files
    override fun displayName(path: String) = path.substringAfterLast('/').substringBeforeLast('.')
}

private class PickedFile(var path: String?) : FileChooser {
    var asked = 0
    override suspend fun chooseOpen(format: DocumentFormat<*>) = path.also { asked++ }
    override suspend fun chooseSave(format: DocumentFormat<*>, suggestedName: String) = path.also { asked++ }
}

private data class Append(val text: String) : Command<String> {
    override fun apply(document: String) = Applied(document + text, Trim(text.length))
}

private data class Trim(val count: Int) : Command<String> {
    override fun apply(document: String) = Applied(document.dropLast(count), Append(document.takeLast(count)))
}

@OptIn(ExperimentalTestApi::class)
class DocumentTest {
    @Test
    fun aSessionTracksAHistoryItIsGiven() = runTest {
        val history = History("a")
        val session = DocumentSession(TextFormat, history.document.value, MemoryStorage(), history = history)
        assertFalse(session.edited)
        history.execute(Append("b"))
        assertTrue(session.edited)
        session.saveTo("file.txt")
        assertFalse(session.edited)
        history.undo()
        assertTrue(session.edited)
    }

    @Test
    fun savingMakesItCleanAndUndoingBackToTheSaveDoesToo() = runTest {
        val storage = MemoryStorage()
        val session = DocumentSession(TextFormat, "fox", storage)
        assertFalse(session.edited)
        assertNull(session.fileName)

        session.history.execute(Append(" tail"))
        assertTrue(session.edited)
        val chooser = PickedFile("/docs/Fox.txt")
        assertTrue(session.save(chooser))
        assertEquals("fox tail", storage.files.getValue("/docs/Fox.txt").decodeToString())
        assertEquals("Fox", session.fileName)
        assertFalse(session.edited)

        session.history.execute(Append(" ears"))
        assertTrue(session.edited)
        session.history.undo()
        assertFalse(session.edited, "back at the saved state")
        session.history.undo()
        assertTrue(session.edited, "before the saved state")
        session.history.redo()
        assertTrue(session.save(chooser))
        assertEquals(1, chooser.asked, "a document with a file saves without asking")
    }

    @Test
    fun cancellingTheSaveDialogSavesNothing() = runTest {
        val session = DocumentSession(TextFormat, "fox", MemoryStorage())
        session.history.execute(Append("!"))
        assertFalse(session.save(PickedFile(null)))
        assertTrue(session.edited)
        assertNull(session.path)
    }

    @Test
    fun openingAndRestoring() = runTest {
        val storage = MemoryStorage().apply { files["/a.txt"] = "hare".encodeToByteArray() }
        val session = DocumentSession(TextFormat, "fox", storage)
        session.open("/a.txt")
        assertEquals("hare", session.history.document.value)
        assertFalse(session.edited)
        session.restore("hare, recovered", "/a.txt")
        assertTrue(session.edited, "a recovered document still needs saving")
        session.saveTo("/a.txt")
        assertFalse(session.edited)
    }

    @Test
    fun recentFilesPutTheLatestFirst() = runTest {
        val storage = MemoryStorage().apply { files["/b"] = byteArrayOf() }
        val recent = RecentFiles(listOf("/a", "/b"), limit = 3)
        recent.add("/c")
        recent.add("/b")
        assertEquals(listOf("/b", "/c", "/a"), recent.entries)
        recent.add("/d")
        assertEquals(listOf("/d", "/b", "/c"), recent.entries)
        recent.prune(storage)
        assertEquals(listOf("/b"), recent.entries)
    }

    @Test
    fun closingAnEditedDocumentAsksFirst() = runComposeUiTest {
        val storage = MemoryStorage()
        val session = DocumentSession(TextFormat, "fox", storage)
        var closed = 0
        setStretchContent { UnsavedChangesDialog(session, PickedFile("/docs/Fox.txt")) }

        session.whenSaved { closed++ }
        assertEquals(1, closed, "nothing to save, so it closes at once")

        session.history.execute(Append("!"))
        session.whenSaved { closed++ }
        waitForIdle()
        onNodeWithText("Save the changes to Untitled first?").assertExists()
        onNodeWithText("Cancel").performClick()
        assertEquals(1, closed)
        onNodeWithText("Unsaved changes").assertDoesNotExist()

        session.whenSaved { closed++ }
        waitForIdle()
        onNodeWithText("Save").performClick()
        waitUntil { closed == 2 }
        assertEquals("fox!", storage.files.getValue("/docs/Fox.txt").decodeToString())
        assertFalse(session.edited)

        session.history.execute(Append("?"))
        session.whenSaved { closed++ }
        waitForIdle()
        onNodeWithText("Don’t save").performClick()
        assertEquals(3, closed)
        assertEquals("fox!", storage.files.getValue("/docs/Fox.txt").decodeToString())
    }
}
