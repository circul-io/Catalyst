package io.circul.catalyst.policy.predicate

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

// TODO re-do
class CommonRetryPredicatesTest {

    private val nullResult = Result.success<Any?>(null)
    private val intResult = Result.success<Any?>(1)
    private val stringResult = Result.success<Any?>("PASS")
    private val errorResult = Result.failure<Any?>(Exception("FAIL"))

    @Test
    fun test_attempts_predicate() {
        assertFailsWith(IllegalArgumentException::class) {
            attempts(0)
        }
        assertFailsWith(IllegalArgumentException::class) {
            attempts(-10)
        }
        attempts(1).let {
            assertFalse(it.shouldRetry(nullResult, 0))
            assertFalse(it.shouldRetry(nullResult, 1))
        }
        attempts(2).let {
            assertTrue(it.shouldRetry(nullResult, 0))
            assertFalse(it.shouldRetry(nullResult, 1))
            assertFalse(it.shouldRetry(nullResult, 2))
        }
    }

    @Test
    fun test_int_attempts_extension_predicate() {
        assertFailsWith(IllegalArgumentException::class) {
            0.attempts
        }
        assertFailsWith(IllegalArgumentException::class) {
            (-10).attempts
        }
        1.attempts.let {
            assertFalse(it.shouldRetry(nullResult, 0))
            assertFalse(it.shouldRetry(nullResult, 1))
        }
        2.attempts.let {
            assertTrue(it.shouldRetry(nullResult, 0))
            assertFalse(it.shouldRetry(nullResult, 1))
            assertFalse(it.shouldRetry(nullResult, 2))
        }
    }

    @Test
    fun test_onException_predicate() = onException.let {
        assertFalse(it.shouldRetry(intResult, 1))
        assertTrue(it.shouldRetry(errorResult, 1))
        assertFalse(it.shouldRetry(nullResult, 2))
    }

    @Test
    fun test_untilResult_predicate() = untilResult.let {
        assertTrue(it.shouldRetry(nullResult, 1))
        assertFalse(it.shouldRetry(stringResult, 1))
        assertTrue(it.shouldRetry(errorResult, 2))
        assertFalse(it.shouldRetry(intResult, 2))
    }

    @Test
    fun test_until_predicate() = until { result, i ->
        i == 10 || result.getOrNull() == "PASS" || result.isFailure
    }.let {
        assertFalse(it.shouldRetry(nullResult, 10))
        assertFalse(it.shouldRetry(stringResult, 1))
        assertFalse(it.shouldRetry(errorResult, 1))
        assertTrue(it.shouldRetry(nullResult, 9))
        assertTrue(it.shouldRetry(Result.success("FAIL"), 1))
    }

    @Test
    fun test_on_predicate() = on { result, i ->
        i < 10 && result.getOrNull() != "PASS" && !result.isFailure
    }.let {
        assertFalse(it.shouldRetry(nullResult, 10))
        assertFalse(it.shouldRetry(stringResult, 1))
        assertFalse(it.shouldRetry(errorResult, 1))
        assertTrue(it.shouldRetry(nullResult, 9))
        assertTrue(it.shouldRetry(Result.success("FAIL"), 1))
    }

    @Test
    fun test_onResultType_predicate() = onResultType<String>().let {
        assertTrue(it.shouldRetry(Result.success("PASS"), 1))
        assertFalse(it.shouldRetry(Result.success(1), 1))
        assertFalse(it.shouldRetry(Result.success(null), 1))
        assertFalse(it.shouldRetry(Result.failure(Exception("Test")), 1))
    }

    @Test
    fun test_untilResultType_predicate() = untilResultType<String>().let {
        assertFalse(it.shouldRetry(Result.success("PASS"), 1))
        assertTrue(it.shouldRetry(Result.success(1), 1))
        assertTrue(it.shouldRetry(Result.success(null), 1))
        assertTrue(it.shouldRetry(Result.failure(Exception("Test")), 1))
    }

    @Test
    fun test_timeLimit_predicate() {
        val timeSource = TestTimeSource()
        timeLimit(30.seconds, timeSource).let {
            assertTrue(it.shouldRetry(nullResult, 0))
            timeSource += 1.seconds
            assertTrue(it.shouldRetry(nullResult, 0))
            timeSource += 20.seconds
            assertTrue(it.shouldRetry(nullResult, 0))
            timeSource += 9.seconds
            assertFalse(it.shouldRetry(nullResult, 0))
        }
        30.seconds.timeLimit.let {

        }
    }
}
