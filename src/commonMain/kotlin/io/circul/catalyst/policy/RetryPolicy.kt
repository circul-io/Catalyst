package io.circul.catalyst.policy

import io.circul.catalyst.policy.delay.DelayStrategy
import io.circul.catalyst.policy.predicate.RetryPredicate

/**
 * Represents a retry policy that combines a [RetryPredicate] and a [DelayStrategy].
 * The [RetryPolicy] class delegates its retry predicate and retry delay behaviors to the specified delegates.
 * This allows for flexible and composable retry policies.
 *
 * @param retryPredicateDelegate The [RetryPredicate] delegate used to evaluate whether a retry should be performed.
 * @param delayStrategyDelegate The [DelayStrategy] delegate used to determine the delay before a retry is attempted.
 *
 * @constructor Creates a new [RetryPolicy] with the specified [RetryPredicate] and [DelayStrategy] delegates.
 * @since 1.0.0
 */
open class RetryPolicy(
    retryPredicateDelegate: RetryPredicate,
    delayStrategyDelegate: DelayStrategy
) : RetryPredicate by retryPredicateDelegate, DelayStrategy by delayStrategyDelegate
