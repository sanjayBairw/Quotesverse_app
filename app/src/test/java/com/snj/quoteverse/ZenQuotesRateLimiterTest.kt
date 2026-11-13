package com.snj.quoteverse

import com.snj.quoteverse.data.api.RateLimitExceededException
import com.snj.quoteverse.data.api.ZenQuotesRateLimiter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ZenQuotesRateLimiterTest {

    @Test
    fun rateLimiterAllowsFiveRequestsInThirtySecondsAndBlocksSixth() = runTest {
        var simulatedTime = 1_000_000L
        val rateLimiter = ZenQuotesRateLimiter(
            maxRequests = 5,
            windowMillis = 30_000L,
            clock = { simulatedTime }
        )

        // First 5 requests should succeed
        repeat(5) {
            assertTrue(rateLimiter.canMakeRequest())
            rateLimiter.acquire()
        }

        // 6th request within window must be blocked
        assertFalse(rateLimiter.canMakeRequest())
        try {
            rateLimiter.acquire()
            fail("Expected RateLimitExceededException on 6th request")
        } catch (e: RateLimitExceededException) {
            assertTrue("Expected retry seconds > 0", e.retryAfterSeconds > 0)
        }

        // Advance time by 30,001 ms -> oldest request expires
        simulatedTime += 30_001L

        assertTrue(rateLimiter.canMakeRequest())
        rateLimiter.acquire() // Succeeded
    }

    @Test
    fun recordRateLimitHitEnforcesPenaltyCooldown() = runTest {
        var simulatedTime = 1_000_000L
        val rateLimiter = ZenQuotesRateLimiter(
            maxRequests = 5,
            windowMillis = 30_000L,
            clock = { simulatedTime }
        )

        // Server returns rate limit warning
        rateLimiter.recordRateLimitHit(30_000L)

        assertFalse(rateLimiter.canMakeRequest())
        assertEquals(30, rateLimiter.getRemainingCooldownSeconds())

        simulatedTime += 30_001L
        assertTrue(rateLimiter.canMakeRequest())
    }
}

