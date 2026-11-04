import dev.chieppa.stretch.gradle.GenerateAppIdentity

plugins {
    alias(libs.plugins.kotlinMultiplatform)
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
    jvm("desktop")
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        // ./gradlew :gallery:wasmJsBrowserDistribution writes the static site to build/dist/wasmJs/productionExecutable.
        outputModuleName = "gallery"
        browser {
            commonWebpackConfig { outputFileName = "gallery.js" }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":stretch-frame"))
            implementation(project(":stretch-document"))
            implementation(project(":stretch-core"))
            implementation(project(":stretch-gpu"))
        }
        val desktopMain by getting {
            resources.srcDir(tasks.named<GenerateAppIdentity>("generateAppIdentity").flatMap { it.resources })
            dependencies {
                implementation(project(":stretch-devtools"))
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.compose.ui.test)
            }
        }
    }
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
