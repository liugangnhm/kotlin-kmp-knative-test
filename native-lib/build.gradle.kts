plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    `maven-publish`
}

group = "com.example"
version = "1.0.0"

repositories {
    google()
    mavenCentral()
}

val cDirectory = layout.projectDirectory.dir("src/nativeInterop/c")
val cBuildDirectory = layout.buildDirectory.dir("native/c")
val cObject = cBuildDirectory.map { it.file("demo.o") }
val cLibrary = cBuildDirectory.map { it.file("libdemo.a") }

val compileC by tasks.registering(Exec::class) {
    inputs.files(cDirectory.file("demo.c"), cDirectory.file("demo.h"))
    outputs.file(cObject)
    doFirst { cBuildDirectory.get().asFile.mkdirs() }
    commandLine("gcc", "-std=c11", "-Wall", "-Wextra", "-Werror", "-O2", "-c",
        cDirectory.file("demo.c").asFile.absolutePath, "-o", cObject.get().asFile.absolutePath)
}

val archiveC by tasks.registering(Exec::class) {
    dependsOn(compileC)
    inputs.file(cObject)
    outputs.file(cLibrary)
    commandLine("ar", "rcs", cLibrary.get().asFile.absolutePath, cObject.get().asFile.absolutePath)
}

kotlin {
    jvmToolchain(17)
    jvm()
    android {
        namespace = "com.example.nativelib"
        compileSdk = 36
        minSdk = 24
    }
    mingwX64 {
        compilations.getByName("main") {
            cinterops.create("demo") {
                definitionFile.set(project.file("src/nativeInterop/cinterop/demo.def"))
                includeDirs(cDirectory.asFile)
                extraOpts("-libraryPath", cBuildDirectory.get().asFile.absolutePath)
            }
        }
    }
    sourceSets {
        androidMain.dependencies {
            implementation(project(":android-cbridge"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

tasks.named("cinteropDemoMingwX64") {
    dependsOn(archiveC)
    inputs.files(cDirectory.file("demo.h"), cLibrary)
}

publishing {
    repositories {
        maven {
            name = "Demo"
            url = rootProject.layout.projectDirectory.dir("build/demo-repo").asFile.toURI()
        }
    }
}
