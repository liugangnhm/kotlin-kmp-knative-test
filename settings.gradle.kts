pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "kmp-knative-c-demo"
include(":native-lib", ":demo-app", ":stats-app", ":android-cbridge", ":android-app")
