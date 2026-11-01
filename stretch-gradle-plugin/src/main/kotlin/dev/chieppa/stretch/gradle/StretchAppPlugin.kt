package dev.chieppa.stretch.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.gradle.api.tasks.SourceSetContainer
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.desktop.DesktopExtension
import java.util.UUID

/**
 * What the app is called and how it looks to the OS. Set once; the plugin carries it to the launcher, the installers
 * and the running app, so the taskbar, dock, Start menu and Task Manager all show the app rather than Java.
 */
abstract class StretchAppExtension {
    /** The name people see: launcher, window grouping, Task Manager, menus. */
    abstract val name: Property<String>

    /** A reverse-DNS id, like `dev.chieppa.squash`: the macOS bundle id, and the seed for the Windows upgrade id. */
    abstract val id: Property<String>

    /** A square PNG, 1024px for best results, at least 256px. */
    abstract val icon: RegularFileProperty

    /** Who makes it, shown in installers and file properties. */
    abstract val vendor: Property<String>
}

/**
 * `id("dev.chieppa.stretch.app")` on a Compose desktop app: configure `stretchApp { name; id; icon }` and the
 * packaged launcher, installers and running windows all carry that identity. Apply it with the Compose plugin and a
 * Kotlin JVM app module.
 */
class StretchAppPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val app = project.extensions.create("stretchApp", StretchAppExtension::class.java)
        val generate = project.tasks.register("generateAppIdentity", GenerateAppIdentity::class.java) {
            group = "stretch"
            description = "Writes the app's icons and identity resources from stretchApp { }"
            appName.set(app.name)
            appId.set(app.id)
            icon.set(app.icon)
            packaging.set(project.layout.buildDirectory.dir("stretch-app/packaging"))
            resources.set(project.layout.buildDirectory.dir("stretch-app/resources"))
        }

        project.pluginManager.withPlugin("java") {
            project.extensions.getByType(SourceSetContainer::class.java).getByName("main").resources.srcDir(generate.flatMap { it.resources })
        }

        project.pluginManager.withPlugin("org.jetbrains.compose") {
            project.afterEvaluate {
                val desktop = (project.extensions.getByType(ComposeExtension::class.java) as ExtensionAware)
                    .extensions.getByType(DesktopExtension::class.java)
                val dist = desktop.application.nativeDistributions
                val name = app.name.get()
                val packaging = generate.flatMap { it.packaging }
                dist.packageName = dist.packageName ?: name
                // Installers want a plain dotted number; a snapshot version like 0.1.0-SNAPSHOT becomes 0.1.0.
                dist.packageVersion = dist.packageVersion ?: packageVersion(project.version.toString())
                // Windows shows this as the launcher's file description: the name in Task Manager.
                dist.description = dist.description ?: name
                dist.vendor = dist.vendor ?: app.vendor.orNull
                dist.windows.iconFile.convention(packaging.map { it.file("icon.ico") })
                dist.windows.menuGroup = dist.windows.menuGroup ?: name
                dist.windows.upgradeUuid = dist.windows.upgradeUuid ?: UUID.nameUUIDFromBytes(app.id.get().toByteArray()).toString()
                dist.macOS.iconFile.convention(packaging.map { it.file("icon.icns") })
                dist.macOS.bundleID = dist.macOS.bundleID ?: app.id.get()
                dist.macOS.dockName = dist.macOS.dockName ?: name
                dist.linux.iconFile.convention(packaging.map { it.file("icon.png") })
                dist.linux.packageName = dist.linux.packageName ?: name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
            }
        }
    }
}

/** The leading `1.2.3` of a project version, or 1.0.0 when it has none. */
internal fun packageVersion(version: String): String =
    Regex("""^\d+(\.\d+){0,2}""").find(version)?.value ?: "1.0.0"
