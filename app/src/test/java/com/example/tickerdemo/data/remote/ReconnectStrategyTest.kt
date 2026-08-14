package com.example.tickerdemo.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ReconnectStrategyTest {

    @Test
    fun `delay grows exponentially with attempt number`() {
        val strategy = ReconnectStrategy(baseDelayMillis = 1_000, maxDelayMillis = 60_000, random = NoJitter)

        assertEquals(1_000L, strategy.nextDelayMillis(1))
        assertEquals(2_000L, strategy.nextDelayMillis(2))
        assertEquals(4_000L, strategy.nextDelayMillis(3))
        assertEquals(8_000L, strategy.nextDelayMillis(4))
    }

    @Test
    fun `delay is capped at maxDelayMillis even for very high attempt counts`() {
        val strategy = ReconnectStrategy(baseDelayMillis = 1_000, maxDelayMillis = 30_000, random = NoJitter)

        assertEquals(30_000L, strategy.nextDelayMillis(20))
    }

    @Test
    fun `jitter keeps the delay within the configured ratio of the exponential value`() {
        val strategy = ReconnectStrategy(baseDelayMillis = 1_000, maxDelayMillis = 60_000, jitterRatio = 0.25)

        repeat(50) {
            val delay = strategy.nextDelayMillis(3) // exponential = 4_000
            assertTrue("delay=$delay should be within [3000, 5000]", delay in 3_000..5_000)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `attempt below 1 is rejected`() {
        ReconnectStrategy().nextDelayMillis(0)
    }

    private object NoJitter : Random() {
        override fun nextBits(bitCount: Int): Int = 0
        override fun nextDouble(from: Double, until: Double): Double = (from + until) / 2
    }
}
