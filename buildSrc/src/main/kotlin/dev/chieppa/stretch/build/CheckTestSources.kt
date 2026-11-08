package dev.chieppa.stretch.build

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * What tests may not do, each with the reason the failure gives. Tests wait on events, idling or a virtual clock,
 * never on time passing, and reach only what their module makes visible to them.
 */
val forbiddenInTests: List<Pair<Regex, String>> = listOf(
    Regex("""Thread\.sleep\(""") to "waits on the clock; wait for an event, waitForIdle or the virtual clock",
    Regex("""\bdelay\(""") to "waits on the clock; advance a test dispatcher or mainClock instead",
    Regex("""\bwaitUntil\w*\(""") to "polls against the clock; wait for an event or register the work as an idling resource",
    Regex("""\b(try)?[sS]etAccessible\b""") to "widens visibility by reflection; make it internal instead",
    Regex("""\bisAccessible\s*=\s*true""") to "widens visibility by reflection; make it internal instead",
)

/** Fails when a test source in [sources] does something in [forbiddenInTests], naming each file and line. */
@CacheableTask
abstract class CheckTestSources : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    /** Written when the check passes, so an unchanged set of sources is not read again. */
    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun run() {
        val found = sources.files.sorted().flatMap { file ->
            file.readLines().withIndex().flatMap { (index, line) ->
                forbiddenInTests.filter { (pattern, _) -> pattern.containsMatchIn(line) }
                    .map { (pattern, reason) -> "${file.path}:${index + 1}: ${pattern.find(line)!!.value} $reason" }
            }
        }
        if (found.isNotEmpty()) throw GradleException("Tests must not wait on time or widen visibility:\n" + found.joinToString("\n"))
        report.get().asFile.apply { parentFile.mkdirs() }.writeText("${sources.files.size} test sources checked\n")
    }
}
