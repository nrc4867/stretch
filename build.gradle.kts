plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
}

allprojects {
    group = "dev.chieppa.stretch"
    version = "0.1.0-SNAPSHOT"
}

// The Gradle plugin is its own build; check runs its tests too.
tasks.register("check") {
    dependsOn(gradle.includedBuild("stretch-gradle-plugin").task(":check"))
}
