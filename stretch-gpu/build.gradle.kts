plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            api(project(":stretch-frame"))
        }
        val desktopMain by getting {
            dependencies {
                // WebGPU through wgpu-native, bound with the Foreign Function & Memory API.
                api(libs.wgpu4k)
                // The WebGPU types and descriptors scenes build pipelines with.
                api(libs.webgpu.ktypes)
                api(libs.webgpu.ktypes.descriptors)
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
