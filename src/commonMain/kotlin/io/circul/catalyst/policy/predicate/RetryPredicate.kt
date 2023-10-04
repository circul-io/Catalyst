package io.circul.catalyst.policy.predicate


/**
 * Represents a retry predicate that determines whether a block of code should be retried
 * based on the result of its execution and the current retry count.
 *
 * The [shouldRetry] method must be implemented to specify the retry condition.
 *
 * In addition to [shouldRetry], this interface provides default implementations for boolean
 * logic operations, allowing for the creation of complex retry conditions by combining multiple
 * retry predicates.
 *
 * Usage:
 * ```kotlin
 * val alwaysRetry = object : RetryPredicate {
 *     override fun shouldRetry(result: Result<Any?>, retryCount: Int) = true
 * }
 * ```
 * @since 1.0.0
 */
interface RetryPredicate {

    /**
     * Called by [retry], determines whether a retry should be performed based on the [result] of execution
     * and the [retryCount].
     *
     * [retry]: io.circul.catalyst.retry
     *
     * @param result The [Result] of the execution.
     * @param retryCount The number of times the operation has been retried.
     * @return `true` if the operation should be retried, `false` otherwise.
     * @since 1.0.0
     */
    fun shouldRetry(result: Result<Any?>, retryCount: Int): Boolean

    /**
     * Negates the retry condition, inverting the result of [shouldRetry].
     *
     * #### Truth Table
     * ```
     * A | NOT A
     * ---------
     * 0 | 1
     * 1 | 0
     * ```
     *
     * #### Usage
     * ```kotlin
     * val predicateNotA = !predicateA
     * ```
     *
     * @return a [RetryPredicate] that negates the result of the original predicate.
     * @since 1.0.0
     */
    operator fun not() = object : RetryPredicate {
        override fun shouldRetry(result: Result<Any?>, retryCount: Int): Boolean =
            !this@RetryPredicate.shouldRetry(result, retryCount)
    }

    /**
     * Combines this predicate with another using logical AND (conjunction).
     *
     * #### Truth Table
     * ```
     * A | B | A AND B
     * --------------
     * 0 | 0 | 0
     * 0 | 1 | 0
     * 1 | 0 | 0
     * 1 | 1 | 1
     * ```
     *
     * #### Usage
     * ```kotlin
     * val predicateC = predicateA and predicateB
     * ```
     *
     * @param other The other [RetryPredicate] to be combined with.
     * @return a [RetryPredicate] that returns `true` only if both predicates return `true`.
     * @since 1.0.0
     */
    infix fun and(other: RetryPredicate): RetryPredicate = object : RetryPredicate {
        override fun shouldRetry(result: Result<Any?>, retryCount: Int): Boolean =
            this@RetryPredicate.shouldRetry(result, retryCount) && other.shouldRetry(result, retryCount)
    }

    /**
     * Combines this predicate with another using logical OR (disjunction).
     *
     * #### Truth Table
     * ```
     * A | B | A OR B
     * --------------
     * 0 | 0 | 0
     * 0 | 1 | 1
     * 1 | 0 | 1
     * 1 | 1 | 1
     * ```
     *
     * #### Usage
     * ```kotlin
     * val predicateC = predicateA or predicateB
     * ```
     *
     * @param other The other [RetryPredicate] to be combined with.
     * @return a [RetryPredicate] that returns `true` if either predicate returns `true`.
     * @since 1.0.0
     */
    infix fun or(other: RetryPredicate): RetryPredicate = object : RetryPredicate {
        override fun shouldRetry(result: Result<Any?>, retryCount: Int): Boolean =
            this@RetryPredicate.shouldRetry(result, retryCount) || other.shouldRetry(result, retryCount)
    }

    /**
     * Combines this predicate with another using logical XOR (exclusive OR).
     *
     * #### Truth Table
     * ```
     * A | B | A XOR B
     * ---------------
     * 0 | 0 | 0
     * 0 | 1 | 1
     * 1 | 0 | 1
     * 1 | 1 | 0
     * ```
     *
     * #### Usage
     * ```kotlin
     * val predicateC = predicateA xor predicateB
     * ```
     *
     * @param other The other [RetryPredicate] to be combined with.
     * @return a [RetryPredicate] that returns `true` only if exactly one predicate returns `true`.
     * @since 1.0.0
     */
    infix fun xor(other: RetryPredicate): RetryPredicate = object : RetryPredicate {
        override fun shouldRetry(result: Result<Any?>, retryCount: Int): Boolean =
            this@RetryPredicate.shouldRetry(result, retryCount) != other.shouldRetry(result, retryCount)
    }

    /**
     * Combines this predicate with another using logical NAND.
     *
     * #### Truth Table
     * ```
     * A | B | A NAND B
     * ----------------
     * 0 | 0 | 1
     * 0 | 1 | 1
     * 1 | 0 | 1
     * 1 | 1 | 0
     * ```
     *
     * #### Usage
     * ```kotlin
     * val predicateC = predicateA nand predicateB
     * ```
     *
     * @param other The other [RetryPredicate] to be combined with.
     * @return a [RetryPredicate] that returns `true` only if at least one predicate returns `false`.
     * @since 1.0.0
     */
    infix fun nand(other: RetryPredicate): RetryPredicate = object : RetryPredicate {
        override fun shouldRetry(result: Result<Any?>, retryCount: Int): Boolean =
            !(this@RetryPredicate.shouldRetry(result, retryCount) && other.shouldRetry(result, retryCount))
    }

    /**
     * Combines this predicate with another using logical NOR.
     *
     * #### Truth Table
     * ```
     * A | B | A NOR B
     * ---------------
     * 0 | 0 | 1
     * 0 | 1 | 0
     * 1 | 0 | 0
     * 1 | 1 | 0
     * ```
     *
     * #### Usage
     * ```kotlin
     * val predicateC = predicateA nor predicateB
     * ```
     *
     * @param other The other [RetryPredicate] to be combined with.
     * @return a [RetryPredicate] that returns `true` only if both predicates return `false`.
     * @since 1.0.0
     */
    infix fun nor(other: RetryPredicate): RetryPredicate = object : RetryPredicate {
        override fun shouldRetry(result: Result<Any?>, retryCount: Int): Boolean =
            !(this@RetryPredicate.shouldRetry(result, retryCount) || other.shouldRetry(result, retryCount))
    }

    /**
     * Combines this predicate with another using logical XNOR.
     *
     * #### Truth Table
     * ```
     * A | B | A XNOR B
     * ----------------
     * 0 | 0 | 1
     * 0 | 1 | 0
     * 1 | 0 | 0
     * 1 | 1 | 1
     * ```
     *
     * #### Usage
     * ```kotlin
     * val predicateC = predicateA xnor predicateB
     * ```
     *
     * @param other The other [RetryPredicate] to be combined with.
     * @return a [RetryPredicate] that returns `true` only if both predicates return the same value.
     * @since 1.0.0
     */
    infix fun xnor(other: RetryPredicate): RetryPredicate = object : RetryPredicate {
        override fun shouldRetry(result: Result<Any?>, retryCount: Int): Boolean =
            this@RetryPredicate.shouldRetry(result, retryCount) == other.shouldRetry(result, retryCount)
    }
}
