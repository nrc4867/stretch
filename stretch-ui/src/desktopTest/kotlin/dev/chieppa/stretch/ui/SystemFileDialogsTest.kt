package dev.chieppa.stretch.ui

import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SystemFileDialogsTest {
    @Test
    fun aKindListsItsExtensionsAsPatterns() {
        val kinds = listOf(FileKind("Images", setOf("png", "jpg")), FileKind("Anything", emptySet()))
        assertEquals(listOf("Images" to "*.png;*.jpg", "Anything" to "*.*"), filterSpecs(kinds))
    }

    @Test
    fun aKindCoversItsOwnFilesOnly() {
        val images = FileKind("Images", setOf("png", "jpg"))
        assertTrue(images.covers("sketch.PNG"))
        assertFalse(images.covers("sketch.squash"))
        assertTrue(FileKind("Anything", emptySet()).covers("sketch.squash"))
    }

    @Test
    fun aKindWithNoNameIsNamedByItsExtensions() {
        assertEquals("PNG, JPG", extensionsName(linkedSetOf("png", "jpg")))
    }

    @Test
    fun aSavedNameGetsTheExtensionItLacks() {
        assertEquals("fennec.squash", withExtension("fennec", "squash"))
        assertEquals("fennec.SQUASH", withExtension("fennec.SQUASH", "squash"))
        assertEquals("notes", withExtension("notes", ""))
    }

    @Test
    fun theShellsDialogTakesItsTypesOptionsAndName() {
        if (!System.getProperty("os.name").orEmpty().startsWith("Windows")) return
        val (options, name) = assertNotNull(WindowsFileDialog.probe("fennec"), "the shell makes a save dialog")
        assertTrue(options and 0x2 != 0, "the overwrite prompt is on, so the options went to the right method")
        assertEquals("fennec", name, "the name reads back from the method after it")
    }

    @Test
    fun theShellsFolderDialogPicksFoldersAndStartsInTheOneGiven() {
        if (!System.getProperty("os.name").orEmpty().startsWith("Windows")) return
        val start = createTempDirectory("folder-dialog").toFile()
        try {
            val (options, folder) = assertNotNull(WindowsFileDialog.probeFolder(start.path), "the shell makes an open dialog")
            assertTrue(options and 0x20 != 0, "FOS_PICKFOLDERS is on")
            assertEquals(start.canonicalPath, folder?.let { File(it).canonicalPath }, "the dialog opens in the start folder")
        } finally {
            start.delete()
        }
    }
}
