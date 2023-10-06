package io.circul.catalyst

import io.circul.catalyst.delay.constantDelay
import io.circul.catalyst.predicate.attempts
import io.circul.catalyst.predicate.on
import io.circul.catalyst.predicate.onException
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds

class RetryPolicyTest {

    @Test
    @JsName("retrySucceedsWithoutException")
    fun `retry succeeds without exception`() = runTest {
        val policy = RetryPolicy(/* default policy is onException with noDelay */)
        val result = policy.retry { 42 }
        assertEquals(42, result)
    }

    @Test
    @JsName("retryDoesNotRetryWhenPredicateReturnsFalse")
    fun `retry does not retry when predicate returns false`() = runTest {
        val policy = on { _, _, _ -> false }.asRetryPolicy
        assertFailsWith<RuntimeException> {
            policy.retry { throw RuntimeException("Error!") }
        }
    }

    @Test
    @JsName("retrySucceedsAfterSeveralFailedAttempts")
    fun `retry succeeds after several failed attempts`() = runTest {
        val policy = onException.asRetryPolicy
        var count = 0
        val result = policy.retry {
            count++
            if (count < 4) throw RuntimeException("Error!")
            42
        }
        assertEquals(4, count)
        assertEquals(42, result)
    }

    @Test
    @JsName("retryStopsAfterHittingMaxRetries")
    fun `retry stops after hitting max retries`() = runTest {
        val policy = 10.attempts.asRetryPolicy
        var count = 0
        assertFailsWith<RuntimeException> {
            policy.retry {
                count++
                throw RuntimeException("Error!")
            }
        }
        assertEquals(10, count)
    }

    @Test
    @JsName("retryDelaysBetweenRetries")
    @kotlinx.coroutines.ExperimentalCoroutinesApi
    fun `retry delays properly between retries`() = runTest {
        val policy = 10.attempts with 500.milliseconds.constantDelay
        assertEquals(0, currentTime)
        assertFailsWith<RuntimeException> {
            policy.retry {
                throw RuntimeException("Error!")
            }
        }
        assertEquals(4500, currentTime)
    }
}
