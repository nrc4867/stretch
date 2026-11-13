package dev.chieppa.stretch.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.util.Properties

/**
 * Writes the app's identity from its name, id and icon: the icon files the packager puts on the launcher and
 * installer ([packaging]), and the resources the running app reads to name and badge its windows ([resources]).
 */
@DisableCachingByDefault(because = "It writes a few small files quickly.")
abstract class GenerateAppIdentity : DefaultTask() {
    @get:Input
    abstract val appName: Property<String>

    @get:Input
    abstract val appId: Property<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val icon: RegularFileProperty

    /** Hand-tuned SVGs for single sizes, `<icon name>-<size>.svg` beside an SVG icon. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val iconVariants: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val packaging: DirectoryProperty

    @get:OutputDirectory
    abstract val resources: DirectoryProperty

    @TaskAction
    fun generate() {
        val source = IconSource.read(icon.get().asFile)
        val pack = packaging.get().asFile.apply { deleteRecursively(); mkdirs() }
        pack.resolve("icon.ico").writeBytes(AppIcons.ico(source))
        pack.resolve("icon.icns").writeBytes(AppIcons.icns(source))
        pack.resolve("icon.png").writeBytes(AppIcons.png(source.at(512)))

        val res = resources.get().asFile.apply { deleteRecursively() }.resolve(RESOURCE_DIR).apply { mkdirs() }
        AppIcons.windowSizes.forEach { size -> res.resolve("icon-$size.png").writeBytes(AppIcons.png(source.at(size))) }
        // For the launcher an app run from jars makes for itself (AppLauncher in stretch-frame).
        listOf("icon.ico", "icon.icns", "icon.png").forEach { pack.resolve(it).copyTo(res.resolve(it), overwrite = true) }
        val props = Properties().apply {
            setProperty("name", appName.get())
            setProperty("id", appId.get())
            setProperty("icons", AppIcons.windowSizes.joinToString(","))
        }
        res.resolve("app.properties").outputStream().use { props.store(it, null) }
    }

    companion object {
        /** Where the running app finds its identity on the classpath; read by `AppIdentity` in stretch-frame. */
        const val RESOURCE_DIR = "stretch-app"
    }
}
