package com.snj.quoteverse

import com.snj.quoteverse.data.api.QuoteRemoteDataSourceImpl
import com.snj.quoteverse.data.api.RateLimitExceededException
import com.snj.quoteverse.data.api.ZenQuoteDto
import com.snj.quoteverse.data.api.ZenQuotesRateLimiter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class QuoteRemoteDataSourceTest {

    private lateinit var fakeService: FakeZenQuotesApiService
    private lateinit var rateLimiter: ZenQuotesRateLimiter
    private lateinit var remoteDataSource: QuoteRemoteDataSourceImpl

    @Before
    fun setup() {
        fakeService = FakeZenQuotesApiService()
        rateLimiter = ZenQuotesRateLimiter(maxRequests = 5, windowMillis = 30_000L)
        remoteDataSource = QuoteRemoteDataSourceImpl(
            apiService = fakeService,
            rateLimiter = rateLimiter
        )
    }

    @Test
    fun getRandomQuoteReturnsValidQuoteSuccessfully() = runTest {
        fakeService.randomQuotesToReturn = listOf(
            ZenQuoteDto(
                quoteText = "Simplicity is the ultimate sophistication.",
                author = "Leonardo da Vinci"
            )
        )

        val result = remoteDataSource.getRandomQuote()
        assertTrue(result.isSuccess)
        val quote = result.getOrNull()
        assertNotNull(quote)
        assertEquals("Simplicity is the ultimate sophistication.", quote?.quoteText)
        assertEquals("Leonardo da Vinci", quote?.author)
    }

    @Test
    fun getTodayQuoteReturnsValidDailyQuote() = runTest {
        fakeService.todayQuotesToReturn = listOf(
            ZenQuoteDto(
                quoteText = "Act as if what you do makes a difference. It does.",
                author = "William James"
            )
        )

        val result = remoteDataSource.getTodayQuote()
        assertTrue(result.isSuccess)
        val quote = result.getOrNull()
        assertNotNull(quote)
        assertEquals("William James", quote?.author)
    }

    @Test
    fun getQuotesBatchReturnsBatchOfValidQuotes() = runTest {
        val result = remoteDataSource.getQuotesBatch()
        assertTrue(result.isSuccess)
        val batch = result.getOrNull()
        assertNotNull(batch)
        assertEquals(50, batch?.size)
    }

    @Test
    fun handlesRateLimitWarningFromZenQuotesGracefully() = runTest {
        fakeService.shouldReturnRateLimitWarning = true

        val result = remoteDataSource.getRandomQuote()
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is RateLimitExceededException)
        assertFalse(rateLimiter.canMakeRequest())
    }

    @Test
    fun handlesNetworkExceptionsGracefullyWithoutCrashing() = runTest {
        fakeService.shouldThrowNetworkError = true

        val result = remoteDataSource.getRandomQuote()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }
}
