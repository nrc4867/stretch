rootProject.name = "stretch"

pluginManagement {
    includeBuild("stretch-gradle-plugin")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// Common tests run on the desktop target, so `check` needs no browser or Node.
gradle.startParameter.excludedTaskNames += listOf("wasmJsTest", "wasmJsBrowserTest", "kotlinWasmStoreYarnLock")

include(":stretch-core")
include(":stretch-theme")
include(":stretch-ui")
include(":stretch-frame")
include(":stretch-document")
include(":stretch-input")
include(":stretch-gpu")
include(":stretch-gpu-native")
include(":stretch-test")
include(":stretch-devtools")
include(":gallery")
include(":stretch-code")
include(":stretch-diff")
include(":stretch-markdown")
include(":stretch-web")
include(":stretch-mermaid")
