package io.circul.catalyst.policy.delay

import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.times

typealias ExponentialBackoff = Pair<Duration, Double>
typealias LinearBackoff = Pair<Duration, Duration>
typealias DelayFunction = (index: Int) -> Duration

fun constantDelay(delay: Duration) = object : DelayStrategy {
    override fun get(index: Int): Duration = delay
}

fun constantDelay(delayMillis: Long) = constantDelay(delayMillis.milliseconds)

inline val Duration.delay get(): DelayStrategy = constantDelay(this)

val noDelay = constantDelay(Duration.ZERO)

fun sequentialDelay(delays: List<Duration>) = object : DelayStrategy {
    init {
        require(delays.isNotEmpty()) { "delays list must not be empty" }
    }

    override fun get(index: Int): Duration = (delays.getOrNull(index) ?: delays.last()).coerceAtLeast(Duration.ZERO)
}

fun sequentialDelay(vararg delayMillis: Long) = sequentialDelay(delayMillis.map { it.milliseconds })

inline val List<Duration>.sequentialDelay
    get(): DelayStrategy = sequentialDelay(this)

fun linearBackoffDelay(initialDelay: Duration, increment: Duration) = object : DelayStrategy {
    override fun get(index: Int): Duration = (initialDelay + increment * index).coerceAtLeast(Duration.ZERO)
}

fun fibonacciBackoffDelay(baseDelay: Duration) = object : DelayStrategy {
    override fun get(index: Int): Duration {
        return fibonacci(index) * baseDelay
    }

    private fun fibonacci(n: Int): Int =
        (2..n).fold(Pair(0, 1)) { (a, b), _ ->
            b to a + b
        }.second
}

inline val Duration.fibonacciBackoffDelay
    get(): DelayStrategy = fibonacciBackoffDelay(this)


inline val LinearBackoff.linearBackoffDelay
    get(): DelayStrategy = linearBackoffDelay(this.first, this.second)

inline infix fun Duration.incrementedEachRetryBy(that: Duration): DelayStrategy = linearBackoffDelay(this, that)

fun exponentialBackoffDelay(initialDelay: Duration, factor: Double) = object : DelayStrategy {
    override fun get(index: Int): Duration = (initialDelay * factor.pow(index)).coerceAtLeast(Duration.ZERO)
}

inline val ExponentialBackoff.exponentialBackoffDelay
    get(): DelayStrategy = exponentialBackoffDelay(this.first, this.second)

inline infix fun Duration.scaledEachRetryBy(that: Double): DelayStrategy = exponentialBackoffDelay(this, that)

fun customDelay(delayFunction: DelayFunction) = object : DelayStrategy {
    override fun get(index: Int): Duration = delayFunction(index).coerceAtLeast(Duration.ZERO)
}

inline val DelayFunction.customDelay
    get(): DelayStrategy = customDelay(this)
