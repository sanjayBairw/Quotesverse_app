package com.snj.quoteverse

import com.snj.quoteverse.data.dao.QuoteDao
import com.snj.quoteverse.data.entity.QuoteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory Fake [QuoteDao] replicating Room query semantics for fast deterministic JVM tests.
 */
class FakeQuoteDao : QuoteDao {

    private val quotesState = MutableStateFlow<List<QuoteEntity>>(emptyList())
    private var nextId = 1L

    override fun getAllQuotes(): Flow<List<QuoteEntity>> = quotesState.map { list ->
        list.sortedBy { it.id }
    }

    override suspend fun getAllQuotesOnce(): List<QuoteEntity> = quotesState.value.sortedBy { it.id }

    override fun getQuoteById(id: Long): Flow<QuoteEntity?> = quotesState.map { list ->
        list.firstOrNull { it.id == id }
    }

    override suspend fun getQuoteByIdOnce(id: Long): QuoteEntity? {
        return quotesState.value.firstOrNull { it.id == id }
    }

    override fun getQuotesByCategory(category: String): Flow<List<QuoteEntity>> = quotesState.map { list ->
        list.filter { it.category == category }.sortedBy { it.id }
    }

    override fun getFavoriteQuotes(): Flow<List<QuoteEntity>> = quotesState.map { list ->
        list.filter { it.isFavorite }.sortedWith(compareByDescending<QuoteEntity> { it.createdAt }.thenByDescending { it.id })
    }

    override fun searchQuotes(query: String): Flow<List<QuoteEntity>> = quotesState.map { list ->
        list.filter {
            it.quoteText.contains(query, ignoreCase = true) ||
                it.author.contains(query, ignoreCase = true)
        }.sortedBy { it.id }
    }

    override fun searchQuotesByCategory(query: String, category: String): Flow<List<QuoteEntity>> =
        quotesState.map { list ->
            list.filter {
                it.category == category && (
                    it.quoteText.contains(query, ignoreCase = true) ||
                        it.author.contains(query, ignoreCase = true)
                    )
            }.sortedBy { it.id }
        }

    override fun getDailyQuote(): Flow<QuoteEntity?> = quotesState.map { list ->
        list.firstOrNull { it.isDailyQuote }
    }

    override suspend fun getDailyQuoteOnce(): QuoteEntity? {
        return quotesState.value.firstOrNull { it.isDailyQuote }
    }

    override suspend fun getRandomQuote(): QuoteEntity? {
        return quotesState.value.randomOrNull()
    }

    override suspend fun getRandomQuoteExcluding(excludeId: Long): QuoteEntity? {
        return quotesState.value.filter { it.id != excludeId }.randomOrNull()
    }

    override suspend fun getRandomQuoteByCategory(category: String, excludeId: Long): QuoteEntity? {
        return quotesState.value.filter { it.category == category && it.id != excludeId }.randomOrNull()
    }

    override suspend fun getQuoteCount(): Int = quotesState.value.size

    override fun getFavoriteCount(): Flow<Int> = quotesState.map { list ->
        list.count { it.isFavorite }
    }

    override suspend fun getAllQuoteTexts(): List<String> =
        quotesState.value.map { it.quoteText }

    override suspend fun findQuoteByExactText(text: String): QuoteEntity? =
        quotesState.value.firstOrNull {
            it.quoteText.trim().equals(text.trim(), ignoreCase = true)
        }

    override suspend fun findQuoteByTextAndAuthor(text: String, author: String): QuoteEntity? =
        quotesState.value.firstOrNull {
            it.quoteText.trim().equals(text.trim(), ignoreCase = true) &&
                it.author.trim().equals(author.trim(), ignoreCase = true)
        }

    override suspend fun insertQuote(quote: QuoteEntity): Long {
        val assignedId = if (quote.id == 0L) nextId++ else quote.id.also {
            if (it >= nextId) nextId = it + 1
        }
        val persisted = quote.copy(id = assignedId)
        val updated = quotesState.value.filterNot { it.id == assignedId } + persisted
        quotesState.value = updated
        return assignedId
    }

    override suspend fun insertQuotes(quotes: List<QuoteEntity>) {
        val current = quotesState.value.toMutableList()
        for (quote in quotes) {
            val assignedId = if (quote.id == 0L) nextId++ else quote.id.also {
                if (it >= nextId) nextId = it + 1
            }
            current.removeAll { it.id == assignedId }
            current.add(quote.copy(id = assignedId))
        }
        quotesState.value = current
    }

    override suspend fun updateFavorite(quoteId: Long, isFavorite: Boolean) {
        quotesState.value = quotesState.value.map { quote ->
            if (quote.id == quoteId) quote.copy(isFavorite = isFavorite) else quote
        }
    }

    override suspend fun clearDailyQuotes() {
        quotesState.value = quotesState.value.map { quote ->
            if (quote.isDailyQuote) quote.copy(isDailyQuote = false) else quote
        }
    }

    override suspend fun markAsDailyQuote(quoteId: Long) {
        quotesState.value = quotesState.value.map { quote ->
            if (quote.id == quoteId) quote.copy(isDailyQuote = true) else quote
        }
    }
}

