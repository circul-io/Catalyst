package io.circul.catalyst.predicate

import io.circul.catalyst.predicate.RetryPredicate
import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RetryPredicateTest {

    private val alwaysRetry = object : RetryPredicate {
        override fun shouldRetry(result: Result<Any?>, retryCount: Int) = true
    }
    private val neverRetry = object : RetryPredicate {
        override fun shouldRetry(result: Result<Any?>, retryCount: Int) = false
    }

    private val nullResult = Result.success<Any?>(null)

    @Test
    @JsName("testNotOperator")
    fun `test NOT operator`() {
        assertFalse(neverRetry.shouldRetry(nullResult, 0))
        assertTrue(!neverRetry.shouldRetry(nullResult, 0))
        assertTrue(alwaysRetry.shouldRetry(nullResult, 0))
        assertFalse(!alwaysRetry.shouldRetry(nullResult, 0))

    }

    @Test
    @JsName("testAndOperator")
    fun `test AND operator`() {
        assertFalse((neverRetry and neverRetry).shouldRetry(nullResult, 0))
        assertFalse((neverRetry and alwaysRetry).shouldRetry(nullResult, 0))
        assertFalse((alwaysRetry and neverRetry).shouldRetry(nullResult, 0))
        assertTrue((alwaysRetry and alwaysRetry).shouldRetry(nullResult, 0))
    }

    @Test
    @JsName("testOrOperator")
    fun `test OR operator`() {
        assertFalse((neverRetry or neverRetry).shouldRetry(nullResult, 0))
        assertTrue((neverRetry or alwaysRetry).shouldRetry(nullResult, 0))
        assertTrue((alwaysRetry or neverRetry).shouldRetry(nullResult, 0))
        assertTrue((alwaysRetry or alwaysRetry).shouldRetry(nullResult, 0))
    }

    @Test
    @JsName("testXorOperator")
    fun `test XOR operator`() {
        assertFalse((neverRetry xor neverRetry).shouldRetry(nullResult, 0))
        assertTrue((neverRetry xor alwaysRetry).shouldRetry(nullResult, 0))
        assertTrue((alwaysRetry xor neverRetry).shouldRetry(nullResult, 0))
        assertFalse((alwaysRetry xor alwaysRetry).shouldRetry(nullResult, 0))
    }

    @Test
    @JsName("testNandOperator")
    fun `test NAND operator`() {
        assertTrue((neverRetry nand neverRetry).shouldRetry(nullResult, 0))
        assertTrue((neverRetry nand alwaysRetry).shouldRetry(nullResult, 0))
        assertTrue((alwaysRetry nand neverRetry).shouldRetry(nullResult, 0))
        assertFalse((alwaysRetry nand alwaysRetry).shouldRetry(nullResult, 0))
    }

    @Test
    @JsName("testNorOperator")
    fun `test NOR operator`() {
        assertTrue((neverRetry nor neverRetry).shouldRetry(nullResult, 0))
        assertFalse((neverRetry nor alwaysRetry).shouldRetry(nullResult, 0))
        assertFalse((alwaysRetry nor neverRetry).shouldRetry(nullResult, 0))
        assertFalse((alwaysRetry nor alwaysRetry).shouldRetry(nullResult, 0))
    }

    @Test
    @JsName("testXnorOperator")
    fun `test XNOR operator`() {
        assertTrue((neverRetry xnor neverRetry).shouldRetry(nullResult, 0))
        assertFalse((neverRetry xnor alwaysRetry).shouldRetry(nullResult, 0))
        assertFalse((alwaysRetry xnor neverRetry).shouldRetry(nullResult, 0))
        assertTrue((alwaysRetry xnor alwaysRetry).shouldRetry(nullResult, 0))
    }
}
