plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    // The desktop engine is the JCEF inside the JetBrains Runtime, so this module compiles against that runtime.
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.jvmToolchain.get().toInt()))
        vendor.set(JvmVendorSpec.JETBRAINS)
    }
    jvm("desktop")
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { browser() }

    sourceSets {
        commonMain.dependencies {
            api(project(":stretch-ui"))
            api(project(":stretch-core"))
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.compose.ui.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        val desktopMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    // The engine needs a display and starts Chromium, so its own tests run only with -Pcef.
    if (providers.gradleProperty("cef").isPresent) {
        systemProperty("stretch.web.test", "true")
        exclude("**/*EnginelessTest*")
    } else {
        exclude("**/*EngineTest*")
        jvmArgs("--limit-modules", providers.gradleProperty("stretch.modulesWithoutEngine").get())
    }
    systemProperty("stretch.web.output", layout.buildDirectory.dir("engine-test").get().asFile.path)
}
