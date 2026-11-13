package com.snj.quoteverse.data.api

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object representing a quote returned by ZenQuotes API.
 * Free endpoints return JSON arrays of this structure.
 *
 * Fields:
 * - q: Quote text
 * - a: Author name
 * - c: Character count
 * - h: HTML representation
 */
data class ZenQuoteDto(
    @SerializedName("q")
    val quoteText: String? = null,

    @SerializedName("a")
    val author: String? = null,

    @SerializedName("c")
    val characterCount: String? = null,

    @SerializedName("h")
    val html: String? = null
) {
    /**
     * Checks if this response is actually the ZenQuotes rate limit warning message:
     * "Too many requests. Obtain an auth key for unlimited access, or try again later."
     */
    fun isRateLimitWarning(): Boolean {
        val text = quoteText?.trim().orEmpty()
        val auth = author?.trim().orEmpty()
        return text.startsWith("Too many requests", ignoreCase = true) ||
                auth.equals("zenquotes.io", ignoreCase = true)
    }

    /**
     * Validates that the quote text and author are non-blank and not a service warning.
     */
    fun isValidQuote(): Boolean {
        val text = quoteText?.trim().orEmpty()
        val auth = author?.trim().orEmpty()
        return text.isNotBlank() &&
                auth.isNotBlank() &&
                !isRateLimitWarning()
    }
}

