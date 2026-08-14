package com.example.tickerdemo.data.remote

import kotlin.random.Random

/**
 * Exponential backoff with +/-25% jitter, capped at [maxDelayMillis].
 * Jitter avoids every disconnected client hammering the server in lockstep after an outage.
 */
class ReconnectStrategy(
    private val baseDelayMillis: Long = 1_000,
    private val maxDelayMillis: Long = 30_000,
    private val jitterRatio: Double = 0.25,
    private val random: Random = Random.Default,
) {
    fun nextDelayMillis(attempt: Int): Long {
        require(attempt >= 1) { "attempt must be >= 1, was $attempt" }

        val shift = (attempt - 1).coerceAtMost(MAX_SHIFT)
        val exponential = (baseDelayMillis shl shift).coerceAtMost(maxDelayMillis)
        val jitter = 1.0 + random.nextDouble(-jitterRatio, jitterRatio)

        return (exponential * jitter).toLong().coerceIn(baseDelayMillis, maxDelayMillis)
    }

    private companion object {
        const val MAX_SHIFT = 10
    }
}
