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
    // -PfetchEngine packages without Chromium; the app fetches it when the documents page asks.
    fetchWebEngine = providers.gradleProperty("fetchEngine").isPresent
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
            implementation(project(":stretch-diff"))
            implementation(project(":stretch-code"))
            implementation(project(":stretch-console"))
            implementation(project(":stretch-markdown"))
            implementation(project(":stretch-web"))
            implementation(project(":stretch-mermaid"))
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
                implementation(project(":stretch-test"))
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    // -Pcef draws the documents page with the real engine, which needs the JetBrains Runtime and a while.
    if (providers.gradleProperty("cef").isPresent) {
        javaLauncher.set(javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(libs.versions.jvmToolchain.get().toInt()))
            vendor.set(JvmVendorSpec.JETBRAINS)
        })
        exclude("**/*EnginelessTest*")
    } else {
        exclude("**/*EngineTest*")
        jvmArgs("--limit-modules", providers.gradleProperty("stretch.modulesWithoutEngine").get())
    }
    systemProperty("stretch.web.output", layout.buildDirectory.dir("documents-pages").get().asFile.path)
}

compose.desktop {
    application {
        mainClass = "dev.chieppa.stretch.gallery.MainKt"
        // The JetBrains Runtime, so StretchWindow can draw its own title bar in a native frame.
        javaHome = javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(libs.versions.jvmToolchain.get().toInt()))
            vendor.set(JvmVendorSpec.JETBRAINS)
        }.get().metadata.installationPath.asFile.absolutePath
        // -PjavaHome=<folder> runs on another Java, such as one without JCEF, where the documents page offers to fetch the engine.
        providers.gradleProperty("javaHome").orNull?.let { javaHome = it }
        jvmArgs += listOf("--enable-native-access=ALL-UNNAMED")
        // -Pdevtools=<port> serves screenshots of the gallery's windows on 127.0.0.1 (see DevTools).
        providers.gradleProperty("devtools").orNull?.let {
            jvmArgs += "-Dstretch.devtools=$it"
            nativeDistributions.modules("jdk.httpserver")
        }
        // -Pgpu adds the WebGPU viewport page and opens on it.
        if (providers.gradleProperty("gpu").isPresent) jvmArgs += "-Dstretch.gpu=true"
        // -Pdocuments opens on the diff viewer, markdown editor and HTML view.
        if (providers.gradleProperty("documents").isPresent) jvmArgs += "-Dstretch.documents=true"
    }
}
