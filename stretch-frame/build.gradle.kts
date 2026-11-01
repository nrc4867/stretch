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
            api(project(":stretch-ui"))
            api(project(":stretch-core"))
            implementation(libs.compose.components.resources)
        }
        val desktopMain by getting {
            dependencies {
                implementation(libs.jna)
                implementation(libs.jna.platform)
            }
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(project(":stretch-test"))
        }
    }
}

compose.resources {
    packageOfResClass = "dev.chieppa.stretch.frame.generated"
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
