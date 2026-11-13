package com.snj.quoteverse.data.repository

import com.snj.quoteverse.data.api.QuoteApiService
import com.snj.quoteverse.data.api.QuoteCategorizer
import com.snj.quoteverse.data.api.QuoteRemoteDataSource
import com.snj.quoteverse.data.api.QuoteRemoteDataSourceImpl
import com.snj.quoteverse.data.api.ZenQuotesApiClient
import com.snj.quoteverse.data.api.ZenQuotesRateLimiter
import com.snj.quoteverse.data.dao.QuoteDao
import com.snj.quoteverse.data.database.QuoteSeedData
import com.snj.quoteverse.data.entity.QuoteEntity
import com.snj.quoteverse.utils.QuotePreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Single source of truth coordinating offline Room quotes, local preferences,
 * and the free ZenQuotes API (respecting the 5 requests / 30 seconds limit).
 */
class QuoteRepository(
    private val quoteDao: QuoteDao,
    private val preferences: QuotePreferences,
    private val remoteDataSource: QuoteRemoteDataSource = QuoteRemoteDataSourceImpl()
) {

    /**
     * Backward-compatible secondary constructor for testing or custom services.
     */
    constructor(
        quoteDao: QuoteDao,
        preferences: QuotePreferences,
        apiService: QuoteApiService,
        rateLimiter: ZenQuotesRateLimiter = ZenQuotesApiClient.rateLimiter
    ) : this(
        quoteDao = quoteDao,
        preferences = preferences,
        remoteDataSource = QuoteRemoteDataSourceImpl(apiService, rateLimiter)
    )

    private val seedMutex = Mutex()

    val allQuotes: Flow<List<QuoteEntity>> = quoteDao.getAllQuotes()
    val favoriteQuotes: Flow<List<QuoteEntity>> = quoteDao.getFavoriteQuotes()
    val favoriteCount: Flow<Int> = quoteDao.getFavoriteCount()
    val dailyQuote: Flow<QuoteEntity?> = quoteDao.getDailyQuote()

    val recentlyViewedQuotes: Flow<List<QuoteEntity>> = combine(
        quoteDao.getAllQuotes(),
        preferences.recentQuoteIds
    ) { quotes, recentIds ->
        if (recentIds.isEmpty()) {
            emptyList()
        } else {
            val quoteById = quotes.associateBy { it.id }
            recentIds.mapNotNull { quoteById[it] }
        }
    }

    /**
     * Ensures initial offline quotes exist in Room and a daily quote is assigned for today.
     * Offline-first: initializes from Room/seed data immediately without blocking on network.
     */
    suspend fun ensureInitialized(todayKey: String = currentDateKey()): QuoteEntity? {
        seedMutex.withLock {
            if (quoteDao.getQuoteCount() == 0) {
                quoteDao.insertQuotes(QuoteSeedData.getInitialQuotes())
            }
            val currentDaily = quoteDao.getDailyQuoteOnce()
            val savedDate = preferences.getLastDailyQuoteDate()

            if (currentDaily == null || savedDate != todayKey) {
                val nextDaily = if (currentDaily != null) {
                    quoteDao.getRandomQuoteExcluding(currentDaily.id) ?: currentDaily
                } else {
                    quoteDao.getRandomQuote()
                }
                if (nextDaily != null) {
                    quoteDao.setDailyQuote(nextDaily.id)
                    preferences.setLastDailyQuoteDate(todayKey)
                    if (preferences.getRecentQuoteIds().isEmpty()) {
                        preferences.addRecentQuoteId(nextDaily.id)
                    }
                    return quoteDao.getQuoteByIdOnce(nextDaily.id) ?: nextDaily.copy(isDailyQuote = true)
                }
            }
            if (currentDaily != null && preferences.getRecentQuoteIds().isEmpty()) {
                preferences.addRecentQuoteId(currentDaily.id)
            }
            return currentDaily
        }
    }

    fun getQuoteById(id: Long): Flow<QuoteEntity?> = quoteDao.getQuoteById(id)

    suspend fun getQuoteByIdOnce(id: Long): QuoteEntity? = quoteDao.getQuoteByIdOnce(id)

    fun observeQuotes(query: String, category: String?): Flow<List<QuoteEntity>> {
        val trimmedQuery = query.trim()
        val normalizedCategory = category?.trim()?.takeIf {
            it.isNotEmpty() && !it.equals(ALL_CATEGORY, ignoreCase = true)
        }

        return when {
            trimmedQuery.isBlank() && normalizedCategory == null -> quoteDao.getAllQuotes()
            trimmedQuery.isBlank() && normalizedCategory != null -> quoteDao.getQuotesByCategory(normalizedCategory)
            trimmedQuery.isNotBlank() && normalizedCategory == null -> quoteDao.searchQuotes(trimmedQuery)
            else -> quoteDao.searchQuotesByCategory(trimmedQuery, normalizedCategory!!)
        }
    }

    suspend fun getRandomQuote(
        excludeId: Long? = null,
        category: String? = null
    ): QuoteEntity? {
        ensureInitialized()
        val normalizedCategory = category?.trim()?.takeIf {
            it.isNotEmpty() && !it.equals(ALL_CATEGORY, ignoreCase = true)
        }
        val exclusion = excludeId ?: -1L

        val candidate = if (normalizedCategory != null) {
            quoteDao.getRandomQuoteByCategory(normalizedCategory, exclusion)
                ?: quoteDao.getRandomQuoteExcluding(exclusion)
        } else {
            quoteDao.getRandomQuoteExcluding(exclusion)
        }
        return candidate ?: quoteDao.getRandomQuote()
    }

    suspend fun setDailyQuote(quoteId: Long, dateKey: String = currentDateKey()) {
        quoteDao.setDailyQuote(quoteId)
        preferences.setLastDailyQuoteDate(dateKey)
    }

    suspend fun toggleFavorite(quote: QuoteEntity): Boolean {
        val nextState = !quote.isFavorite
        quoteDao.updateFavorite(quote.id, nextState)
        return nextState
    }

    suspend fun setFavorite(quoteId: Long, isFavorite: Boolean) {
        quoteDao.updateFavorite(quoteId, isFavorite)
    }

    fun recordQuoteViewed(quoteId: Long) {
        preferences.addRecentQuoteId(quoteId)
    }

    suspend fun getDailyQuoteOnce(): QuoteEntity? {
        ensureInitialized()
        return quoteDao.getDailyQuoteOnce() ?: quoteDao.getRandomQuote()
    }

    fun getLastSyncTimestamp(): Long = preferences.getLastSyncTimestamp()

    /**
     * Synchronizes today's quote from ZenQuotes (/api/today) without creating duplicates.
     * Respects rate limit (5 req / 30s) and works offline gracefully.
     */
    suspend fun syncTodayQuoteFromApi(
        force: Boolean = false,
        todayKey: String = currentDateKey()
    ): Result<QuoteEntity?> {
        ensureInitialized(todayKey)

        if (!force && preferences.getLastTodayApiSyncDate() == todayKey) {
            return Result.success(quoteDao.getDailyQuoteOnce())
        }

        val remoteResult = remoteDataSource.getTodayQuote()
        return remoteResult.fold(
            onSuccess = { dto ->
                val rawText = dto.quoteText?.trim().orEmpty()
                val rawAuthor = dto.author?.trim().orEmpty()
                val category = QuoteCategorizer.categorize(rawText, rawAuthor)

                val existing = quoteDao.findQuoteByExactText(rawText)
                val finalQuote = if (existing != null) {
                    quoteDao.setDailyQuote(existing.id)
                    quoteDao.getQuoteByIdOnce(existing.id) ?: existing.copy(isDailyQuote = true)
                } else {
                    val newId = quoteDao.insertQuote(
                        QuoteEntity(
                            quoteText = rawText,
                            author = rawAuthor,
                            category = category,
                            isFavorite = false,
                            isDailyQuote = true,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                    quoteDao.setDailyQuote(newId)
                    quoteDao.getQuoteByIdOnce(newId)
                }

                preferences.setLastTodayApiSyncDate(todayKey)
                preferences.setLastDailyQuoteDate(todayKey)
                if (finalQuote != null) {
                    preferences.addRecentQuoteId(finalQuote.id)
                }
                Result.success(finalQuote)
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    /**
     * Fetches a batch of 50 quotes from ZenQuotes (/api/quotes) in a single request,
     * filters out duplicates, and caches them in Room.
     * This respects the 5 requests / 30s limit by batching rather than requesting single quotes.
     */
    suspend fun fetchQuotesBatchFromApi(): Result<Int> {
        ensureInitialized()

        val remoteResult = remoteDataSource.getQuotesBatch()
        return remoteResult.fold(
            onSuccess = { batch ->
                val existingTexts = quoteDao.getAllQuoteTexts()
                    .map { normalizeQuoteText(it) }
                    .toHashSet()

                val newEntities = mutableListOf<QuoteEntity>()
                val now = System.currentTimeMillis()

                for ((index, dto) in batch.withIndex()) {
                    if (!dto.isValidQuote()) continue
                    val text = dto.quoteText!!.trim()
                    val normalized = normalizeQuoteText(text)
                    if (existingTexts.contains(normalized)) continue

                    existingTexts.add(normalized)
                    val author = dto.author!!.trim()
                    val category = QuoteCategorizer.categorize(text, author)

                    newEntities.add(
                        QuoteEntity(
                            quoteText = text,
                            author = author,
                            category = category,
                            isFavorite = false,
                            isDailyQuote = false,
                            createdAt = now + index
                        )
                    )
                }

                if (newEntities.isNotEmpty()) {
                    quoteDao.insertQuotes(newEntities)
                }

                preferences.setLastSyncTimestamp(now)
                Result.success(newEntities.size)
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    /**
     * Fetches a single random quote from ZenQuotes (/api/random),
     * caches it in Room if unique, and returns it.
     */
    suspend fun fetchRandomQuoteFromApi(): Result<QuoteEntity> {
        ensureInitialized()

        val remoteResult = remoteDataSource.getRandomQuote()
        return remoteResult.fold(
            onSuccess = { dto ->
                val text = dto.quoteText!!.trim()
                val author = dto.author!!.trim()
                val existing = quoteDao.findQuoteByExactText(text)

                val quote = if (existing != null) {
                    existing
                } else {
                    val category = QuoteCategorizer.categorize(text, author)
                    val id = quoteDao.insertQuote(
                        QuoteEntity(
                            quoteText = text,
                            author = author,
                            category = category,
                            isFavorite = false,
                            isDailyQuote = false,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                    quoteDao.getQuoteByIdOnce(id)
                        ?: QuoteEntity(id = id, quoteText = text, author = author, category = category)
                }

                preferences.addRecentQuoteId(quote.id)
                Result.success(quote)
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    private fun normalizeQuoteText(text: String): String {
        return text.trim()
            .replace("“", "\"")
            .replace("”", "\"")
            .replace("‘", "'")
            .replace("’", "'")
            .lowercase(Locale.ROOT)
    }

    companion object {
        const val ALL_CATEGORY = "All"

        fun currentDateKey(): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        }
    }
}
