package com.snj.quoteverse

import com.snj.quoteverse.data.api.RateLimitExceededException
import com.snj.quoteverse.data.api.ZenQuoteDto
import com.snj.quoteverse.data.api.ZenQuotesRateLimiter
import com.snj.quoteverse.data.entity.QuoteEntity
import com.snj.quoteverse.data.repository.QuoteRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ZenQuotesRepositoryIntegrationTest {

    private lateinit var fakeDao: FakeQuoteDao
    private lateinit var fakePreferences: FakeQuotePreferences
    private lateinit var fakeApiService: FakeZenQuotesApiService
    private lateinit var rateLimiter: ZenQuotesRateLimiter
    private lateinit var repository: QuoteRepository

    @Before
    fun setup() {
        fakeDao = FakeQuoteDao()
        fakePreferences = FakeQuotePreferences()
        fakeApiService = FakeZenQuotesApiService()
        rateLimiter = ZenQuotesRateLimiter(maxRequests = 5, windowMillis = 30_000L)
        repository = QuoteRepository(
            quoteDao = fakeDao,
            preferences = fakePreferences,
            apiService = fakeApiService,
            rateLimiter = rateLimiter
        )
    }

    @Test
    fun syncTodayQuoteCachesNewQuoteAndSetsAsDaily() = runTest {
        val result = repository.syncTodayQuoteFromApi(force = true, todayKey = "2026-11-13")
        assertTrue(result.isSuccess)

        val daily = result.getOrNull()
        assertNotNull(daily)
        assertTrue(daily!!.isDailyQuote)
        assertEquals("H. Jackson Brown Jr.", daily.author)

        val dbDaily = fakeDao.getDailyQuoteOnce()
        assertEquals(daily.id, dbDaily?.id)
        assertEquals("2026-11-13", fakePreferences.getLastTodayApiSyncDate())
    }

    @Test
    fun syncTodayQuoteDeduplicatesIfQuoteAlreadyExistsInRoom() = runTest {
        val existingId = fakeDao.insertQuote(
            QuoteEntity(
                quoteText = "The best preparation for tomorrow is doing your best today.",
                author = "H. Jackson Brown Jr.",
                category = "Study",
                isDailyQuote = false
            )
        )
        val initialCount = fakeDao.getQuoteCount()

        val result = repository.syncTodayQuoteFromApi(force = true, todayKey = "2026-11-13")
        assertTrue(result.isSuccess)

        val daily = result.getOrNull()
        assertNotNull(daily)
        assertEquals(existingId, daily!!.id)
        assertEquals(initialCount, fakeDao.getQuoteCount()) // No duplicate inserted!
        assertTrue(fakeDao.getQuoteByIdOnce(existingId)?.isDailyQuote == true)
    }

    @Test
    fun fetchQuotesBatchInsertsFiftyQuotesAndFiltersDuplicates() = runTest {
        // Pre-insert 10 overlapping quotes
        for (i in 1..10) {
            fakeDao.insertQuote(
                QuoteEntity(
                    quoteText = "Inspirational batch quote #$i",
                    author = "Author $i",
                    category = "Motivation"
                )
            )
        }
        val countBefore = fakeDao.getQuoteCount()
        assertEquals(10, countBefore)

        val result = repository.fetchQuotesBatchFromApi()
        assertTrue(result.isSuccess)
        val newCount = result.getOrNull()!!
        assertEquals(40, newCount) // 50 total minus 10 pre-existing duplicates

        val totalQuotes = fakeDao.getQuoteCount()
        assertEquals(50, totalQuotes)
    }

    @Test
    fun fetchQuotesBatchRejectsRateLimitWarningPayload() = runTest {
        fakeApiService.shouldReturnRateLimitWarning = true

        val result = repository.fetchQuotesBatchFromApi()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RateLimitExceededException)

        // Ensure rate limit warning quote was not inserted into Room
        val all = fakeDao.getAllQuotes().first()
        assertTrue(all.none { it.author == "zenquotes.io" })
    }

    @Test
    fun rateLimiterEnforcesFiveRequestsLimitAcrossOperations() = runTest {
        repeat(5) {
            fakeApiService.randomQuotesToReturn = listOf(
                ZenQuoteDto(quoteText = "Quote $it", author = "Author $it")
            )
            val result = repository.fetchRandomQuoteFromApi()
            assertTrue(result.isSuccess)
        }

        // 6th request must be throttled
        val sixthResult = repository.fetchRandomQuoteFromApi()
        assertTrue(sixthResult.isFailure)
        assertTrue(sixthResult.exceptionOrNull() is RateLimitExceededException)
    }
}

