package com.snj.quoteverse.data.api

import retrofit2.http.GET

/**
 * Retrofit service interface for the free ZenQuotes API endpoints.
 * Requires no API key.
 *
 * Endpoints:
 * - GET /api/random : Fetches a single random quote.
 * - GET /api/today  : Fetches the official quote of the day.
 * - GET /api/quotes : Fetches a batch of 50 curated quotes for Room caching.
 */
interface QuoteApiService {

    /**
     * Fetches a single random quote.
     * Endpoint: https://zenquotes.io/api/random
     */
    @GET("api/random")
    suspend fun getRandomQuote(): List<ZenQuoteDto>

    /**
     * Fetches the official quote of the day.
     * Endpoint: https://zenquotes.io/api/today
     */
    @GET("api/today")
    suspend fun getTodayQuote(): List<ZenQuoteDto>

    /**
     * Fetches a batch of 50 quotes for local Room caching.
     * Endpoint: https://zenquotes.io/api/quotes
     */
    @GET("api/quotes")
    suspend fun getQuotesBatch(): List<ZenQuoteDto>
}

