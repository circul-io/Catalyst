package io.circul.catalyst.policy.delay

import kotlin.js.JsName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class CommonDelayStrategiesTest {

    @Test
    @JsName("testConstantDelayFunctionWithDuration")
    fun `test constantDelay function with Duration `() {
        val delay = constantDelay(5.seconds)
        assertEquals(5.seconds, delay[0])
        assertEquals(5.seconds, delay[1])
        assertEquals(5.seconds, delay[2])
    }

    @Test
    @JsName("testConstantDelayFunctionWithLong")
    fun `test constantDelay function with Long `() {
        val delay = constantDelay(5000L)
        assertEquals(5.seconds, delay[0])
        assertEquals(5.seconds, delay[1])
        assertEquals(5.seconds, delay[2])
    }

    @Test
    @JsName("testDurationDelayPropertyExtension")
    fun `test Duration delay property extension`() {
        val delay = 5.seconds.constantDelay
        assertEquals(5.seconds, delay[0])
        assertEquals(5.seconds, delay[1])
        assertEquals(5.seconds, delay[2])
    }

    @Test
    @JsName("testNoDelayImmutableValue")
    fun `test noDelay immutable value`() {
        assertEquals(0.seconds, noDelay[0])
        assertEquals(Duration.ZERO, noDelay[1])
        assertEquals(Duration.ZERO, noDelay[2])
    }

    @Test
    @JsName("testSequentialDelayFunctionWithDurationList")
    fun `test sequentialDelay function with Duration List`() {
        val delay = sequentialDelay(listOf(1.seconds, 1.5.seconds, 5.seconds))
        assertEquals(1.seconds, delay[0])
        assertEquals(1.5.seconds, delay[1])
        assertEquals(5.seconds, delay[2])
        assertEquals(5.seconds, delay[3])
    }

    @Test
    @JsName("testSequentialDelayFunctionWithVarargLong")
    fun `test sequentialDelay function with vararg Long`() {
        val delay = sequentialDelay(1000L, 1500L, 5000L)
        assertEquals(1.seconds, delay[0])
        assertEquals(1.5.seconds, delay[1])
        assertEquals(5.seconds, delay[2])
        assertEquals(5.seconds, delay[3])
    }

    @Test
    @JsName("testDurationListSequentialDelayPropertyExtension")
    fun `test Duration List sequentialDelay property extension`() {
        val delay = listOf(1.seconds, 1.5.seconds, 5.seconds).sequentialDelay
        assertEquals(1.seconds, delay[0])
        assertEquals(1.5.seconds, delay[1])
        assertEquals(5.seconds, delay[2])
        assertEquals(5.seconds, delay[3])
    }

    @Test
    @JsName("testLinearBackoffDelayFunction")
    fun `test linearBackoffDelay function`() {
        val delay = linearBackoffDelay(1.seconds, 500.milliseconds)
        assertEquals(1.seconds, delay[0])
        assertEquals(1.5.seconds, delay[1])
        assertEquals(2.seconds, delay[2])
        assertEquals(2.5.seconds, delay[3])
    }

    @Test
    @JsName("testDurationDelayWithIncrementsOfDurationInfixFunction")
    fun `test Duration delayWithIncrementsOf Duration infix function`() {
        val delay = 1.seconds delayWithIncrementsOf 500.milliseconds
        assertEquals(1.seconds, delay[0])
        assertEquals(1.5.seconds, delay[1])
        assertEquals(2.seconds, delay[2])
        assertEquals(2.5.seconds, delay[3])
    }

    @Test
    @JsName("testFibonacciBackoffDelayFunction")
    fun `test fibonacciBackoffDelay function`() {
        var delay = fibonacciBackoffDelay(1.seconds)
        assertEquals(0.seconds, delay[0])
        assertEquals(1.seconds, delay[1])
        assertEquals(1.seconds, delay[2])
        assertEquals(2.seconds, delay[3])
        assertEquals(3.seconds, delay[4])
        assertEquals(5.seconds, delay[5])

        delay = fibonacciBackoffDelay(1.seconds, 2 to 2)
        assertEquals(2.seconds, delay[0])
        assertEquals(2.seconds, delay[1])
        assertEquals(4.seconds, delay[2])
        assertEquals(6.seconds, delay[3])
        assertEquals(10.seconds, delay[4])
        assertEquals(16.seconds, delay[5])
    }

    @Test
    @JsName("testDurationFibonacciBackoffDelayPropertyExtension")
    fun `test Duration fibonacciBackoffDelay property extension`() {
        val delay = 1.seconds.fibonacciBackoffDelay
        assertEquals(0.seconds, delay[0])
        assertEquals(1.seconds, delay[1])
        assertEquals(1.seconds, delay[2])
        assertEquals(2.seconds, delay[3])
        assertEquals(3.seconds, delay[4])
        assertEquals(5.seconds, delay[5])
    }

    @Test
    @JsName("testExponentialBackoffDelayFunction")
    fun `test exponentialBackoffDelay function`() {
        val delay = exponentialBackoffDelay(100.milliseconds, 1.5)
        assertEquals(100.milliseconds, delay[0])
        assertEquals(150.milliseconds, delay[1])
        assertEquals(225.milliseconds, delay[2])
        assertEquals(337.5.milliseconds, delay[3])
        assertEquals(506.25.milliseconds, delay[4])
    }

    @Test
    @JsName("testDurationDelayWithScaleFactorOfDoubleInfixFunction")
    fun `test Duration delayWithScaleFactorOf Double infix function`() {
        val delay = 100.milliseconds delayWithScaleFactorOf 1.5
        assertEquals(100.milliseconds, delay[0])
        assertEquals(150.milliseconds, delay[1])
        assertEquals(225.milliseconds, delay[2])
        assertEquals(337.5.milliseconds, delay[3])
        assertEquals(506.25.milliseconds, delay[4])
    }

    @Test
    @JsName("testCustomDelayFunction")
    fun `test customDelay function`() {
        val delay = customDelay { it.seconds + 100.milliseconds }
        assertEquals(0.1.seconds, delay[0])
        assertEquals(1.1.seconds, delay[1])
        assertEquals(2.1.seconds, delay[2])
        assertEquals(3.1.seconds, delay[3])
        assertEquals(4.1.seconds, delay[4])
    }

    @Test
    @JsName("testDelayFunctionLambdaCustomDelayPropertyExtension")
    fun `test DelayFunction lambda customDelay property extension`() {
        val delay = { i: Int -> i.seconds + 100.milliseconds }.customDelay
        assertEquals(0.1.seconds, delay[0])
        assertEquals(1.1.seconds, delay[1])
        assertEquals(2.1.seconds, delay[2])
        assertEquals(3.1.seconds, delay[3])
        assertEquals(4.1.seconds, delay[4])
    }
}
