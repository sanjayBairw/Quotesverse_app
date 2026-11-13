package com.snj.quoteverse.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.snj.quoteverse.data.entity.QuoteEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for querying and mutating quotes in the local Room database.
 */
@Dao
interface QuoteDao {

    @Query("SELECT * FROM quotes ORDER BY id ASC")
    fun getAllQuotes(): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM quotes ORDER BY id ASC")
    suspend fun getAllQuotesOnce(): List<QuoteEntity>

    @Query("SELECT * FROM quotes WHERE id = :id LIMIT 1")
    fun getQuoteById(id: Long): Flow<QuoteEntity?>

    @Query("SELECT * FROM quotes WHERE id = :id LIMIT 1")
    suspend fun getQuoteByIdOnce(id: Long): QuoteEntity?

    @Query("SELECT * FROM quotes WHERE category = :category ORDER BY id ASC")
    fun getQuotesByCategory(category: String): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM quotes WHERE isFavorite = 1 ORDER BY createdAt DESC, id DESC")
    fun getFavoriteQuotes(): Flow<List<QuoteEntity>>

    @Query(
        """
        SELECT * FROM quotes
        WHERE quoteText LIKE '%' || :query || '%'
           OR author LIKE '%' || :query || '%'
        ORDER BY id ASC
        """
    )
    fun searchQuotes(query: String): Flow<List<QuoteEntity>>

    @Query(
        """
        SELECT * FROM quotes
        WHERE category = :category
          AND (quoteText LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%')
        ORDER BY id ASC
        """
    )
    fun searchQuotesByCategory(query: String, category: String): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM quotes WHERE isDailyQuote = 1 LIMIT 1")
    fun getDailyQuote(): Flow<QuoteEntity?>

    @Query("SELECT * FROM quotes WHERE isDailyQuote = 1 LIMIT 1")
    suspend fun getDailyQuoteOnce(): QuoteEntity?

    @Query("SELECT * FROM quotes ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomQuote(): QuoteEntity?

    @Query("SELECT * FROM quotes WHERE id != :excludeId ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomQuoteExcluding(excludeId: Long): QuoteEntity?

    @Query("SELECT * FROM quotes WHERE category = :category AND id != :excludeId ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomQuoteByCategory(category: String, excludeId: Long): QuoteEntity?

    @Query("SELECT COUNT(*) FROM quotes")
    suspend fun getQuoteCount(): Int

    @Query("SELECT COUNT(*) FROM quotes WHERE isFavorite = 1")
    fun getFavoriteCount(): Flow<Int>

    @Query("SELECT quoteText FROM quotes")
    suspend fun getAllQuoteTexts(): List<String>

    @Query("SELECT * FROM quotes WHERE LOWER(TRIM(quoteText)) = LOWER(TRIM(:text)) LIMIT 1")
    suspend fun findQuoteByExactText(text: String): QuoteEntity?

    @Query("SELECT * FROM quotes WHERE LOWER(TRIM(quoteText)) = LOWER(TRIM(:text)) AND LOWER(TRIM(author)) = LOWER(TRIM(:author)) LIMIT 1")
    suspend fun findQuoteByTextAndAuthor(text: String, author: String): QuoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuote(quote: QuoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuotes(quotes: List<QuoteEntity>)

    @Query("UPDATE quotes SET isFavorite = :isFavorite WHERE id = :quoteId")
    suspend fun updateFavorite(quoteId: Long, isFavorite: Boolean)

    @Query("UPDATE quotes SET isDailyQuote = 0 WHERE isDailyQuote = 1")
    suspend fun clearDailyQuotes()

    @Query("UPDATE quotes SET isDailyQuote = 1 WHERE id = :quoteId")
    suspend fun markAsDailyQuote(quoteId: Long)

    @Transaction
    suspend fun setDailyQuote(quoteId: Long) {
        clearDailyQuotes()
        markAsDailyQuote(quoteId)
    }
}

