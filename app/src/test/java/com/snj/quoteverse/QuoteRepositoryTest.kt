package com.snj.quoteverse

import com.snj.quoteverse.data.repository.QuoteRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QuoteRepositoryTest {

    private lateinit var fakeDao: FakeQuoteDao
    private lateinit var fakePreferences: FakeQuotePreferences
    private lateinit var repository: QuoteRepository

    @Before
    fun setup() {
        fakeDao = FakeQuoteDao()
        fakePreferences = FakeQuotePreferences()
        repository = QuoteRepository(fakeDao, fakePreferences)
    }

    @Test
    fun ensureInitializedSeedsQuotesIdempotentlyAndAssignsDailyQuote() = runTest {
        val firstDaily = repository.ensureInitialized("2026-11-12")
        assertNotNull(firstDaily)
        assertTrue(fakeDao.getQuoteCount() >= 100)

        val secondDailySameDay = repository.ensureInitialized("2026-11-12")
        assertEquals(firstDaily?.id, secondDailySameDay?.id)
        assertEquals(fakeDao.getQuoteCount(), fakeDao.getAllQuotesOnce().size)
    }

    @Test
    fun ensureInitializedRotatesDailyQuoteWhenDayChanges() = runTest {
        val dayOneDaily = repository.ensureInitialized("2026-11-12")
        val dayTwoDaily = repository.ensureInitialized("2026-11-13")

        assertNotNull(dayOneDaily)
        assertNotNull(dayTwoDaily)
        assertNotEquals(dayOneDaily?.id, dayTwoDaily?.id)
    }

    @Test
    fun toggleFavoritePersistsAndEmitsInFavoriteQuotesFlow() = runTest {
        repository.ensureInitialized("2026-11-12")
        val target = repository.allQuotes.first().first()
        assertFalse(target.isFavorite)

        val added = repository.toggleFavorite(target)
        assertTrue(added)

        val favoritesAfterAdd = repository.favoriteQuotes.first()
        assertTrue(favoritesAfterAdd.any { it.id == target.id && it.isFavorite })

        val updatedTarget = repository.getQuoteByIdOnce(target.id)!!
        val removed = repository.toggleFavorite(updatedTarget)
        assertFalse(removed)

        val favoritesAfterRemove = repository.favoriteQuotes.first()
        assertTrue(favoritesAfterRemove.none { it.id == target.id })
    }

    @Test
    fun getRandomQuoteExcludesCurrentAndRespectsCategory() = runTest {
        repository.ensureInitialized("2026-11-12")
        val motivationQuotes = repository.observeQuotes("", "Motivation").first()
        val firstMotivation = motivationQuotes.first()

        val randomMotivation = repository.getRandomQuote(
            excludeId = firstMotivation.id,
            category = "Motivation"
        )

        assertNotNull(randomMotivation)
        assertEquals("Motivation", randomMotivation?.category)
        assertNotEquals(firstMotivation.id, randomMotivation?.id)
    }

    @Test
    fun observeQuotesSearchesByTextAndAuthorAndFiltersCategory() = runTest {
        repository.ensureInitialized("2026-11-12")

        val byAuthor = repository.observeQuotes("Aristotle", null).first()
        assertTrue(byAuthor.isNotEmpty())
        assertTrue(byAuthor.all { it.author.contains("Aristotle", ignoreCase = true) })

        val byTextAndCategory = repository.observeQuotes("wisdom", "Wisdom").first()
        assertTrue(byTextAndCategory.isNotEmpty())
        assertTrue(
            byTextAndCategory.all {
                it.category == "Wisdom" &&
                    (it.quoteText.contains("wisdom", ignoreCase = true) ||
                        it.author.contains("wisdom", ignoreCase = true))
            }
        )
    }

    @Test
    fun recordQuoteViewedTracksRecentQuotesInMostRecentOrder() = runTest {
        repository.ensureInitialized("2026-11-12")
        val quotes = repository.allQuotes.first()

        repository.recordQuoteViewed(quotes[2].id)
        repository.recordQuoteViewed(quotes[5].id)
        repository.recordQuoteViewed(quotes[2].id)

        val recent = repository.recentlyViewedQuotes.first()
        assertEquals(quotes[2].id, recent.first().id)
        assertEquals(quotes[5].id, recent[1].id)
    }
}

