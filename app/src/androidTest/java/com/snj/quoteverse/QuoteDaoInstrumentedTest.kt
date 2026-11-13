package com.snj.quoteverse

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.snj.quoteverse.data.dao.QuoteDao
import com.snj.quoteverse.data.database.QuoteDatabase
import com.snj.quoteverse.data.database.QuoteSeedData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuoteDaoInstrumentedTest {

    private lateinit var database: QuoteDatabase
    private lateinit var quoteDao: QuoteDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            QuoteDatabase::class.java
        ).allowMainThreadQueries().build()
        quoteDao = database.quoteDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertSearchFavoriteRandomAndDailyQuoteWorkEndToEnd() = runBlocking {
        quoteDao.insertQuotes(QuoteSeedData.getInitialQuotes())
        assertTrue(quoteDao.getQuoteCount() >= 100)

        val all = quoteDao.getAllQuotesOnce()
        val firstQuote = all.first()

        // Favorite toggle
        quoteDao.updateFavorite(firstQuote.id, true)
        val favorites = quoteDao.getFavoriteQuotes().first()
        assertTrue(favorites.any { it.id == firstQuote.id })

        quoteDao.updateFavorite(firstQuote.id, false)
        assertFalse(quoteDao.getQuoteByIdOnce(firstQuote.id)?.isFavorite == true)

        // Search
        val searchMatches = quoteDao.searchQuotes("Aristotle").first()
        assertTrue(searchMatches.isNotEmpty())
        assertTrue(searchMatches.all { it.author.contains("Aristotle", ignoreCase = true) })

        // Random quote excluding current
        val randomQuote = quoteDao.getRandomQuoteExcluding(firstQuote.id)
        assertNotNull(randomQuote)
        assertNotEquals(firstQuote.id, randomQuote?.id)

        // Daily quote transaction
        val secondQuote = all[1]
        quoteDao.setDailyQuote(secondQuote.id)
        val daily = quoteDao.getDailyQuoteOnce()
        assertEquals(secondQuote.id, daily?.id)
    }
}

