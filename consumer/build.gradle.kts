plugins {
    kotlin("multiplatform") version "2.4.20"
}

repositories {
    maven {
        url = layout.projectDirectory.dir("../build/demo-repo").asFile.toURI()
        content { includeGroup("com.example") }
    }
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
    jvm()
    mingwX64 {
        binaries.executable {
            baseName = "consumer-demo"
            entryPoint = "consumer.main"
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation("com.example:native-lib:1.0.0")
        }
    }
}

tasks.register<JavaExec>("runJvm") {
    group = "application"
    val compilation = kotlin.jvm().compilations.getByName("main")
    classpath = compilation.output.allOutputs + compilation.runtimeDependencyFiles!!
    mainClass.set("consumer.MainKt")
}
