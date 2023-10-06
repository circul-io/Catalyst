# Catalyst (1.0.0)

[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-1.9.10-blue)](https://kotlinlang.org/docs/multiplatform.html)
[![License: Apache-2.0](https://img.shields.io/badge/License-Apache%202.0-brightgreen.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/io.circul/Catalyst.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.circul/Catalyst/1.0.0/overview)

Catalyst is a Kotlin Multiplatform coroutine-based retry library that offers an expressive DSL for building complex
retry policies.
---

## Installation
Catalyst is on [Maven Central](https://central.sonatype.com/artifact/io.circul/Catalyst/1.0.0/overview).
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
    implementation("io.circul:Catalyst:1.0.0")
}
```

#### Groovy (build.gradle)
```groovy
dependencies {
    implementation 'io.circul:Catalyst:1.0.0'
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
    override fun shouldRetry(result: Result<Any?>, retryCount: Int, elapsedTime: Duration): Boolean =
        result.isFailure && retryCount < 9 && elapsedTime < 10.seconds
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
val onPredicate = on { result, _, _ ->
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

#### Stateful Predicates
All of the predefined predicates are stateless as they depend on method arguments. I.e., the `Result` object is a result
of the execution of the lambda function passed to `retry()`, and the `retryCount` and `elapsedTime` are tracked by the
retry function itself.

However, if you wish to define a predicate that encapsulates its own state and you want to reuse your `RetryPolicy` 
throughout your code, you should use a factory function to ensure each `retry()` block has its own stateful instance.

For example, you may wish to define a retry predicate that takes into account specific types of results or exceptions
and tracks their counts independently, using those counts as a condition for `shouldRetry`:

```kotlin
val retryPolicyFactory: () -> RetryPolicy = {
    object : RetryPredicate {
        // Track counts for each type of Throwable
        val mapOfExceptionCounts = mutableMapOf<KClass<out Throwable>, Int>()

        // Retry if the Result is a failure and the particular failure type has not occurred more than three times
        override fun shouldRetry(result: Result<Any?>, retryCount: Int, elapsedTime: Duration): Boolean {
            if (result.isFailure) {
                val exceptionClass = result.exceptionOrNull()!!::class
                val count = mapOfExceptionCounts.getOrPut(exceptionClass) { 0 } + 1
                mapOfExceptionCounts[exceptionClass] = count
                return count < 3
            }
            return false
        }
    }.asRetryPolicy
}
```

### Define a DelayStrategy
Delay strategies are defined by objects that implement the `DelayStrategy` interface, overriding the `[]` (get) operator
which returns a `Duration`.

```kotlin
// A custom delay strategy that starts from zero seconds retry delay and increments by one second each iteration 
val oneSecondIncrements = object : DelayStrategy {
    override fun get(index: Int): Duration = index.seconds
}
```

#### Constant Delay
Constant delays always delay subsequent executions of the retry block by a fixed amount. Catalyst provides several ways
to define a constant delay. The example below shows three ways to define a one second constant delay.
```kotlin
import io.circul.catalyst.delay.constantDelay
import kotlin.time.Duration.Companion.seconds

// constantDelay(Duration)
val delay1 = constantDelay(1.seconds)

// constantDelay(Long)
val delay2 = constantDelay(1000L)

// Duration.constantDelay
val delay3 = 1.seconds.constantDelay
```

Catalyst also provides `noDelay`, which is effectively a constant delay of `Duration.ZERO`.

#### Sequential Delay
A sequential delay simply takes a list of delays to use in sequence until the `RetryPredicate` returns false for
`shouldRetry()`.

The example below shows three identical ways of creating a sequential delay of 500 milliseconds, 500 milliseconds, 
1 second, 5 seconds:

**Note:** if the number of retries allowed (as determined by the `RetryPredicate`) exceeds the length of the list of
delays, then the last item in the list will effectively become a constant delay for the remainder of the retry block 
execution.

```kotlin
import io.circul.catalyst.delay.sequentialDelay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

// sequentialDelay(List<Duration>)
val delay1 = sequentialDelay(listOf(500.milliseconds, 500.milliseconds, 1.seconds, 5.seconds))

// sequentialDelay(vararg Long)
val delay2 = sequentialDelay(500L, 500L, 1000L, 5000L)

// List<Duration>.sequentialDelay
val delay3 = listOf(500.milliseconds, 500.milliseconds, 1.seconds, 5.seconds).sequentialDelay
```

#### Linear Backoff Delay
A linear backoff delay is a `DelayStrategy` that increments by a fixed amount with each successive attempt.

Catalyst provides two ways to create a linear backoff delay. The example below demonstrates both ways to create a
linear backoff delay strategy with an initial delay of 200 milliseconds, with increments of 100 milliseconds.
```kotlin
import io.circul.catalyst.delay.sequentialDelay
import io.circul.catalyst.delay.delayWithIncrementsOf
import kotlin.time.Duration.Companion.milliseconds

// linearBackoffDelay(Duration, Duration)
val delay1 = linearBackoffDelay(200.milliseconds, 100.milliseconds)

// Duration delayWithIncrementsOf Duration
val delay2 = 200.milliseconds delayWithIncrementsOf 100.milliseconds
```

#### Fibonacci Backoff Delay
Catalyst provides a `DelayStrategy` that uses the fibonacci sequence to multiply a base delay. By default, the
sequence will start with [0, 1], which means that the first multiplier is zero. The example below demonstrates the
various options to create a fibonacci backoff delay strategy.

```kotlin
import io.circul.catalyst.delay.fibonacciBackoffDelay
import io.circul.catalyst.delay.delayWithFibonacciSequenceOf
import kotlin.time.Duration.Companion.milliseconds

/* Default start sequence examples (0, 1) */
// fibonacciBackoffDelay(Duration)
val delay1 = fibonacciBackoffDelay(100.milliseconds)
// Duration.fibonacciBackoffDelay
val delay2 = 100.milliseconds.fibonacciBackoffDelay

/* Custom start sequence (1, 2)*/
// fibonacciBackoffDelay(Duration, Pair<Int, Int>)
val delay3 = fibonacciBackoffDelay(100.milliseconds, (1 to 2))
// Duration delayWithFibonacciSequenceOf Pair<Int, Int>
val delay4 = 100.milliseconds delayWithFibonacciSequenceOf (1 to 2)
```

#### Exponential Backoff Delay
An exponential backoff delay is a `DelayStrategy` with exponentially increasing delay durations.
The example below demonstrates two ways of creating a delay strategy that doubles for each subsequent attempt.

```kotlin
import io.circul.catalyst.delay.exponentialBackoffDelay
import io.circul.catalyst.delay.delayWithScaleFactorOf
import kotlin.time.Duration.Companion.milliseconds

// exponentialBackoffDelay(Duration, Double)
val delay1 = exponentialBackoffDelay(100.milliseconds, 2.0)

// Duration delayWithScaleFactorOf Double
val delay2 = 100.milliseconds delayWithScaleFactorOf 2.0
```

#### Custom Delay
If the predefined delay strategies described above do not suit your needs, you can define your own custom
`DelayStrategy`. You could simply implement the interface and override the `[]` (get) operator, but Catalyst provides
a higher-order function to simplify this. The example below shows two ways of creating a (rather contrived) custom delay
strategy that will delay by the number of retries already attempted, in seconds, plus 500 milliseconds.

```kotlin
import io.circul.catalyst.delay.customDelay
import kotlin.time.Duration.Companion.milliseconds

// customDelay((Int) -> Duration)
val delay1 = customDelay { it.seconds + 500.milliseconds }

// ((Int) -> Duration).customDelay
val delay2 = { i: Int -> i.seconds + 100.milliseconds }.customDelay
```

#### Composite Delay Strategies
As with predicates, different delay strategies can be combined to form a composite `DelayStrategy`. The following
arithmetic operator methods are supported:
- `DelayStrategy + DelayStrategy`
- `DelayStrategy - DelayStrategy`
- `DelayStrategy * Int`
- `DelayStrategy * Double`
- `DelayStrategy / Int`
- `DelayStrategy / Double`
- `-DelayStrategy` (unary minus)

Additionally, the following extension functions are provided by the library:
- `Int * DelayStrategy`
- `Double * DelayStrategy`

#### Constraining Delay Strategies
In addition to having the ability to arithmetically combine delay strategies, Catalyst also lets you constrain delay
times to minimum and maximum values.

There are a number of ways to do this:
- `minOf(DelayStrategy, vararg DelayStrategy)` will create a composite `DelayStrategy` that will return the minimum delay
duration of each of the provided `DelayStrategy` arguments for the `[]` (get) operator
- `maxOf(DelayStrategy, vararg DelayStrategy)` will create a composite `DelayStrategy` that will return the maximum delay
  duration of each of the provided `DelayStrategy` arguments for the `[]` (get) operator
- `DelayStrategy.coerceAtLeast(Duration)` will return a new `DelayStrategy` that constrains the receiving
  `DelayStrategy` to a minimum `Duration`
- `DelayStrategy.coerceAtMost(Duration)` will return a new `DelayStrategy` that constrains the receiving
  `DelayStrategy` to a maximum `Duration`
- `DelayStrategy.coerceIn(Duration, Duration)` will return a new `DelayStrategy` that constrains the receiving
  `DelayStrategy` to a minimum and maximum `Duration`
- `DelayStrategy.coerceIn(ClosedRange<Duration>)` will return a new `DelayStrategy` that constrains the receiving
  `DelayStrategy` within a range of `Duration`s.

```kotlin
import io.circul.catalyst.delay.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

val minDelay1Second = minOf(1.seconds.constantDelay, exponentialBackoffDelay(100.milliseconds, 2.0))
val maxDelay10Seconds = maxOf(10.seconds.constantDelay, exponentialBackoffDelay(100.milliseconds, 2.0))
val coercedMin1Second = exponentialBackoffDelay(100.milliseconds, 2.0).coerceAtLeast(1.seconds)
val coercedMax10Seconds = exponentialBackoffDelay(100.milliseconds, 2.0).coerceAtMost(10.seconds)
val coercedInMinMax = exponentialBackoffDelay(100.milliseconds, 2.0).coerceIn(1.seconds, 10.seconds)
val coercedInRange = exponentialBackoffDelay(100.milliseconds, 2.0).coerceIn(1.seconds..10.seconds)
````

#### Jitter Factors
Catalyst also provides a way to apply random offsets to the calculated delay values. This can be useful to prevent
synchronized spikes in load, avoid shared resource contention, and improve the stability and throughput when many
clients are retrying.

To apply jitter to a `DelayStrategy`, you can use the `withJitter` method that takes a `jitterFactor` argument.
The jitter factor is a multiplier applied to the calculated delay, determining a range for potential variation. This
factor is multiplied by a random value between -1 and 1 and then combined with the base delay to introduce controlled
randomness to the overall delay.

```kotlin
import io.circul.catalyst.delay.delayWithScaleFactorOf
import kotlin.time.Duration.Companion.milliseconds

val delay = (100.milliseconds delayWithScaleFactorOf 2.0).withJitter(0.2)
```

### Create a RetryPolicy
A `RetryPolicy` is simply a class that contains a `RetryPredicate` and a `DelayStrategy` and has a public `retry()`
method.

To create a `RetryPolicy`, you may either use its constructor or a number of other options.

The `RetryPolicy` constructor has named arguments for `retryPredicate` and `delayStrategy`. The default values for these
arguments are `onException` and `noDelay`, respectively.

If you only want to differ from these defaults for one part, you can either use the constructor providing only the named
argument of the part you wish to provide, or you can simply use the `asRetryPolicy` extension property on either the
`RetryPredicate` or `DelayStrategy` object.

There is also a `with` infix function that returns a `RetryPolicy` when it is used between a `RetryPredicate` and a
`DelayStrategy` (or vice versa).

```kotlin
// onException, noDelay
val retryPolicy1 = RetryPolicy()

// untilResult, noDelay
val retryPolicy2 = RetryPolicy(retryPredicate = untilResult)

// onException, constantDelay(100.milliseconds)
val retryPolicy3 = RetryPolicy(delayStrategy = 100.milliseconds.constantDelay)

// untilResult, constantDelay(100.milliseconds)
val retryPolicy4 = RetryPolicy(retryPredicate = untilResult, delayStrategy = 100.milliseconds.constantDelay)

// untilResult, noDelay
val retryPolicy5 = untilResult.asRetryPolicy

// onException, constantDelay(100.milliseconds)
val retryPolicy6 = 100.milliseconds.constantDelay.asRetryPolicy

// untilResult, constantDelay(100.milliseconds)
val retryPolicy7 = untilResult with 100.milliseconds.constantDelay

// untilResult, constantDelay(100.milliseconds)
val retryPolicy8 = 100.milliseconds.constantDelay with untilResult
```

### Use a RetryPolicy
Once you have created your `RetryPolicy`, you simply need to call its `retry` method to define a retry-able block of
code:

```kotlin
import kotlin.random.Random
import io.circul.catalyst.delay.constantDelay
import io.circul.catalyst.predicate.attempts
import io.circul.catalyst.predicate.onException
import kotlin.time.Duration.Companion.milliseconds

fun fiftyFiftyChance(): String {
  if (Random.nextBoolean()) {
    return "Success!"
  } else {
    throw RuntimeException("Failed!")
  }
}

suspend fun retrySomething() {
  val result = runCatching {
    ((onException and 3.attempts) with 100.milliseconds.constantDelay).retry {
      fiftyFiftyChance()
    }
  }.onFailure {
    println(it) // Handle final exceptions
  }.getOrNull() // returns a `String?`
}
```

--- 

## Documentation
https://catalyst.circul.io

## Licence
https://www.apache.org/licenses/LICENSE-2.0
