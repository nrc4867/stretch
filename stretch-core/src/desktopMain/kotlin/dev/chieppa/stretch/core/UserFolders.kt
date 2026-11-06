package dev.chieppa.stretch.core

import java.io.File

private const val HOME_PROPERTY = "stretch.home"

/**
 * The folder every Stretch app shares for this user, for what is too big for each app to keep its own, such as a
 * fetched Java runtime: `%LOCALAPPDATA%\Stretch` on Windows, `~/Library/Application Support/Stretch` on macOS and
 * `$XDG_DATA_HOME/stretch` on Linux. `-Dstretch.home` moves it.
 */
fun stretchUserFolder(): File {
    System.getProperty(HOME_PROPERTY)?.let { return File(it) }
    val os = System.getProperty("os.name").orEmpty()
    val home = System.getProperty("user.home")
    return when {
        os.startsWith("Windows") -> File(System.getenv("LOCALAPPDATA") ?: "$home\\AppData\\Local", "Stretch")
        os.startsWith("Mac") -> File(home, "Library/Application Support/Stretch")
        else -> File(System.getenv("XDG_DATA_HOME") ?: "$home/.local/share", "stretch")
    }
}
