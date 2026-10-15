pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

// Intentionally a separate build: no includeBuild or project dependency.
rootProject.name = "published-library-consumer"
