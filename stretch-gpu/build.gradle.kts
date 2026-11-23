plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())
    jvm("desktop")
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { browser() }

    sourceSets {
        commonMain.dependencies {
            api(project(":stretch-frame"))
        }
        val desktopMain by getting {
            dependencies {
                // WebGPU through wgpu-native: the bindings, kept out of apps' sight, and the library for every
                // platform, so an app runs wherever it is packaged.
                implementation(libs.webgpu.native.ffm)
                for (platform in listOf("windows-x64", "linux-x64", "linux-arm64", "macos-x64", "macos-arm64")) {
                    runtimeOnly(project.dependencies.variantOf(libs.webgpu.native.ffm) { classifier("natives-$platform") })
                }
            }
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(project(":stretch-test"))
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
}
