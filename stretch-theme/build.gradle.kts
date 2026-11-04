import dev.chieppa.stretch.build.GenerateIcons
import dev.chieppa.stretch.build.GenerateTokens

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
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.ui)
            implementation(libs.compose.components.resources)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

compose.resources {
    packageOfResClass = "dev.chieppa.stretch.theme.generated"
}

val generatedDir = "src/commonMain/kotlin/dev/chieppa/stretch/theme"
val tokensJson = rootProject.layout.projectDirectory.file("design/tokens.json")
val iconsDir = rootProject.layout.projectDirectory.dir("design/icons")

fun GenerateTokens.configure(verify: Boolean) {
    tokens.set(tokensJson)
    target.set(layout.projectDirectory.file("$generatedDir/Tokens.kt"))
    this.verify.set(verify)
}

fun GenerateIcons.configure(verify: Boolean) {
    icons.set(iconsDir)
    target.set(layout.projectDirectory.file("$generatedDir/StretchIcons.kt"))
    this.verify.set(verify)
}

tasks.register<GenerateTokens>("generateTokens") { group = "stretch"; configure(verify = false) }
tasks.register<GenerateIcons>("generateIcons") { group = "stretch"; configure(verify = false) }
val verifyTokens = tasks.register<GenerateTokens>("verifyTokens") { group = "verification"; configure(verify = true) }
val verifyIcons = tasks.register<GenerateIcons>("verifyIcons") { group = "verification"; configure(verify = true) }

tasks.named("check") { dependsOn(verifyTokens, verifyIcons) }
