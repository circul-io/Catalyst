package io.circul.catalyst.predicate

import kotlin.js.JsName
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class CommonRetryPredicatesTest {

    private val nullResult = Result.success<Any?>(null)
    private val intResult = Result.success<Any?>(1)
    private val stringResult = Result.success<Any?>("PASS")
    private val errorResult = Result.failure<Any?>(Exception("FAIL"))
    private val elapsedTime = 234.milliseconds

    @Test
    @JsName("testAttemptsPredicateFunction")
    fun `test attempts predicate function`() {
        assertFailsWith(IllegalArgumentException::class) {
            attempts(0)
        }
        assertFailsWith(IllegalArgumentException::class) {
            attempts(-10)
        }
        attempts(1).let {
            assertFalse(it.shouldRetry(nullResult, 0, elapsedTime))
            assertFalse(it.shouldRetry(nullResult, 1, elapsedTime))
        }
        attempts(2).let {
            assertTrue(it.shouldRetry(nullResult, 0, elapsedTime))
            assertFalse(it.shouldRetry(nullResult, 1, elapsedTime))
            assertFalse(it.shouldRetry(nullResult, 2, elapsedTime))
        }
    }

    @Test
    @JsName("testIntAttemptsPropertyExtension")
    fun `test Int attempts property extension`() {
        assertFailsWith(IllegalArgumentException::class) {
            0.attempts
        }
        assertFailsWith(IllegalArgumentException::class) {
            (-10).attempts
        }
        1.attempts.let {
            assertFalse(it.shouldRetry(nullResult, 0, elapsedTime))
            assertFalse(it.shouldRetry(nullResult, 1, elapsedTime))
        }
        2.attempts.let {
            assertTrue(it.shouldRetry(nullResult, 0, elapsedTime))
            assertFalse(it.shouldRetry(nullResult, 1, elapsedTime))
            assertFalse(it.shouldRetry(nullResult, 2, elapsedTime))
        }
    }

    @Test
    @JsName("testOnExceptionPredicateValue")
    fun `test onException predicate value`() = onException.let {
        assertFalse(it.shouldRetry(intResult, 1, elapsedTime))
        assertTrue(it.shouldRetry(errorResult, 1, elapsedTime))
        assertFalse(it.shouldRetry(nullResult, 2, elapsedTime))
    }

    @Test
    @JsName("testOnNullPredicateValue")
    fun `test onNull predicateValue`() = onNull.let {
        assertFalse(it.shouldRetry(intResult, 1, elapsedTime))
        assertFalse(it.shouldRetry(errorResult, 1, elapsedTime))
        assertTrue(it.shouldRetry(nullResult, 2, elapsedTime))
    }

    @Test
    @JsName("testUntilResultPredicate")
    fun `test untilResult predicate`() = untilResult.let {
        assertTrue(it.shouldRetry(nullResult, 1, elapsedTime))
        assertFalse(it.shouldRetry(stringResult, 1, elapsedTime))
        assertTrue(it.shouldRetry(errorResult, 2, elapsedTime))
        assertFalse(it.shouldRetry(intResult, 2, elapsedTime))
    }

    @Test
    fun test_until_predicate() = until { result, i, _ ->
        i == 10 || result.getOrNull() == "PASS" || result.isFailure
    }.let {
        assertFalse(it.shouldRetry(nullResult, 10, elapsedTime))
        assertFalse(it.shouldRetry(stringResult, 1, elapsedTime))
        assertFalse(it.shouldRetry(errorResult, 1, elapsedTime))
        assertTrue(it.shouldRetry(nullResult, 9, elapsedTime))
        assertTrue(it.shouldRetry(Result.success("FAIL"), 1, elapsedTime))
    }

    @Test
    fun test_on_predicate() = on { result, i, _ ->
        i < 10 && result.getOrNull() != "PASS" && !result.isFailure
    }.let {
        assertFalse(it.shouldRetry(nullResult, 10, elapsedTime))
        assertFalse(it.shouldRetry(stringResult, 1, elapsedTime))
        assertFalse(it.shouldRetry(errorResult, 1, elapsedTime))
        assertTrue(it.shouldRetry(nullResult, 9, elapsedTime))
        assertTrue(it.shouldRetry(Result.success("FAIL"), 1, elapsedTime))
    }

    @Test
    fun test_onResultType_predicate() = onResultType<String>().let {
        assertTrue(it.shouldRetry(Result.success("PASS"), 1, elapsedTime))
        assertFalse(it.shouldRetry(Result.success(1), 1, elapsedTime))
        assertFalse(it.shouldRetry(Result.success(null), 1, elapsedTime))
        assertFalse(it.shouldRetry(Result.failure(Exception("Test")), 1, elapsedTime))
    }

    @Test
    fun test_untilResultType_predicate() = untilResultType<String>().let {
        assertFalse(it.shouldRetry(Result.success("PASS"), 1, elapsedTime))
        assertTrue(it.shouldRetry(Result.success(1), 1, elapsedTime))
        assertTrue(it.shouldRetry(Result.success(null), 1, elapsedTime))
        assertTrue(it.shouldRetry(Result.failure(Exception("Test")), 1, elapsedTime))
    }

    @Test
    fun test_timeLimit_predicate() {
        timeLimit(30.seconds).let {
            assertTrue(it.shouldRetry(nullResult, 0, 0.seconds))
            assertTrue(it.shouldRetry(nullResult, 0, 1.seconds))
            assertTrue(it.shouldRetry(nullResult, 0, 21.seconds))
            assertFalse(it.shouldRetry(nullResult, 0, 30.seconds))
            assertFalse(it.shouldRetry(nullResult, 0, 32.seconds))
        }
        30.seconds.timeLimit.let {
            assertTrue(it.shouldRetry(nullResult, 0, 0.seconds))
            assertTrue(it.shouldRetry(nullResult, 0, 1.seconds))
            assertTrue(it.shouldRetry(nullResult, 0, 21.seconds))
            assertFalse(it.shouldRetry(nullResult, 0, 30.seconds))
            assertFalse(it.shouldRetry(nullResult, 0, 32.seconds))
        }
    }

    @Test
    @JsName("testCustomStatefulPredicate")
    fun `test custom stateful RetryPredicate`() {

        val customPredicate = object : RetryPredicate {
            val mapOfExceptionCounts = mutableMapOf<KClass<out Throwable>, Int>()

            override fun shouldRetry(result: Result<Any?>, retryCount: Int, elapsedTime: Duration): Boolean {
                if (result.isFailure) {
                    val exceptionClass = result.exceptionOrNull()!!::class
                    val count = mapOfExceptionCounts.getOrPut(exceptionClass) { 0 } + 1
                    mapOfExceptionCounts[exceptionClass] = count
                    return count < 3
                }
                return false
            }
        }

        assertTrue(
            customPredicate.shouldRetry(
                Result.failure(ArithmeticException("ArithmeticException 1")), 0, elapsedTime
            )
        )
        assertTrue(
            customPredicate.shouldRetry(
                Result.failure(IllegalStateException("IllegalStateException 1")), 0, elapsedTime
            )
        )
        assertTrue(
            customPredicate.shouldRetry(
                Result.failure(ArithmeticException("ArithmeticException 2")), 0, elapsedTime
            )
        )
        assertTrue(
            customPredicate.shouldRetry(
                Result.failure(IllegalStateException("IllegalStateException 2")), 0, elapsedTime
            )
        )
        assertFalse(
            customPredicate.shouldRetry(
                Result.failure(ArithmeticException("ArithmeticException 3")), 0, elapsedTime
            )
        )
        assertFalse(
            customPredicate.shouldRetry(
                Result.failure(IllegalStateException("IllegalStateException 3")), 0, elapsedTime
            )
        )
    }
}
