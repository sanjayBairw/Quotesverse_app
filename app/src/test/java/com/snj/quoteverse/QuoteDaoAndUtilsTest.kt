package com.snj.quoteverse

import com.snj.quoteverse.data.entity.QuoteEntity
import com.snj.quoteverse.utils.NotificationScheduler
import com.snj.quoteverse.utils.QuoteActionUtils
import java.util.Calendar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuoteDaoAndUtilsTest {

    @Test
    fun fakeQuoteDaoHandlesInsertCategorySearchFavoriteAndDailyQuote() = runTest {
        val dao = FakeQuoteDao()
        val quotes = listOf(
            QuoteEntity(quoteText = "Stay hungry, stay foolish.", author = "Steve Jobs", category = "Motivation"),
            QuoteEntity(quoteText = "Knowledge is power.", author = "Francis Bacon", category = "Study"),
            QuoteEntity(quoteText = "Turn your wounds into wisdom.", author = "Oprah Winfrey", category = "Life")
        )
        dao.insertQuotes(quotes)

        assertEquals(3, dao.getQuoteCount())
        val all = dao.getAllQuotesOnce()
        val secondId = all[1].id

        dao.updateFavorite(secondId, true)
        assertEquals(1, dao.getFavoriteCount().first())
        assertEquals(secondId, dao.getFavoriteQuotes().first().single().id)

        dao.updateFavorite(secondId, false)
        assertEquals(0, dao.getFavoriteCount().first())

        val searchResults = dao.searchQuotes("Bacon").first()
        assertEquals(1, searchResults.size)
        assertEquals("Study", searchResults.first().category)

        val categorySearch = dao.searchQuotesByCategory("wisdom", "Life").first()
        assertEquals(1, categorySearch.size)

        dao.setDailyQuote(secondId)
        val daily = dao.getDailyQuoteOnce()
        assertNotNull(daily)
        assertEquals(secondId, daily?.id)
        assertFalse(dao.getQuoteByIdOnce(all[0].id)?.isDailyQuote == true)
    }

    @Test
    fun quoteActionUtilsFormatsQuotesAndTimesAccurately() {
        val quote = QuoteEntity(
            id = 1L,
            quoteText = "Knowing yourself is the beginning of all wisdom.",
            author = "Aristotle",
            category = "Wisdom"
        )

        val clipboardText = QuoteActionUtils.formatQuoteForClipboard(quote)
        assertEquals("\"Knowing yourself is the beginning of all wisdom.\" — Aristotle", clipboardText)

        val shareText = QuoteActionUtils.formatQuoteForShare(quote)
        assertTrue(shareText.contains("#QuoteVerse"))
        assertTrue(shareText.contains("#Wisdom"))

        assertEquals("12:05 AM", QuoteActionUtils.formatTime12Hour(0, 5))
        assertEquals("08:00 AM", QuoteActionUtils.formatTime12Hour(8, 0))
        assertEquals("12:30 PM", QuoteActionUtils.formatTime12Hour(12, 30))
        assertEquals("09:45 PM", QuoteActionUtils.formatTime12Hour(21, 45))
    }

    @Test
    fun notificationTriggerCalculatesSameDayOrNextDayAccurately() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 12, 21, 47, 15)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val sameDayTrigger = NotificationScheduler.calculateNextTriggerMillis(21, 48, now)
        val sameDayCal = Calendar.getInstance().apply { timeInMillis = sameDayTrigger }
        assertEquals(12, sameDayCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(21, sameDayCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(48, sameDayCal.get(Calendar.MINUTE))
        assertEquals(0, sameDayCal.get(Calendar.SECOND))

        val nextDayTrigger = NotificationScheduler.calculateNextTriggerMillis(21, 46, now)
        val nextDayCal = Calendar.getInstance().apply { timeInMillis = nextDayTrigger }
        assertEquals(13, nextDayCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(21, nextDayCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(46, nextDayCal.get(Calendar.MINUTE))
    }
}

