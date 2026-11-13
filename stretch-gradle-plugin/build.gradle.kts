plugins {
    `kotlin-dsl`
}

group = "dev.chieppa.stretch"
version = providers.gradleProperty("stretch.version").getOrElse("0.1.0-SNAPSHOT")

apply(from = "../gradle/publish.gradle.kts")

dependencies {
    compileOnly("org.jetbrains.compose:compose-gradle-plugin:1.12.2")
    // Draws an SVG app icon at each size (MIT).
    implementation("com.github.weisj:jsvg:2.1.0")
    testImplementation(kotlin("test"))
}

gradlePlugin {
    plugins {
        create("stretchApp") {
            id = "dev.chieppa.stretch.app"
            implementationClass = "dev.chieppa.stretch.gradle.StretchAppPlugin"
        }
    }
}
