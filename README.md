# Catalyst (1.0.0-alpha1)

[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-1.9.10-blue)](https://kotlinlang.org/docs/multiplatform.html)
[![License: Apache-2.0](https://img.shields.io/badge/License-Apache%202.0-brightgreen.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/io.circul/Catalyst.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.circul/Catalyst/1.0.0-alpha1/overview)

Catalyst is a Kotlin coroutine-based retry library that offers an expressive DSL for building complex retry policies.

## Installation
Catalyst is on [Maven Central](https://central.sonatype.com/artifact/io.circul/Catalyst/1.0.0-alpha1/overview).
To use Catalyst in your Kotlin project, follow these steps:

### Step 1: Add the Maven Central Repository (if not already added)
Ensure you have the **mavenCentral()** repository in your project's build file:

```groovy
repositories {
    mavenCentral()
}
```

### Step 2: Add the Dependency
Add the following dependency to your project's build file:

#### Kotlin (build.gradle.kts)
```kotlin
dependencies {
    implementation("io.circul:Catalyst:1.0.0-alpha1")
}
```

#### Groovy (build.gradle)
```groovy
dependencies {
    implementation 'io.circul:Catalyst:1.0.0-alpha1'
}
```

Step 3: Sync your project
If you're using IntelliJ IDEA or Android Studio, you may need to synchronize your project after adding the dependency.

## Documentation
https://catalyst.circul.io
