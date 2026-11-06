package dev.chieppa.stretch.frame

/** The contents of the files that make a launcher, kept apart from writing them so they can be checked. */
internal object LauncherFiles {
    /** The program name inside a macOS bundle, and the bundle's icon file. */
    const val MAC_PROGRAM = "launcher"
    const val MAC_ICON = "icon.icns"

    /** A macOS bundle's `Info.plist` for an app called [name] with [id]. */
    fun infoPlist(name: String, id: String): String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
        <plist version="1.0">
        <dict>
          <key>CFBundleName</key><string>${xml(name)}</string>
          <key>CFBundleDisplayName</key><string>${xml(name)}</string>
          <key>CFBundleIdentifier</key><string>${xml(id)}</string>
          <key>CFBundleExecutable</key><string>$MAC_PROGRAM</string>
          <key>CFBundleIconFile</key><string>$MAC_ICON</string>
          <key>CFBundlePackageType</key><string>APPL</string>
          <key>NSHighResolutionCapable</key><true/>
        </dict>
        </plist>
    """.trimIndent() + "\n"

    /** A shell script that replaces itself with [command], so the app is the bundle's own process. */
    fun shellScript(command: List<String>): String = "#!/bin/sh\nexec " + command.joinToString(" ") { shellQuoted(it) } + "\n"

    /** A Linux desktop entry that runs [command], for an app whose windows have the class [name]. */
    fun desktopEntry(name: String, command: List<String>, icon: String): String = buildString {
        appendLine("[Desktop Entry]")
        appendLine("Type=Application")
        appendLine("Name=$name")
        appendLine("Exec=" + command.joinToString(" ") { desktopQuoted(it) })
        appendLine("Icon=$icon")
        appendLine("StartupWMClass=$name")
        appendLine("Terminal=false")
    }

    /** [arguments] as a Java `@` argument file: one to a line, quoted, which keeps long class paths out of the command line. */
    fun argumentFile(arguments: List<String>): String =
        arguments.joinToString("") { "\"" + it.replace("\\", "\\\\").replace("\"", "\\\"") + "\"\n" }

    /** [arguments] as one Windows command line, quoted the way Windows programs read them back. */
    fun windowsArguments(arguments: List<String>): String = arguments.joinToString(" ") { windowsQuoted(it) }

    private fun xml(text: String) = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun shellQuoted(text: String) = "'" + text.replace("'", "'\\''") + "'"

    private fun desktopQuoted(text: String): String =
        "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"").replace("`", "\\`").replace("$", "\\$").replace("%", "%%") + "\""

    private fun windowsQuoted(text: String): String {
        if (text.isNotEmpty() && text.none { it == ' ' || it == '\t' || it == '"' }) return text
        val out = StringBuilder("\"")
        var slashes = 0
        for (char in text) {
            when (char) {
                '\\' -> slashes++
                '"' -> {
                    out.append("\\".repeat(slashes * 2 + 1)).append('"')
                    slashes = 0
                }
                else -> {
                    out.append("\\".repeat(slashes)).append(char)
                    slashes = 0
                }
            }
        }
        return out.append("\\".repeat(slashes * 2)).append('"').toString()
    }
}
