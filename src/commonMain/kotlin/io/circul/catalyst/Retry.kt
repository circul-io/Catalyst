package io.circul.catalyst

import io.circul.catalyst.policy.RetryPolicy
import io.circul.catalyst.policy.delay.DelayStrategy
import io.circul.catalyst.policy.predicate.RetryPredicate
import kotlinx.coroutines.delay

/**
 * Represents a factory function that produces a [RetryPredicate] instance.
 *
 * @since 1.0.0
 * @see retry
 */
typealias RetryPredicateFactory = () -> RetryPredicate

/**
 * Represents a factory function that produces a [RetryPolicy] instance.
 *
 * @since 1.0.0
 * @see retry
 */
typealias RetryPolicyFactory = () -> RetryPolicy

/**
 * Represents a block to be retried, taking an integer parameter (the retry count)
 * and returning a value of type [T].
 *
 * @see retry
 * @since 1.0.0
 */
typealias RetryBlock<T> = suspend (Int) -> T

/**
 * Retries the given [block] based on the specified [retryPredicateFactory] and [delayStrategy].
 *
 * Use this version of [retry] if you want to store your retry predicates in a variable for reuse,
 * and you have stateful predicates (like [timeLimit]) that need to reset their state each time
 * [retry] is used.
 *
 * [timeLimit]: io.circul.catalyst.policy.predicate.timeLimit
 *
 * @param retryPredicateFactory A factory function that produces a [RetryPredicate] instance.
 * @param delayStrategy The delay strategy between retry attempts.
 * @param block The block to be executed and potentially retried.
 * @return The result of the [block], if successful.
 * @throws Throwable if the [block] fails and no further retry attempts should be made.
 * @since 1.0.0
 */
suspend fun <T> retry(
    retryPredicateFactory: RetryPredicateFactory,
    delayStrategy: DelayStrategy,
    block: RetryBlock<T>
): T = retry(retryPredicateFactory(), delayStrategy, block)

/**
 * Retries the given [block] based on the specified [retryPredicate] and [delayStrategy].
 *
 * @param retryPredicate The predicate to determine if a retry should be attempted.
 * @param delayStrategy The delay strategy between retry attempts.
 * @param block The block to be executed and potentially retried.
 * @return The result of the [block], if successful.
 * @throws Throwable if the [block] fails and no further retry attempts should be made.
 * @since 1.0.0
 */
suspend fun <T> retry(
    retryPredicate: RetryPredicate,
    delayStrategy: DelayStrategy,
    block: RetryBlock<T>
): T = retry(RetryPolicy(retryPredicate, delayStrategy), block)

/**
 * Retries the given [block] based on the specified [retryPolicyFactory].
 *
 * Use this version of [retry] if you want to store your retry policy in a variable for reuse,
 * and you have stateful predicates (like [timeLimit]) that need to reset their state each time
 * [retry] is used.
 *
 * [timeLimit]: io.circul.catalyst.policy.predicate.timeLimit
 *
 * @param retryPolicyFactory A factory function that produces a [RetryPolicy] instance.
 * @param block The block to be executed and potentially retried.
 * @return The result of the [block], if successful.
 * @throws Throwable if the [block] fails and no further retry attempts should be made.
 * @since 1.0.0
 */
suspend fun <T> retry(
    retryPolicyFactory: RetryPolicyFactory,
    block: RetryBlock<T>
): T = retry(retryPolicyFactory(), 0, block)

/**
 * Retries the given [block] based on the specified [retryPolicy].
 *
 * @param retryPolicy The policy to determine if and how a retry should be attempted.
 * @param block The block to be executed and potentially retried.
 * @return The result of the [block], if successful.
 * @throws Throwable if the [block] fails and no further retry attempts should be made.
 * @since 1.0.0
 */
suspend fun <T> retry(
    retryPolicy: RetryPolicy,
    block: RetryBlock<T>
): T = retry(retryPolicy, 0, block)

/**
 * Private recursive function to execute and potentially retry the [block] based on the given
 * [retryPolicy] and [retryCounter].
 *
 * All other [retry] functions ultimately execute this one.
 *
 * @param retryPolicy The policy to determine if and how a retry should be attempted.
 * @param retryCounter The current retry attempt count.
 * @param block The block to be executed and potentially retried.
 * @return The result of the [block], if successful.
 * @throws Throwable if the [block] fails and no further retry attempts should be made.
 * @since 1.0.0
 */
private suspend fun <T> retry(
    retryPolicy: RetryPolicy,
    retryCounter: Int,
    block: RetryBlock<T>
): T {
    val result = try {
        Result.success(block(retryCounter))
    } catch (e: Throwable) {
        Result.failure(e)
    }

    if (retryPolicy.shouldRetry(result, retryCounter)) {
        delay(retryPolicy[retryCounter])
        return retry(retryPolicy, retryCounter + 1, block)
    }

    return result.getOrThrow()
}
