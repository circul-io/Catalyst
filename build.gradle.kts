import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    kotlin("multiplatform") version "1.9.10"
    id("org.jetbrains.dokka") version "1.9.0"
    id("maven-publish")
    id("signing")
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
            dependencies {
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

tasks.register<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        named<MavenPublication>("jvm") {
            artifact(tasks["javadocJar"])
        }

        withType<MavenPublication>().all {
            pom {
                name.set("Catalyst")
                description.set("Kotlin coroutines-based retry library")
                url.set("https://catalyst.circul.io")

                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("JeremyMWillemse")
                        name.set("Jeremy Willemse")
                        email.set("jeremy@willemse.co")
                    }
                }
                scm {
                    url.set("https://github.com/circul-io/Catalyst")
                    connection.set("scm:git:https://github.com/circul-io/Catalyst.git")
                    developerConnection.set("scm:git:ssh://github.com/circul-io/Catalyst.git")
                }
            }
        }
    }

    repositories {
        maven {
            name = "OSSRH"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")

            credentials {
                username = project.findProperty("ossrhUsername") as String?
                password = project.findProperty("ossrhPassword") as String?
            }
        }
    }
}

signing {
    sign(publishing.publications)
}
