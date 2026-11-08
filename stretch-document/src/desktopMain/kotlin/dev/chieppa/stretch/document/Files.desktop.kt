package dev.chieppa.stretch.document

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.LocalAwtWindow
import java.awt.Frame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import org.jetbrains.compose.resources.stringResource
import dev.chieppa.stretch.ui.FileKind
import dev.chieppa.stretch.ui.SystemFileDialogs
import dev.chieppa.stretch.ui.extensionsName
import dev.chieppa.stretch.document.generated.Res
import dev.chieppa.stretch.document.generated.choose_open
import dev.chieppa.stretch.document.generated.choose_save

/**
 * Documents on the local file system, read and written on the IO dispatcher. Writes go to a temporary file beside the
 * target, then replace it in one move.
 */
object FileStorage : DocumentStorage {
    override suspend fun read(path: String): ByteArray = withContext(Dispatchers.IO) { Files.readAllBytes(Path.of(path)) }

    override suspend fun write(path: String, bytes: ByteArray) = withContext(Dispatchers.IO) { writeFile(path, bytes) }

    internal fun writeFile(path: String, bytes: ByteArray) {
        val target = Path.of(path).toAbsolutePath()
        Files.createDirectories(target.parent)
        val temp = Files.createTempFile(target.parent, ".${target.fileName}", ".tmp")
        try {
            Files.write(temp, bytes)
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temp)
        }
    }

    override suspend fun exists(path: String): Boolean = withContext(Dispatchers.IO) { Files.isRegularFile(Path.of(path)) }

    override fun displayName(path: String): String = Path.of(path).fileName.toString().substringBeforeLast('.')
}

/**
 * The OS's own open and save dialogs, over [owner], through [SystemFileDialogs]: the format's files listed under its description (every
 * type it reads when opening, its own type when saving), and its extension added to a name typed without one.
 */
class SystemFileChooser(private val owner: Frame?, private val openTitle: String, private val saveTitle: String) : FileChooser {
    override suspend fun chooseOpen(format: DocumentFormat<*>): String? =
        SystemFileDialogs.open(openTitle, listOf(kindOf(format, format.openExtensions)), multiple = false, owner = owner).firstOrNull()

    override suspend fun chooseSave(format: DocumentFormat<*>, suggestedName: String): String? =
        SystemFileDialogs.save(saveTitle, listOf(kindOf(format, setOf(format.extension))), suggestedName, format.extension, owner)

    private fun kindOf(format: DocumentFormat<*>, extensions: Set<String>) = FileKind(format.description.ifBlank { extensionsName(extensions) }, extensions)
}

actual val documentStorage: DocumentStorage = FileStorage

/** A [SystemFileChooser] over the window this is composed in. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun rememberFileChooser(): FileChooser = rememberFileChooser(LocalAwtWindow.current as? Frame)

/** A [SystemFileChooser] over [owner], the app's window, with dialog titles in the app's language. */
@Composable
fun rememberFileChooser(owner: Frame?): FileChooser {
    val open = stringResource(Res.string.choose_open)
    val save = stringResource(Res.string.choose_save)
    return remember(owner, open, save) { SystemFileChooser(owner, open, save) }
}
