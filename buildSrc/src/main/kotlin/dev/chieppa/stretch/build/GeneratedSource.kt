package dev.chieppa.stretch.build

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

/**
 * A Kotlin file generated from a design source and checked in. With [verify] set, the task fails when the checked-in
 * file differs from what the source produces instead of writing it.
 */
abstract class GeneratedSource : DefaultTask() {
    @get:Internal
    abstract val target: RegularFileProperty

    @get:Input
    abstract val verify: Property<Boolean>

    init {
        verify.convention(false)
    }

    protected abstract fun render(): String

    @TaskAction
    fun run() {
        val file = target.get().asFile
        val text = render()
        if (verify.get()) {
            if (!file.exists() || file.readText().replace("\r\n", "\n") != text) {
                throw GradleException("${file.name} is out of date; run the generate task that writes it")
            }
        } else {
            file.parentFile.mkdirs()
            file.writeText(text)
        }
    }
}

internal fun String.camelCase(): String =
    split('-', '.', '_').filter { it.isNotEmpty() }.mapIndexed { i, part ->
        if (i == 0) part.lowercase() else part.replaceFirstChar { it.uppercase() }
    }.joinToString("")

internal fun String.pascalCase(): String = camelCase().replaceFirstChar { it.uppercase() }

internal fun Double.kt(): String {
    val rounded = Math.round(this * 1000) / 1000.0
    return if (rounded == Math.floor(rounded)) rounded.toLong().toString() else rounded.toString()
}
