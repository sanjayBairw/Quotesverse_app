package com.snj.quoteverse.data.api

import java.io.IOException

/**
 * Remote data source abstraction for fetching quotes from the free ZenQuotes API.
 * Encapsulates network communication, JSON response validation, rate limiting, and error mapping.
 */
interface QuoteRemoteDataSource {

    /**
     * Fetches a single random quote from GET /api/random.
     */
    suspend fun getRandomQuote(): Result<ZenQuoteDto>

    /**
     * Fetches the official quote of the day from GET /api/today.
     */
    suspend fun getTodayQuote(): Result<ZenQuoteDto>

    /**
     * Fetches a batch of 50 quotes from GET /api/quotes for offline caching.
     */
    suspend fun getQuotesBatch(): Result<List<ZenQuoteDto>>
}

/**
 * Default production implementation of [QuoteRemoteDataSource] using Retrofit and [ZenQuotesRateLimiter].
 */
class QuoteRemoteDataSourceImpl(
    private val apiService: QuoteApiService = ZenQuotesApiClient.service,
    private val rateLimiter: ZenQuotesRateLimiter = ZenQuotesApiClient.rateLimiter
) : QuoteRemoteDataSource {

    override suspend fun getRandomQuote(): Result<ZenQuoteDto> {
        return try {
            rateLimiter.acquire()
            val dtos = apiService.getRandomQuote()
            val dto = dtos.firstOrNull()

            if (dto == null || !dto.isValidQuote()) {
                if (dto?.isRateLimitWarning() == true) {
                    rateLimiter.recordRateLimitHit()
                    val cooldown = rateLimiter.getRemainingCooldownSeconds()
                    return Result.failure(RateLimitExceededException(cooldown))
                }
                return Result.failure(IOException("Empty or invalid quote received from ZenQuotes API."))
            }

            Result.success(dto)
        } catch (e: RateLimitExceededException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTodayQuote(): Result<ZenQuoteDto> {
        return try {
            rateLimiter.acquire()
            val dtos = apiService.getTodayQuote()
            val dto = dtos.firstOrNull()

            if (dto == null || !dto.isValidQuote()) {
                if (dto?.isRateLimitWarning() == true) {
                    rateLimiter.recordRateLimitHit()
                    val cooldown = rateLimiter.getRemainingCooldownSeconds()
                    return Result.failure(RateLimitExceededException(cooldown))
                }
                return Result.failure(IOException("Empty or invalid daily quote received from ZenQuotes API."))
            }

            Result.success(dto)
        } catch (e: RateLimitExceededException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getQuotesBatch(): Result<List<ZenQuoteDto>> {
        return try {
            rateLimiter.acquire()
            val batch = apiService.getQuotesBatch()

            if (batch.firstOrNull()?.isRateLimitWarning() == true) {
                rateLimiter.recordRateLimitHit()
                val cooldown = rateLimiter.getRemainingCooldownSeconds()
                return Result.failure(RateLimitExceededException(cooldown))
            }

            val validQuotes = batch.filter { it.isValidQuote() }
            Result.success(validQuotes)
        } catch (e: RateLimitExceededException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

