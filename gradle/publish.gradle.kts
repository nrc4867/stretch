// Publishes the applying project to GitHub Packages, in the repository named by GITHUB_REPOSITORY (set by Actions).
// Credentials come from ORG_GRADLE_PROJECT_GitHubPackagesUsername and ORG_GRADLE_PROJECT_GitHubPackagesPassword.

val defaultRepository = "nrc4867/stretch"

apply(plugin = "maven-publish")

configure<PublishingExtension> {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/${providers.environmentVariable("GITHUB_REPOSITORY").getOrElse(defaultRepository)}")
            credentials(PasswordCredentials::class)
        }
    }
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set(project.name)
            licenses {
                license {
                    name.set("AGPL-3.0-only")
                    url.set("https://www.gnu.org/licenses/agpl-3.0.txt")
                }
            }
        }
    }
}

// Kotlin Multiplatform and java-gradle-plugin create their own publications; a plain Java library needs one.
plugins.withId("java-library") {
    configure<JavaPluginExtension> { withSourcesJar() }
    configure<PublishingExtension> {
        publications.create<MavenPublication>("maven") { from(components["java"]) }
    }
}
