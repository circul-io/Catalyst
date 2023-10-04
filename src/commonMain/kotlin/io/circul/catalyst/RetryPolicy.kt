package io.circul.catalyst

import io.circul.catalyst.delay.DelayStrategy
import io.circul.catalyst.delay.noDelay
import io.circul.catalyst.predicate.RetryPredicate
import io.circul.catalyst.predicate.onException
import kotlinx.coroutines.delay


/**
 * Represents a block to be retried, taking an integer parameter (the retry count)
 * and returning a value of type [T].
 *
 * @since 1.0.0
 */
private typealias RetryBlock<T> = suspend (Int) -> T

/**
 * Represents a retry policy that combines a [RetryPredicate] and a [DelayStrategy].
 *
 * @param retryPredicate The [RetryPredicate] used to evaluate whether a retry should be performed.
 * @param delayStrategy The [DelayStrategy] used to determine the delay before a retry is attempted.
 *
 * @constructor Creates a new [RetryPolicy] with the specified [RetryPredicate] and [DelayStrategy] delegates.
 * @since 1.0.0
 */
open class RetryPolicy(
    private val retryPredicate: RetryPredicate = onException,
    private val delayStrategy: DelayStrategy = noDelay
) {

    /**
     * Retries the given [block] using this [RetryPolicy]
     *
     * @param block The block to be executed and potentially retried.
     * @return The result of the [block], if successful.
     * @throws Throwable if the [block] fails and no further retry attempts should be made.
     * @since 1.0.0
     */
    suspend fun <T> retry(block: RetryBlock<T>): T = retry(0, block)

    /**
     * Private recursive function to execute and potentially retry the [block] based on this [RetryPolicy]
     *
     * @param retryCounter The current retry attempt count.
     * @param block The block to be executed and potentially retried.
     * @return The result of the [block], if successful.
     * @throws Throwable if the [block] fails and no further retry attempts should be made.
     * @since 1.0.0
     */
    private suspend fun <T> retry(
        retryCounter: Int,
        block: RetryBlock<T>
    ): T {
        val result = try {
            Result.success(block(retryCounter))
        } catch (e: Throwable) {
            Result.failure(e)
        }

        if (retryPredicate.shouldRetry(result, retryCounter)) {
            delay(delayStrategy[retryCounter])
            return retry(retryCounter + 1, block)
        }

        return result.getOrThrow()
    }
}

/**
 * Combines a [RetryPredicate] with a [DelayStrategy] to create a [RetryPolicy].
 *
 * @receiver The [RetryPredicate] determining whether a retry should occur.
 * @param that The [DelayStrategy] to use for calculating delays between retries.
 * @return A [RetryPolicy] combining the retry predicate and delay strategy.
 * @since 1.0.0
 */
infix fun RetryPredicate.with(that: DelayStrategy): RetryPolicy = RetryPolicy(this, that)

/**
 * Combines a [DelayStrategy] with a [RetryPredicate] to create a [RetryPolicy].
 *
 * @receiver The [DelayStrategy] to use for calculating delays between retries.
 * @param that The [RetryPredicate] determining whether a retry should occur.
 * @return A [RetryPolicy] combining the delay strategy and retry predicate.
 * @since 1.0.0
 */
infix fun DelayStrategy.with(that: RetryPredicate): RetryPolicy = RetryPolicy(that, this)

/**
 * Converts a [RetryPredicate] to a [RetryPolicy] with a [DelayStrategy] of [noDelay]
 *
 * @receiver a [RetryPredicate] to convert.
 * @return A [RetryPolicy] with this retry predicate.
 * @since 1.0.0
 */
val RetryPredicate.asRetryPolicy: RetryPolicy
    get() = RetryPolicy(retryPredicate = this)

/**
 * Converts a [DelayStrategy] to a [RetryPolicy] with a [RetryPredicate] of [onException]
 *
 * @receiver a [RetryPredicate] to convert.
 * @return A [RetryPolicy] with this retry predicate.
 * @since 1.0.0
 */
val DelayStrategy.asRetryPolicy: RetryPolicy
    get() = RetryPolicy(delayStrategy = this)
