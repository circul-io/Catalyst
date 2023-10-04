package io.circul.catalyst.delay

import kotlin.js.JsName
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class DelayStrategyTest {

    class TestRandom(vararg returnValues: Double) : Random() {

        private val doubles: MutableList<Double> = returnValues.toMutableList()
        override fun nextBits(bitCount: Int): Int = 0
        override fun nextDouble(): Double = doubles.removeAt(0).also {
            doubles.add(it)
        }
    }

    private val constantStrategy = object : DelayStrategy {
        override fun get(index: Int): Duration = 10.seconds
    }

    private val increasingStrategy = object : DelayStrategy {
        override fun get(index: Int): Duration = (index + 1).seconds
    }

    @Test
    @JsName("testPlusOperator")
    fun `test PLUS operator`() {
        val combined = constantStrategy + increasingStrategy
        assertEquals(11.seconds, combined[0])
        assertEquals(12.seconds, combined[1])
    }

    @Test
    @JsName("testMinusOperator")
    fun `test MINUS operator`() {
        val combined = constantStrategy - increasingStrategy
        assertEquals(9.seconds, combined[0])
        assertEquals(8.seconds, combined[1])
    }

    @Test
    @JsName("testTimesByIntOperator")
    fun `test TIMES by Int operator`() {
        val multiplied = increasingStrategy * 2
        assertEquals(2.seconds, multiplied[0])
        assertEquals(4.seconds, multiplied[1])
        assertEquals(6.seconds, multiplied[2])
    }

    @Test
    @JsName("testTimesByDoubleOperator")
    fun `test TIMES by Double operator`() {
        val multiplied = increasingStrategy * 0.5
        assertEquals(0.5.seconds, multiplied[0])
        assertEquals(1.seconds, multiplied[1])
        assertEquals(1.5.seconds, multiplied[2])
    }

    @Test
    @JsName("testDivByIntOperator")
    fun `test DIV by Int operator`() {
        val divided = constantStrategy / 2
        assertEquals(5.seconds, divided[0])
    }

    @Test
    @JsName("testDivByDoubleOperator")
    fun `test DIV by Double operator`() {
        val multiplied = constantStrategy / 0.5
        assertEquals(20.seconds, multiplied[0])
    }

    @Test
    @JsName("testUnaryMinusOperator")
    fun `test UNARY MINUS operator`() {
        val negated = -constantStrategy
        assertEquals(-(10).seconds, negated[0])
    }

    @Test
    @JsName("testUnaryPlusOperator")
    fun `test UNARY PLUS operator`() {
        val same = +constantStrategy
        assertEquals(10.seconds, same[0])
    }

    @Test
    @JsName("testCoerceAtMostMethod")
    fun `test coerceAtMost method`() {
        val coerced = increasingStrategy.coerceAtMost(5.seconds)
        assertEquals(3.seconds, coerced[2])
        assertEquals(4.seconds, coerced[3])
        assertEquals(5.seconds, coerced[4])
        assertEquals(5.seconds, coerced[5])
        assertEquals(5.seconds, coerced[6])
    }

    @Test
    @JsName("testCoerceAtLeastMethod")
    fun `test coerceAtLeast method`() {
        val coerced = increasingStrategy.coerceAtLeast(5.seconds)
        assertEquals(5.seconds, coerced[2])
        assertEquals(5.seconds, coerced[3])
        assertEquals(5.seconds, coerced[4])
        assertEquals(6.seconds, coerced[5])
        assertEquals(7.seconds, coerced[6])
    }

    @Test
    @JsName("testCoerceInMinMaxMethod")
    fun `test coerceIn min-max method`() {
        val coerced = increasingStrategy.coerceIn(3.seconds, 6.seconds)
        assertEquals(3.seconds, coerced[1])
        assertEquals(3.seconds, coerced[2])
        assertEquals(4.seconds, coerced[3])
        assertEquals(5.seconds, coerced[4])
        assertEquals(6.seconds, coerced[5])
        assertEquals(6.seconds, coerced[6])
    }

    @Test
    @JsName("testCoerceInRangeMethod")
    fun `test coerceIn range method`() {
        val coerced = increasingStrategy.coerceIn(3.seconds..6.seconds)
        assertEquals(3.seconds, coerced[1])
        assertEquals(3.seconds, coerced[2])
        assertEquals(4.seconds, coerced[3])
        assertEquals(5.seconds, coerced[4])
        assertEquals(6.seconds, coerced[5])
        assertEquals(6.seconds, coerced[6])
    }

    @Test
    @JsName("testWithJitterMethod")
    fun `test withJitter method`() {
        val random = TestRandom(
            0.3, // (0.3 * 2 - 1) * 0.5 * 10 = -2
            0.6, // (0.6 * 2 - 1) * 0.5 * 10 = 1
            0.1  // (0.1 * 2 - 1) * 0.5 * 10 = -4
        )
        val jittered = constantStrategy.withJitter(0.5, random)
        assertEquals(8.seconds, jittered[0])  // 10 - 2 = 8 seconds
        assertEquals(11.seconds, jittered[1]) // 10 + 1 = 11 seconds
        assertEquals(6.seconds, jittered[2])  // 10 - 4 = 6 seconds
        assertEquals(8.seconds, jittered[3])  // Same as [0], TestRandom is circular
    }

    @Test
    @JsName("testMinOfFunction")
    fun `test minOf function`() {
        val strategy = io.circul.catalyst.delay.minOf(constantStrategy, increasingStrategy)
        assertEquals(1.seconds, strategy[0])
        assertEquals(2.seconds, strategy[1])
        assertEquals(8.seconds, strategy[7])
        assertEquals(9.seconds, strategy[8])
        assertEquals(10.seconds, strategy[9])
        assertEquals(10.seconds, strategy[10])
        assertEquals(10.seconds, strategy[11])
    }

    @Test
    @JsName("testMaxOfMethod")
    fun `test maxOf method`() {
        val strategy = io.circul.catalyst.delay.maxOf(constantStrategy, increasingStrategy)
        assertEquals(10.seconds, strategy[0])
        assertEquals(10.seconds, strategy[1])
        assertEquals(10.seconds, strategy[7])
        assertEquals(10.seconds, strategy[8])
        assertEquals(10.seconds, strategy[9])
        assertEquals(11.seconds, strategy[10])
        assertEquals(12.seconds, strategy[11])
    }

    @Test
    @JsName("testIntTimesMethod")
    fun `test Int times method`() {
        val multiplied = 2 * increasingStrategy
        assertEquals(2.seconds, multiplied[0])
        assertEquals(4.seconds, multiplied[1])
        assertEquals(6.seconds, multiplied[2])

    }

    @Test
    @JsName("testDoubleTimesMethod")
    fun `test Double times method`() {
        val multiplied = 0.5 * increasingStrategy
        assertEquals(0.5.seconds, multiplied[0])
        assertEquals(1.seconds, multiplied[1])
        assertEquals(1.5.seconds, multiplied[2])
    }
}
