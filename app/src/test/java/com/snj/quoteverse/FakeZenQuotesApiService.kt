package com.snj.quoteverse

import com.snj.quoteverse.data.api.ZenQuoteDto
import com.snj.quoteverse.data.api.ZenQuotesApiService
import java.io.IOException

/**
 * Fake implementation of [ZenQuotesApiService] for fast deterministic JVM testing.
 */
class FakeZenQuotesApiService : ZenQuotesApiService {

    var randomQuotesToReturn: List<ZenQuoteDto> = listOf(
        ZenQuoteDto(quoteText = "Peace begins with a smile.", author = "Mother Teresa")
    )

    var todayQuotesToReturn: List<ZenQuoteDto> = listOf(
        ZenQuoteDto(quoteText = "The best preparation for tomorrow is doing your best today.", author = "H. Jackson Brown Jr.")
    )

    var batchQuotesToReturn: List<ZenQuoteDto> = (1..50).map { index ->
        ZenQuoteDto(
            quoteText = "Inspirational batch quote #$index",
            author = "Author $index",
            characterCount = "30"
        )
    }

    var shouldThrowNetworkError: Boolean = false
    var shouldReturnRateLimitWarning: Boolean = false

    var randomCallsCount = 0
    var todayCallsCount = 0
    var batchCallsCount = 0

    override suspend fun getRandomQuote(): List<ZenQuoteDto> {
        randomCallsCount++
        checkErrors()
        if (shouldReturnRateLimitWarning) return rateLimitWarningList()
        return randomQuotesToReturn
    }

    override suspend fun getTodayQuote(): List<ZenQuoteDto> {
        todayCallsCount++
        checkErrors()
        if (shouldReturnRateLimitWarning) return rateLimitWarningList()
        return todayQuotesToReturn
    }

    override suspend fun getQuotesBatch(): List<ZenQuoteDto> {
        batchCallsCount++
        checkErrors()
        if (shouldReturnRateLimitWarning) return rateLimitWarningList()
        return batchQuotesToReturn
    }

    private fun checkErrors() {
        if (shouldThrowNetworkError) {
            throw IOException("Simulated network timeout")
        }
    }

    private fun rateLimitWarningList(): List<ZenQuoteDto> = listOf(
        ZenQuoteDto(
            quoteText = "Too many requests. Obtain an auth key for unlimited access, or try again later.",
            author = "zenquotes.io"
        )
    )
}
