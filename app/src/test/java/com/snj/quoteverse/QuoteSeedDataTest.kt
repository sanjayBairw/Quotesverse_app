package com.snj.quoteverse

import com.snj.quoteverse.data.database.QuoteSeedData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuoteSeedDataTest {

    @Test
    fun initialQuotesContainAtLeast100UniqueQuotesAcrossAllCategories() {
        val quotes = QuoteSeedData.getInitialQuotes()

        assertTrue("Expected at least 100 quotes, got ${quotes.size}", quotes.size >= 100)

        val uniqueTexts = quotes.map { it.quoteText.trim().lowercase() }.toSet()
        assertEquals("Duplicate quote texts detected in seed data", quotes.size, uniqueTexts.size)

        quotes.forEach { quote ->
            assertTrue("Quote text must not be blank", quote.quoteText.isNotBlank())
            assertTrue("Author must not be blank", quote.author.isNotBlank())
            assertTrue("Category must be recognized", quote.category in QuoteSeedData.categories)
        }

        QuoteSeedData.categories.forEach { category ->
            val count = quotes.count { it.category == category }
            assertTrue("Category $category should contain quotes", count >= 10)
        }
    }
}

