package io.circul.catalyst.predicate

import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * Represents a predicate function that produces a [Boolean] for a given [Result] and the retry count [Int].
 *
 * Used by [on] and [until] for creating custom [RetryPredicate]s
 *
 * @since 1.0.0
 */
typealias PredicateFunction = (Result<Any?>, Int) -> Boolean

/**
 * Returns a [RetryPredicate] that determines if a retry should be performed based on the number
 * of attempts made. It will allow retries as long as the retry count is less than the specified
 * number ([n]) minus one.
 *
 * @param n the maximum number of attempts to allow (including the initial attempt). Must be greater than 0.
 * @return a [RetryPredicate] that allows retries until the retry count reaches [n]-1.
 * @throws IllegalArgumentException if [n] is not greater than 0.
 * @since 1.0.0
 */
fun attempts(n: Int) = object : RetryPredicate {
    init {
        require(n > 0) { "attempts must be > 0" }
    }

    override fun shouldRetry(result: Result<Any?>, retryCount: Int): Boolean = retryCount < n - 1
}

/**
 * Extension property that converts an [Int] to a [RetryPredicate], which allows retries
 * until the retry count reaches this integer minus one.
 *
 * Usage:
 * ```
 * val predicate = 3.attempts
 * ```
 *
 * @since 1.0.0
 */
val Int.attempts get() = attempts(this)

/**
 * A [RetryPredicate] implementation that checks whether the result is a failure.
 *
 * This predicate will return `true` for [shouldRetry] if the result is a failure (i.e., an excepton is thrown),
 * indicating a retry should be attempted.
 *
 * [shouldRetry]: io.circul.catalyst.policy.predicate.RetryPredicate.shouldRetry
 *
 * @since 1.0.0
 */
val onException = object : RetryPredicate {

    override fun shouldRetry(result: Result<Any?>, retryCount: Int): Boolean = result.isFailure
}

/**
 * A [RetryPredicate] implementation that checks whether the result is null and not an error.
 *
 * This predicate will return `true` for [shouldRetry] if a successful result is `null`, indicating a retry should be
 * attempted.
 *
 * [shouldRetry]: io.circul.catalyst.policy.predicate.RetryPredicate.shouldRetry
 *
 * @since 1.0.0
 */
val onNull = object : RetryPredicate {

    override fun shouldRetry(result: Result<Any?>, retryCount: Int): Boolean =
        result.isSuccess && result.getOrNull() == null
}

/**
 * A [RetryPredicate] implementation that checks whether the result is null or an error.
 *
 * This predicate will return `true` for [shouldRetry] if the result is `null` or an error, indicating a retry should be
 * attempted.
 *
 * [untilResult] is the equivalent of `onException or onNull`
 *
 * [shouldRetry]: io.circul.catalyst.policy.predicate.RetryPredicate.shouldRetry
 *
 * @see onException
 * @see io.circul.catalyst.policy.predicate.RetryPredicate.or
 * @see onNull
 * @since 1.0.0
 */
val untilResult = onException or onNull

/**
 * Returns a [RetryPredicate] that mirrors the result of the given [predicate].
 * It will allow retries when the [predicate] returns `false`.
 *
 * @param predicate a lambda expression that takes a [Result] and an [Int] (retry count) as parameters and returns a [Boolean].
 * @return a [RetryPredicate] that mirrors the result of the [predicate].
 * @since 1.0.0
 */
inline fun on(crossinline predicate: PredicateFunction) = object : RetryPredicate {

    override fun shouldRetry(result: Result<Any?>, retryCount: Int): Boolean = predicate(result, retryCount)
}

/**
 * Returns a [RetryPredicate] that negates the result of the given [predicate] lambda function.
 * It will allow retries until the [predicate] returns `true`.
 *
 * @param predicate a lambda expression that takes a [Result] and an [Int] (retry count) as parameters and returns a [Boolean].
 * @return a [RetryPredicate] that negates the result of the [predicate].
 * @since 1.0.0
 */
inline fun until(crossinline predicate: PredicateFunction) = object : RetryPredicate {

    override fun shouldRetry(result: Result<Any?>, retryCount: Int): Boolean = !predicate(result, retryCount)
}

/**
 * Returns a [RetryPredicate] that checks if the result is of the specified type [T].
 *
 * This can be used to only retry operations if the result is a certain type. The check is performed
 * using Kotlin's `is` keyword, which returns `true` for objects of the specified type or any of its subclasses.
 *
 * @return a [RetryPredicate] that allows retries when the result is of type [T]
 * @since 1.0.0
 */
inline fun <reified T> onResultType(): RetryPredicate = object : RetryPredicate {
    override fun shouldRetry(result: Result<Any?>, retryCount: Int) = result.getOrNull() is T
}

/**
 * Returns a [RetryPredicate] that checks if the result is *not* of the specified type [T].
 *
 * This can be used to retry operations until the result is not a certain type. The check is performed
 * using Kotlin's `is` keyword, so it will return `false` for objects of the specified type or any of its subclasses.
 *
 * @return a [RetryPredicate] that allows retries when the result is *not* of type [T]
 * @since 1.0.0
 */
inline fun <reified T> untilResultType(): RetryPredicate = object : RetryPredicate {
    override fun shouldRetry(result: Result<Any?>, retryCount: Int) = result.getOrNull() !is T
}

/**
 * Returns a [RetryPredicate] that limits the retry time to the specified [duration].
 * The predicate will allow retries as long as the elapsed time since the first attempt is less than [duration].
 *
 * @param duration The maximum amount of time to keep retrying.
 * @param clock The [TimeSource] to use for tracking the elapsed time, defaulting to [TimeSource.Monotonic].
 * @return A [RetryPredicate] that limits the retry time to the specified [duration].
 * @since 1.0.0
 */
fun timeLimit(duration: Duration, clock: TimeSource = TimeSource.Monotonic) = object : RetryPredicate {
    private val startTime = clock.markNow()

    override fun shouldRetry(result: Result<Any?>, retryCount: Int): Boolean = startTime.elapsedNow() < duration
}

/**
 * Extension property on [Duration] to get a [RetryPredicate] that limits the retry time to this duration.
 *
 * @since 1.0.0
 */
val Duration.timeLimit get() = timeLimit(this)
