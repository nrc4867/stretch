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
            api(project(":stretch-core"))
            api(project(":stretch-theme"))
            api(kotlin("test"))
            api(libs.kotlinx.coroutines.test)
            api(libs.compose.ui.test)
        }
        val desktopMain by getting {
            dependencies {
                // Not currentOs: that would tie apps' tests to the operating system that published this.
                api(libs.compose.desktop)
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
