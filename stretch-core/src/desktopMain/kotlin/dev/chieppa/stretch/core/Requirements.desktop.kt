package dev.chieppa.stretch.core

import java.util.ServiceLoader

/** Modules list theirs as `META-INF/services/dev.chieppa.stretch.core.Requirement`. */
internal actual fun platformRequirements(): List<Requirement> =
    ServiceLoader.load(Requirement::class.java, Requirement::class.java.classLoader).toList()
