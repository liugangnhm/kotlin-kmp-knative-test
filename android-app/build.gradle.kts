plugins {
    id("com.android.application")
}

repositories {
    google()
    mavenCentral()
}

android {
    namespace = "com.example.kmpdemo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.kmpdemo"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":native-lib"))
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
