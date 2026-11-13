package com.snj.quoteverse

import com.snj.quoteverse.data.api.QuoteCategorizer
import com.snj.quoteverse.data.api.ZenQuoteDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuoteCategorizerAndDtoTest {

    @Test
    fun zenQuoteDtoDetectsValidQuotesAndRateLimitPayload() {
        val validQuote = ZenQuoteDto(
            quoteText = "Peace begins with a smile.",
            author = "Mother Teresa"
        )
        assertTrue(validQuote.isValidQuote())
        assertFalse(validQuote.isRateLimitWarning())

        val rateLimitWarning = ZenQuoteDto(
            quoteText = "Too many requests. Obtain an auth key for unlimited access, or try again later.",
            author = "zenquotes.io"
        )
        assertFalse(rateLimitWarning.isValidQuote())
        assertTrue(rateLimitWarning.isRateLimitWarning())

        val blankQuote = ZenQuoteDto(quoteText = "   ", author = "")
        assertFalse(blankQuote.isValidQuote())
    }

    @Test
    fun quoteCategorizerAccuratelyCategorizesByKeywords() {
        assertEquals("Love", QuoteCategorizer.categorize("Where there is love there is life.", "Gandhi"))
        assertEquals("Friendship", QuoteCategorizer.categorize("A faithful friend is a strong defense.", "Traditional"))
        assertEquals("Happiness", QuoteCategorizer.categorize("Count your life by smiles, not tears.", "John Lennon"))
        assertEquals("Success", QuoteCategorizer.categorize("The secret of success is constancy to purpose.", "Disraeli"))
        assertEquals("Motivation", QuoteCategorizer.categorize("Energy and persistence conquer all things.", "Benjamin Franklin"))
        assertEquals("Study", QuoteCategorizer.categorize("Education is the kindling of a flame.", "Socrates"))
        assertEquals("Attitude", QuoteCategorizer.categorize("Attitude is a little thing that makes a big difference.", "Churchill"))
        assertEquals("Wisdom", QuoteCategorizer.categorize("Silence is a source of great strength and truth.", "Lao Tzu"))
        assertEquals("Inspirational", QuoteCategorizer.categorize("Hold fast to dreams and let your hope shine like a star.", "Poet"))
    }
}

