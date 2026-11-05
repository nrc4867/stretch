plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.jvmToolchain.get().toInt()))
        vendor.set(JvmVendorSpec.JETBRAINS)
    }
    jvm("desktop")
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { browser() }

    sourceSets {
        commonMain.dependencies {
            api(project(":stretch-markdown"))
            api(project(":stretch-web"))
            implementation(libs.compose.components.resources)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.compose.ui.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        val desktopTest by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
    }
}

compose.resources {
    packageOfResClass = "dev.chieppa.stretch.mermaid.generated"
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    // These start Chromium, so they run only with -Pcef.
    if (providers.gradleProperty("cef").isPresent) {
        systemProperty("stretch.web.test", "true")
        exclude("**/*EnginelessTest*")
    } else {
        exclude("**/*EngineTest*")
        jvmArgs("--limit-modules", providers.gradleProperty("stretch.modulesWithoutEngine").get())
    }
    systemProperty("stretch.web.output", layout.buildDirectory.dir("engine-test").get().asFile.path)
}
