package dev.chieppa.stretch.frame

/** A crash in one line, as a held tab shows it. */
internal object CrashSummary {
    /** [cause]'s kind and the first line of its message, or its kind alone when it has no message. */
    fun of(cause: Throwable): String {
        val kind = cause::class.simpleName ?: cause.toString()
        val line = cause.message?.lineSequence()?.map { it.trim() }?.firstOrNull { it.isNotEmpty() }
        return if (line == null) kind else "$kind: $line"
    }
}
