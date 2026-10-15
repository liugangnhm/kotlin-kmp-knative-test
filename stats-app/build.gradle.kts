plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
    jvm()
    mingwX64 {
        binaries.executable {
            baseName = "stats-report"
            entryPoint = "stats.main"
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(project(":native-lib"))
        }
    }
}

tasks.register<JavaExec>("runJvm") {
    group = "application"
    val compilation = kotlin.jvm().compilations.getByName("main")
    classpath = compilation.output.allOutputs + compilation.runtimeDependencyFiles!!
    mainClass.set("stats.MainKt")
}
