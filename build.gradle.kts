plugins {
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
)

subprojects {
    if (name in publishedModules) apply(from = rootProject.file("gradle/publish.gradle.kts"))
}

// The Gradle plugin is its own build; check runs its tests too.
tasks.register("check") {
    dependsOn(gradle.includedBuild("stretch-gradle-plugin").task(":check"))
}

// So is its publishing.
tasks.register("publish") {
    dependsOn(gradle.includedBuild("stretch-gradle-plugin").task(":publish"))
}
