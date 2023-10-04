package io.circul.catalyst.delay

import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.times

/**
 * Represents a strategy for computing delay times based on an index.
 *
 * Complex delay strategies can be composed using arithmetic operators and min/max coercion methods
 * @since 1.0.0
 */
interface DelayStrategy {

    /**
     * Gets the delay for the given retry [index].
     *
     * The delay before the first retry after the initial attempt is at [index] 0.
     *
     * @param index The number of retries already attempted
     * @return The [Duration] to wait before the next retry.
     * @since 1.0.0
     */
    operator fun get(index: Int): Duration


    /**
     * Combines this strategy with another by summing their delay durations when [get] is called for a given index.
     *
     * @param other Another [DelayStrategy] to be added.
     * @return A new [DelayStrategy] representing the sum of both strategies.
     * @since 1.0.0
     */
    operator fun plus(other: DelayStrategy) = object : DelayStrategy {
        override operator fun get(index: Int): Duration = this@DelayStrategy[index] + other[index]
    }

    /**
     * Combines this strategy with another by subtracting their delay durations when [get] is called for a given index.
     *
     * @param other Another [DelayStrategy] to be subtracted.
     * @return A new [DelayStrategy] representing the difference of both strategies.
     * @since 1.0.0
     */
    operator fun minus(other: DelayStrategy) = object : DelayStrategy {
        override operator fun get(index: Int): Duration = this@DelayStrategy[index] - other[index]
    }

    /**
     * Multiplies the delay duration of this strategy by the given integer factor when [get] is called for a given index.
     *
     * @param factor The multiplier factor.
     * @return A new [DelayStrategy] representing the multiplied delay.
     * @since 1.0.0
     */
    operator fun times(factor: Int) = object : DelayStrategy {
        override operator fun get(index: Int): Duration = this@DelayStrategy[index] * factor
    }

    /**
     * Multiplies the delay duration of this strategy by the given double factor when [get] is called for a given index.
     *
     * @param factor The multiplier factor.
     * @return A new [DelayStrategy] representing the multiplied delay.
     * @since 1.0.0
     */
    operator fun times(factor: Double) = object : DelayStrategy {
        override operator fun get(index: Int): Duration = this@DelayStrategy[index] * factor
    }

    /**
     * Divides the delay duration of this strategy by the given integer divisor when [get] is called for a given index.
     *
     * @param divisor The divisor.
     * @return A new [DelayStrategy] representing the divided delay.
     * @since 1.0.0
     */
    operator fun div(divisor: Int) = object : DelayStrategy {
        override operator fun get(index: Int): Duration = this@DelayStrategy[index] / divisor
    }

    /**
     * Divides the delay duration of this strategy by the given double divisor when [get] is called for a given index.
     *
     * @param divisor The divisor.
     * @return A new [DelayStrategy] representing the divided delay.
     * @since 1.0.0
     */
    operator fun div(divisor: Double) = object : DelayStrategy {
        override operator fun get(index: Int): Duration = this@DelayStrategy[index] / divisor
    }

    /**
     * Inverts the delay duration of this strategy when [get] is called for a given index.
     *
     * @return A new [DelayStrategy] representing the inverted delay.
     * @since 1.0.0
     */
    operator fun unaryMinus() = object : DelayStrategy {
        override fun get(index: Int): Duration = -this@DelayStrategy[index]
    }

    /**
     * Returns this delay strategy unchanged. This function exists mainly for symmetry purposes.
     *
     * @return The same [DelayStrategy] instance.
     * @since 1.0.0
     */
    operator fun unaryPlus() = this

    /**
     * Ensures that the delay durations of this strategy are at least of the specified [minimumValue].
     *
     * @param minimumValue The minimum duration value to be coerced to.
     * @return A new [DelayStrategy] representing the coerced delay durations.
     * @since 1.0.0
     */
    fun coerceAtLeast(minimumValue: Duration) = object : DelayStrategy {
        override fun get(index: Int): Duration = this@DelayStrategy[index].coerceAtLeast(minimumValue)
    }

    /**
     * Ensures that the delay durations of this strategy are at most of the specified [maximumValue].
     *
     * @param maximumValue The maximum duration value to be coerced to.
     * @return A new [DelayStrategy] representing the coerced delay durations.
     * @since 1.0.0
     */
    fun coerceAtMost(maximumValue: Duration) = object : DelayStrategy {
        override fun get(index: Int): Duration = this@DelayStrategy[index].coerceAtMost(maximumValue)
    }

    /**
     * Ensures that the delay durations of this strategy are within the specified range.
     *
     * @param minimumValue The minimum duration value of the range.
     * @param maximumValue The maximum duration value of the range.
     * @return A new [DelayStrategy] representing the coerced delay durations.
     * @since 1.0.0
     */
    fun coerceIn(minimumValue: Duration, maximumValue: Duration) = object : DelayStrategy {
        override fun get(index: Int): Duration = this@DelayStrategy[index].coerceIn(minimumValue, maximumValue)
    }

    /**
     * Ensures that the delay durations of this strategy are within the specified duration range.
     *
     * @param range The duration range.
     * @return A new [DelayStrategy] representing the coerced delay durations.
     * @since 1.0.0
     */
    fun coerceIn(range: ClosedRange<Duration>) = object : DelayStrategy {
        override fun get(index: Int): Duration = this@DelayStrategy[index].coerceIn(range)
    }

    /**
     * Introduces jitter (randomness) to the retry delay, spreading out the timing of retries.
     *
     * Jitter helps:
     * - Prevent synchronized spikes in load.
     * - Avoid contention on shared resources.
     * - Improve system stability and throughput when many clients are retrying.
     *
     * @param jitterFactor The multiplier used to determine the range of jitter.
     * @param random An optional random number generator, defaulting to [Random.Default].
     * @return A [DelayStrategy] that incorporates jitter.
     * @since 1.0.0
     */
    fun withJitter(jitterFactor: Double, random: Random = Random.Default) = object : DelayStrategy {
        override fun get(index: Int): Duration = this@DelayStrategy[index].let { baseDelay ->
            val jitter = (random.nextDouble() * 2 - 1) * jitterFactor * baseDelay
            (baseDelay + jitter).coerceAtLeast(Duration.ZERO)
        }
    }
}

/**
 * Returns a [DelayStrategy] that yields the smallest delay duration for each index
 * when compared among the provided strategies.
 *
 * @param first The first [DelayStrategy] to compare.
 * @param others Vararg of other [DelayStrategy] instances to compare.
 * @return A new [DelayStrategy] representing the smallest delay duration for each index.
 * @since 1.0.0
 */
fun minOf(first: DelayStrategy, vararg others: DelayStrategy): DelayStrategy {
    return object : DelayStrategy {
        override fun get(index: Int) = arrayOf(first, *others).minOf { it[index] }
    }
}

/**
 * Returns a [DelayStrategy] that yields the largest delay duration for each index
 * when compared among the provided strategies.
 *
 * @param first The first [DelayStrategy] to compare.
 * @param others Vararg of other [DelayStrategy] instances to compare.
 * @return A new [DelayStrategy] representing the largest delay duration for each index.
 * @since 1.0.0
 */
fun maxOf(first: DelayStrategy, vararg others: DelayStrategy): DelayStrategy {
    return object : DelayStrategy {
        override fun get(index: Int) = arrayOf(first, *others).maxOf { it[index] }
    }
}

/**
 * Multiplies an [Int] with the delay durations of a [DelayStrategy].
 *
 * @receiver The integer multiplier.
 * @param delayStrategy The [DelayStrategy] to be multiplied.
 * @return A new [DelayStrategy] with delay durations multiplied by the receiver [Int].
 * @see DelayStrategy.times
 * @since 1.0.0
 */
operator fun Int.times(delayStrategy: DelayStrategy): DelayStrategy = object : DelayStrategy {
    override fun get(index: Int): Duration = this@times * delayStrategy[index]
}

/**
 * Multiplies a [Double] with the delay durations of a [DelayStrategy].
 *
 * @receiver The double multiplier.
 * @param delayStrategy The [DelayStrategy] to be multiplied.
 * @return A new [DelayStrategy] with delay durations multiplied by the receiver [Double].
 * @see DelayStrategy.times
 * @since 1.0.0
 */
operator fun Double.times(delayStrategy: DelayStrategy): DelayStrategy = object : DelayStrategy {
    override fun get(index: Int): Duration = this@times * delayStrategy[index]
}
