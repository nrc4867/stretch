plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("dev.chieppa.stretch.app")
}

stretchApp {
    name = "Stretch Gallery"
    id = "dev.chieppa.stretch.gallery"
    icon = layout.projectDirectory.file("app-icon.png")
    vendor = "Nicholas Chieppa"
}

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())
}

dependencies {
    implementation(project(":stretch-frame"))
    implementation(project(":stretch-gpu"))
    // Quiets wgpu4k's SLF4J logging; an app that wants the log adds its own SLF4J backend instead.
    runtimeOnly(libs.slf4j.nop)
    implementation(project(":stretch-document"))
    implementation(project(":stretch-core"))
    implementation(project(":stretch-devtools"))
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
    testImplementation(kotlin("test"))
    testImplementation(libs.compose.ui.test)
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

compose.desktop {
    application {
        mainClass = "dev.chieppa.stretch.gallery.MainKt"
        // The JetBrains Runtime, so StretchWindow can draw its own title bar in a native frame.
        javaHome = javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(libs.versions.jvmToolchain.get().toInt()))
            vendor.set(JvmVendorSpec.JETBRAINS)
        }.get().metadata.installationPath.asFile.absolutePath
        jvmArgs += listOf("--enable-native-access=ALL-UNNAMED")
        // -Pdevtools=<port> serves screenshots of the gallery's windows on 127.0.0.1 (see DevTools).
        providers.gradleProperty("devtools").orNull?.let { jvmArgs += "-Dstretch.devtools=$it" }
        // -Pgpu adds the WebGPU viewport page and opens on it.
        if (providers.gradleProperty("gpu").isPresent) jvmArgs += "-Dstretch.gpu=true"
    }
}
