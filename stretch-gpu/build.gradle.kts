import dev.chieppa.stretch.build.WebgpuNatives

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
                // WebGPU through wgpu-native's bindings, kept out of apps' sight. The library itself comes in a
                // natives jar per platform, which each app takes for the platform it packages for.
                implementation(libs.webgpu.native.ffm)
            }
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(project(":stretch-test"))
        }
        val desktopTest by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                runtimeOnly(project.dependencies.variantOf(libs.webgpu.native.ffm) { classifier(WebgpuNatives.classifier(providers)) })
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
