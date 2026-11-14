import dev.chieppa.stretch.build.CheckTestSources

plugins {
    base
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
}

allprojects {
    group = "dev.chieppa.stretch"
    version = providers.gradleProperty("stretch.version").getOrElse("0.1.0-SNAPSHOT")
}

/** The modules apps build on. The gallery and devtools stay out. */
val publishedModules = setOf(
    "stretch-core",
    "stretch-theme",
    "stretch-ui",
    "stretch-frame",
    "stretch-document",
    "stretch-input",
    "stretch-gpu",
    "stretch-gpu-native",
    "stretch-test",
    "stretch-code",
    "stretch-diff",
    "stretch-console",
    "stretch-markdown",
    "stretch-web",
    "stretch-mermaid",
)

subprojects {
    if (name in publishedModules) apply(from = rootProject.file("gradle/publish.gradle.kts"))

    // Tests wait on events, never on time, and leave visibility alone; check fails on one that does.
    val checkTestSources = tasks.register<CheckTestSources>("checkTestSources") {
        group = "verification"
        sources.from(fileTree("src") { include("*Test/**/*.kt", "test/**/*.kt", "test/**/*.java") })
        report.set(layout.buildDirectory.file("checkTestSources.txt"))
    }
    tasks.matching { it.name == "check" }.configureEach { dependsOn(checkTestSources) }

    // Tests named *ScreenTest open real windows, so they run only with -Pscreen; check opens none. Tests named
    // *GpuTest need a real GPU adapter, so they run only with -Pgpu.
    tasks.withType<Test>().configureEach {
        if (!providers.gradleProperty("screen").isPresent) exclude("**/*ScreenTest*")
        if (!providers.gradleProperty("gpu").isPresent) exclude("**/*GpuTest*")
    }
}

val checkPluginTestSources = tasks.register<CheckTestSources>("checkTestSources") {
    group = "verification"
    sources.from(fileTree("stretch-gradle-plugin/src") { include("*Test/**/*.kt", "test/**/*.kt") })
    report.set(layout.buildDirectory.file("checkTestSources.txt"))
}

// The Gradle plugin is its own build; check runs its tests too.
tasks.named("check") {
    dependsOn(gradle.includedBuild("stretch-gradle-plugin").task(":check"), checkPluginTestSources)
}

// So is its publishing.
tasks.register("publish") {
    dependsOn(gradle.includedBuild("stretch-gradle-plugin").task(":publish"))
}
