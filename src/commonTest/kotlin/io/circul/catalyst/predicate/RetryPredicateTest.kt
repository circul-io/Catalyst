package io.circul.catalyst.predicate

import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class RetryPredicateTest {

    private val alwaysRetry = object : RetryPredicate {
        override fun shouldRetry(result: Result<Any?>, retryCount: Int, elapsedTime: Duration) = true
    }
    private val neverRetry = object : RetryPredicate {
        override fun shouldRetry(result: Result<Any?>, retryCount: Int, elapsedTime: Duration) = false
    }

    private val nullResult = Result.success<Any?>(null)
    private val elapsedTime = 234.milliseconds

    @Test
    @JsName("testNotOperator")
    fun `test NOT operator`() {
        assertFalse(neverRetry.shouldRetry(nullResult, 0, elapsedTime))
        assertTrue(!neverRetry.shouldRetry(nullResult, 0, elapsedTime))
        assertTrue(alwaysRetry.shouldRetry(nullResult, 0, elapsedTime))
        assertFalse(!alwaysRetry.shouldRetry(nullResult, 0, elapsedTime))

    }

    @Test
    @JsName("testAndOperator")
    fun `test AND operator`() {
        assertFalse((neverRetry and neverRetry).shouldRetry(nullResult, 0, elapsedTime))
        assertFalse((neverRetry and alwaysRetry).shouldRetry(nullResult, 0, elapsedTime))
        assertFalse((alwaysRetry and neverRetry).shouldRetry(nullResult, 0, elapsedTime))
        assertTrue((alwaysRetry and alwaysRetry).shouldRetry(nullResult, 0, elapsedTime))
    }

    @Test
    @JsName("testOrOperator")
    fun `test OR operator`() {
        assertFalse((neverRetry or neverRetry).shouldRetry(nullResult, 0, elapsedTime))
        assertTrue((neverRetry or alwaysRetry).shouldRetry(nullResult, 0, elapsedTime))
        assertTrue((alwaysRetry or neverRetry).shouldRetry(nullResult, 0, elapsedTime))
        assertTrue((alwaysRetry or alwaysRetry).shouldRetry(nullResult, 0, elapsedTime))
    }

    @Test
    @JsName("testXorOperator")
    fun `test XOR operator`() {
        assertFalse((neverRetry xor neverRetry).shouldRetry(nullResult, 0, elapsedTime))
        assertTrue((neverRetry xor alwaysRetry).shouldRetry(nullResult, 0, elapsedTime))
        assertTrue((alwaysRetry xor neverRetry).shouldRetry(nullResult, 0, elapsedTime))
        assertFalse((alwaysRetry xor alwaysRetry).shouldRetry(nullResult, 0, elapsedTime))
    }

    @Test
    @JsName("testNandOperator")
    fun `test NAND operator`() {
        assertTrue((neverRetry nand neverRetry).shouldRetry(nullResult, 0, elapsedTime))
        assertTrue((neverRetry nand alwaysRetry).shouldRetry(nullResult, 0, elapsedTime))
        assertTrue((alwaysRetry nand neverRetry).shouldRetry(nullResult, 0, elapsedTime))
        assertFalse((alwaysRetry nand alwaysRetry).shouldRetry(nullResult, 0, elapsedTime))
    }

    @Test
    @JsName("testNorOperator")
    fun `test NOR operator`() {
        assertTrue((neverRetry nor neverRetry).shouldRetry(nullResult, 0, elapsedTime))
        assertFalse((neverRetry nor alwaysRetry).shouldRetry(nullResult, 0, elapsedTime))
        assertFalse((alwaysRetry nor neverRetry).shouldRetry(nullResult, 0, elapsedTime))
        assertFalse((alwaysRetry nor alwaysRetry).shouldRetry(nullResult, 0, elapsedTime))
    }

    @Test
    @JsName("testXnorOperator")
    fun `test XNOR operator`() {
        assertTrue((neverRetry xnor neverRetry).shouldRetry(nullResult, 0, elapsedTime))
        assertFalse((neverRetry xnor alwaysRetry).shouldRetry(nullResult, 0, elapsedTime))
        assertFalse((alwaysRetry xnor neverRetry).shouldRetry(nullResult, 0, elapsedTime))
        assertTrue((alwaysRetry xnor alwaysRetry).shouldRetry(nullResult, 0, elapsedTime))
    }
}
