plugins {
    id("com.android.library")
    `maven-publish`
}

group = "com.example"
version = "1.0.0"

repositories {
    google()
    mavenCentral()
}

android {
    namespace = "com.example.androidcbridge"
    compileSdk = 36
    ndkVersion = "28.2.13676358"

    defaultConfig {
        minSdk = 24
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }
    externalNativeBuild {
        cmake {
            path = file("CMakeLists.txt")
            version = "3.22.1"
        }
    }
    publishing {
        singleVariant("release")
    }
}

publishing {
    repositories {
        maven {
            name = "Demo"
            url = rootProject.layout.projectDirectory.dir("build/demo-repo").asFile.toURI()
        }
    }
    publications {
        register<MavenPublication>("release") {
            afterEvaluate { from(components["release"]) }
        }
    }
}
