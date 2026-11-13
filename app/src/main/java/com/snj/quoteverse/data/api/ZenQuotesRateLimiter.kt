package com.snj.quoteverse.data.api

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Exception thrown when the free ZenQuotes API rate limit (5 requests per 30 seconds)
 * is reached.
 */
class RateLimitExceededException(
    val retryAfterSeconds: Int,
    message: String = "ZenQuotes rate limit (5 requests / 30s) reached. Retry in $retryAfterSeconds seconds."
) : Exception(message)

/**
 * Enforces the free ZenQuotes API rate limit of at most 5 requests per 30 seconds.
 * Tracks rolling window timestamps and supports penalty cooldown if the API
 * returns a rate limit response payload.
 */
class ZenQuotesRateLimiter(
    private val maxRequests: Int = 5,
    private val windowMillis: Long = 30_000L,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {

    private val mutex = Mutex()
    private val requestTimestamps = ArrayDeque<Long>()
    private var penaltyCooldownUntilMillis: Long = 0L

    /**
     * Checks if a request can be executed right now.
     */
    suspend fun canMakeRequest(): Boolean = mutex.withLock {
        val now = clock()
        if (now < penaltyCooldownUntilMillis) return false

        cleanupOldTimestamps(now)
        requestTimestamps.size < maxRequests
    }

    /**
     * Returns remaining cooldown in milliseconds, or 0 if a request can be made immediately.
     */
    suspend fun getRemainingCooldownMillis(): Long = mutex.withLock {
        val now = clock()
        if (now < penaltyCooldownUntilMillis) {
            return penaltyCooldownUntilMillis - now
        }

        cleanupOldTimestamps(now)
        if (requestTimestamps.size >= maxRequests) {
            val oldest = requestTimestamps.first()
            val availableAt = oldest + windowMillis
            return (availableAt - now).coerceAtLeast(0L)
        }

        0L
    }

    /**
     * Returns remaining cooldown in whole seconds (rounded up).
     */
    suspend fun getRemainingCooldownSeconds(): Int {
        val millis = getRemainingCooldownMillis()
        if (millis <= 0L) return 0
        return ((millis + 999L) / 1000L).toInt()
    }

    /**
     * Records a successful execution against the rolling rate limit window.
     * Throws [RateLimitExceededException] if rate limit is currently exhausted.
     */
    suspend fun acquire() = mutex.withLock {
        val now = clock()
        if (now < penaltyCooldownUntilMillis) {
            val seconds = ((penaltyCooldownUntilMillis - now + 999L) / 1000L).toInt()
            throw RateLimitExceededException(seconds)
        }

        cleanupOldTimestamps(now)
        if (requestTimestamps.size >= maxRequests) {
            val oldest = requestTimestamps.first()
            val seconds = ((oldest + windowMillis - now + 999L) / 1000L).toInt().coerceAtLeast(1)
            throw RateLimitExceededException(seconds)
        }

        requestTimestamps.addLast(now)
    }

    /**
     * Signals that the server returned a rate-limit payload ("Too many requests"),
     * enforcing an explicit cooldown penalty.
     */
    suspend fun recordRateLimitHit(cooldownMillis: Long = windowMillis) = mutex.withLock {
        val now = clock()
        penaltyCooldownUntilMillis = now + cooldownMillis
        requestTimestamps.clear()
        repeat(maxRequests) { requestTimestamps.addLast(now) }
    }

    /**
     * Resets rate limiter state (used in unit tests).
     */
    suspend fun reset() = mutex.withLock {
        requestTimestamps.clear()
        penaltyCooldownUntilMillis = 0L
    }

    private fun cleanupOldTimestamps(now: Long) {
        val threshold = now - windowMillis
        while (requestTimestamps.isNotEmpty() && requestTimestamps.first() <= threshold) {
            requestTimestamps.removeFirst()
        }
    }
}

