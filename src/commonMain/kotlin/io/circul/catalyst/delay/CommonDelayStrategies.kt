package io.circul.catalyst.delay

import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.times

typealias DelayFunction = (index: Int) -> Duration


/**
 * Creates a [DelayStrategy] that always returns the same [delay] duration.
 *
 * @param delay The constant delay duration.
 * @return A [DelayStrategy] that always yields the same delay.
 * @since 1.0.0
 */
fun constantDelay(delay: Duration) = object : DelayStrategy {
    override fun get(index: Int): Duration = delay
}

/**
 * Creates a [DelayStrategy] that always returns the same delay duration.
 *
 * @param delayMillis The constant delay duration in milliseconds.
 * @return A [DelayStrategy] that always yields the same delay.
 * @since 1.0.0
 */
fun constantDelay(delayMillis: Long) = constantDelay(delayMillis.milliseconds)

/**
 * A property extension that converts the [Duration] to a [DelayStrategy] with a constant delay.
 * @see constantDelay
 * @since 1.0.0
 */
inline val Duration.constantDelay get(): DelayStrategy = constantDelay(this)

/**
 * A [DelayStrategy] that always yields zero delay.
 * @see constantDelay
 * @since 1.0.0
 */
val noDelay = constantDelay(Duration.ZERO)

/**
 * Creates a [DelayStrategy] that returns a list of delays in sequence.
 * Once the list is exhausted, it will keep returning the last delay.
 *
 * @param delays List of sequential delays.
 * @return A [DelayStrategy] that yields delays in sequence.
 * @since 1.0.0
 */
fun sequentialDelay(delays: List<Duration>) = object : DelayStrategy {
    init {
        require(delays.isNotEmpty()) { "delays list must not be empty" }
    }

    override fun get(index: Int): Duration = (delays.getOrNull(index) ?: delays.last()).coerceAtLeast(Duration.ZERO)
}

/**
 * Creates a [DelayStrategy] that returns a list of delays in sequence, defined in milliseconds.
 * Once the list is exhausted, it will keep returning the last delay.
 *
 * @param delayMillis Vararg of sequential delays in milliseconds.
 * @return A [DelayStrategy] that yields delays in sequence.
 * @since 1.0.0
 */
fun sequentialDelay(vararg delayMillis: Long) = sequentialDelay(delayMillis.map { it.milliseconds })

/**
 * A property extension that converts a list of [Duration]s to a sequential [DelayStrategy].
 * @see sequentialDelay
 * @since 1.0.0
 */
inline val List<Duration>.sequentialDelay
    get(): DelayStrategy = sequentialDelay(this)

/**
 * Creates a [DelayStrategy] with a linearly increasing delay duration based on the given initial delay and increment.
 *
 * @param initialDelay The delay before the first retry.
 * @param increment The amount added to the delay for each subsequent retry.
 * @return A [DelayStrategy] with linearly increasing delays.
 * @since 1.0.0
 */
fun linearBackoffDelay(initialDelay: Duration, increment: Duration) = object : DelayStrategy {
    override fun get(index: Int): Duration = (initialDelay + increment * index).coerceAtLeast(Duration.ZERO)
}


/**
 * An infix function to create a linear backoff [DelayStrategy].
 * @see linearBackoffDelay
 * @since 1.0.0
 */
infix fun Duration.delayWithIncrementsOf(that: Duration): DelayStrategy = linearBackoffDelay(this, that)

/**
 * Creates a [DelayStrategy] with delays that increase by the Fibonacci sequence multiplied by the [baseDelay].
 *
 * Note: for the default start pair of (0, 1) the first delay (index 0) will always be [Duration.ZERO] and the second
 * delay (index 1) will be [baseDelay]
 *
 * @param baseDelay The delay unit used for each Fibonacci number.
 * @param start The initial fibonacci sequence pair, defaults to (0, 1)
 * @return A [DelayStrategy] with Fibonacci-based delays.
 * @since 1.0.0
 */
fun fibonacciBackoffDelay(baseDelay: Duration, start: Pair<Int, Int> = 0 to 1) = object : DelayStrategy {
    override fun get(index: Int): Duration = (fibonacci(index) * baseDelay).coerceAtLeast(Duration.ZERO)

    private fun fibonacci(n: Int): Int = when (n) {
        0 -> start.first
        1 -> start.second
        else -> (2..n).fold(start) { (a, b), _ ->
            b to a + b
        }.second
    }
}

/**
 * An infix function to create a fibonacci backoff [DelayStrategy].
 * @see fibonacciBackoffDelay
 * @since 1.0.0
 */
infix fun Duration.delayWithFibonacciSequenceOf(that: Pair<Int, Int>): DelayStrategy = fibonacciBackoffDelay(this, that)

/**
 * A property extension that converts the [Duration] to a Fibonacci-based [DelayStrategy].
 * @see fibonacciBackoffDelay
 * @since 1.0.0
 */
inline val Duration.fibonacciBackoffDelay
    get(): DelayStrategy = fibonacciBackoffDelay(this)


/**
 * Creates a [DelayStrategy] with exponentially increasing delay durations.
 *
 * @param initialDelay The delay before the first retry.
 * @param factor The multiplier for each subsequent retry.
 * @return A [DelayStrategy] with exponentially increasing delays.
 * @since 1.0.0
 */
fun exponentialBackoffDelay(initialDelay: Duration, factor: Double) = object : DelayStrategy {
    override fun get(index: Int): Duration = (initialDelay * factor.pow(index)).coerceAtLeast(Duration.ZERO)
}

/**
 * An infix function to create an exponential backoff [DelayStrategy].
 * @see exponentialBackoffDelay
 * @since 1.0.0
 */
infix fun Duration.delayWithScaleFactorOf(that: Double): DelayStrategy = exponentialBackoffDelay(this, that)

/**
 * Creates a [DelayStrategy] using a custom delay function.
 *
 * @param delayFunction A function to determine the delay based on the retry index.
 * @return A [DelayStrategy] defined by the custom function.
 * @since 1.0.0
 */
fun customDelay(delayFunction: DelayFunction) = object : DelayStrategy {
    override fun get(index: Int): Duration = delayFunction(index).coerceAtLeast(Duration.ZERO)
}

/**
 * A property extension that converts a [DelayFunction] to a [DelayStrategy].
 * @see customDelay
 * @since 1.0.0
 */
inline val DelayFunction.customDelay
    get(): DelayStrategy = customDelay(this)
