import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    kotlin("multiplatform") version "1.9.10"
    id("org.jetbrains.dokka") version "1.9.0"
}

group = "io.circul"
version = "1.0.0-alpha1"

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        jvmToolchain(8)
        withJava()
        testRuns.named("test") {
            executionTask.configure {
                useTestNG()
            }
        }
    }
    js {
        browser {
            commonWebpackConfig(Action<KotlinWebpackConfig> {
                cssSupport {
                    enabled.set(true)
                }
            })
        }
    }
    macosX64()
    macosArm64()
    linuxX64()
    linuxArm64()
    mingwX64()
    iosX64()
    iosArm64()
    
    sourceSets {
        getByName("commonMain") {
            dependencies{
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            }
        }
        getByName("commonTest") {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }
    }
}
