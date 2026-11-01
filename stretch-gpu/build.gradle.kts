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
                val wgpu4k = libs.wgpu4k.get()
                api("${wgpu4k.module}:${wgpu4k.versionConstraint.requiredVersion}") {
                    // wgpu4k logs through SLF4J and brings logback (EPL-1.0 or LGPL-2.1) as the backend. Logging
                    // is the app's choice, and Stretch takes only permissive licenses.
                    exclude(group = "ch.qos.logback")
                }
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
