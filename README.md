# Catalyst (1.0.0-alpha1)

[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-1.9.10-blue)](https://kotlinlang.org/docs/multiplatform.html)
[![License: Apache-2.0](https://img.shields.io/badge/License-Apache%202.0-brightgreen.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/io.circul/Catalyst.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.circul/Catalyst/1.0.0-alpha1/overview)

Catalyst is a Kotlin Multiplatform coroutine-based retry library that offers an expressive DSL for building complex
retry policies.
---

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

### Step 3: Sync your project
If you're using IntelliJ IDEA or Android Studio, you may need to synchronize your project after adding the dependency.

---

## Usage
Catalyst allows you to create expressive and composable retry policies. A retry policy is a combination
of a retry predicate (logic expressing the conditions under which a retry should occur) and a delay strategy (the
formula for determining successive delay times between retry attempts).

### Compose a RetryPredicate

`RetryPredicate` is an interface provided by Catalyst that is used by the `RetryPolicy.retry(...)` method to determine
whether a retry should occur through its `shouldRetry(...)` method.

You can combine as many `RetryPredicate` objects as necessary using boolean operators `and`, `or`, `xor`, `nand`, `nor`,
`xnor`, and `!`(not).

You may choose to simply implement the `RetryPredicate` interface yourself, E.g.,

```kotlin
import io.circul.catalyst.predicate.RetryPredicate

val customPredicate = object : RetryPredicate {
    override fun shouldRetry(result: Result<Any?>, retryCount: Int): Boolean =
        result.isFailure && retryCount < 9
}
```

**However**, for convenience, Catalyst provides several predefined predicate implementations as discussed below.

#### Limiting Retries
To impose an upper limit on the number of attempts a retry block will make, you can use the `attempts(Int)` function,
or the `Int.attempts` extension property. E.g.,

```kotlin
import io.circul.catalyst.predicate.attempts

// Both attemptsPredicate1 and attemptsPredicate2 return a RetryPredicate that limits attempts to 10.
val attemptsPredicate1 = attempts(10)
val attemptsPredicate2 = 10.attempts
```
On its own, the `attempts` `RetryPredicate` is not particularly useful as it will return `true` for `shouldRetry()` on 
every attempt up to the specified maximum, irrespective of the result at each iteration. It should be combined using the
`and` operator with another predicate that considers the result (e.g., `onException`, `onNull`, or `untilResult`)

Catalyst also provides a way to limit retries based on the total elapsed time since the beginning of the first attempt,
either via the `timeLimit(Duration)` function, or the `Duration.timeLimit` extension property. E.g.,

```kotlin
import io.circul.catalyst.predicate.timeLimit
import kotlin.time.Duration.Companion.seconds

// Both timeLimitPredicate1 and timeLimitPredicate2 return a RetryPredicate that
// prevent retries from occurring beyond 30 seconds since the first try.
val timeLimitPredicate1 = timeLimit(30.seconds)
val timeLimitPredicate2 = 30.seconds.timeLimit
```

**WARNING:** `timeLimit` predicates are stateful, so if you wish to define a reusable `RetryPolicy` you should do so via
a factory function to ensure that the start time is correctly reset for each use, and for thread safety.
```kotlin
import io.circul.catalyst.RetryPolicy
import io.circul.catalyst.asRetryPolicy
import io.circul.catalyst.predicate.onException
import io.circul.catalyst.predicate.timeLimit
import kotlin.time.Duration.Companion.seconds

val retryPolicyFactory: () -> RetryPolicy = {
    (30.seconds.timeLimit and onException).asRetryPolicy
}
```


#### Considering the Result
Depending on how the block of code you are using the `RetryPolicy` on returns for successes and failures, you may use
any of a number of `RetryPredicate` objects that check for various result conditions.
- `onException` will return `true` for `shouldRetry` if the block throws an exception
- `onNull` will return `true` for `shouldRetry` if the block does not throw an exception but returns `null`
- `untilResult` is a combination of `onException` **OR** `onNull`, meaning it will return `true` for `shouldRetry` if
either an exception is thrown or the result is `null`.

The composed equivalent of the `customPredicate` example given earlier is: 
```kotlin
import io.circul.catalyst.predicate.attempts
import io.circul.catalyst.predicate.onException

val composedPredicate = onException and 10.attempts
```

If you want to retry based on execution results other than `null` or exceptions, you can use one of two inline functions
with a reified type parameter:
- `onResultType<T>()` will retry as long as the result is of type `T`
- `untilResultType<T>()` will retry until the result is of type `T`

E.g.,
```kotlin
import io.circul.catalyst.predicate.onResultType

// Example error type
data class ErrorDTO(val errorCode: String, val errorMessage: String)

// Example success type
data class SuccessDTO(val response: String)

// If the result of the retry() lambda's execution is of type ErrorDTO, then a retry should be attempted
val onErrorPredicate = onResultType<ErrorDTO>()

// If the result of the retry() lambda's execution is of type SuccessDTO, then a retry should NOT be attempted
val untilSuccessPredicate = untilResultType<SuccessDTO>()
```

For even more complex retry predicates, Catalyst provides two inline functions that take a single `PredicateFunction`
lambda argument that receives the result of the most recent execution and the number of retries that have occurred so
far:
- `on` will retry as long as the result of the lambda argument is `true`
- `until` will retry as long as the result of the lambda argument is `false`

E.g.,
```kotlin
import io.circul.catalyst.predicate.on

// Example error type
data class ErrorDTO(val errorCode: String, val errorMessage: String)

// If the result is of type ErrorDTO AND the error code is 00007, then retry
val onPredicate = on { result, _ ->
    val dto = result.getOrNull()
    dto is ErrorDTO && dto.errorCode == "00007"
}
```

#### Combining Predicates
You can combine as many predicates as you like using the infix boolean operators `and`, `or`, `xor`, `nand`, `nor`,
amd `xnor`. You may also negate a predicate with the `!` (not) operator. This gives you a great deal of flexibility
when it comes to composing your retry policies, but more than likely, you will get by with `and` and `or` most of the
time. Remember to use parentheses to correctly group your predicates. E.g.,

```kotlin
import io.circul.catalyst.predicate.attempts
import io.circul.catalyst.predicate.onResultType

// Example response types
sealed interface ResponseType {
    data class ErrorA(val message: String) : ResponseType
    data class ErrorB(val message: String) : ResponseType
    data class SuccessA(val message: String) : ResponseType
    data class SuccessB(val message: String) : ResponseType
}

// Try up to 10 times to get a non Error response
val retryPredicate = (onResultType<ResponseType.ErrorA>() or onResultType<ResponseType.ErrorB>()) and 10.attempts 
```

--- 

## Documentation
https://catalyst.circul.io

## Licence
https://www.apache.org/licenses/LICENSE-2.0
