package com.snj.quoteverse.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room database entity representing a single quote entry in QuoteVerse.
 */
@Entity(tableName = "quotes")
data class QuoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val quoteText: String,
    val author: String,
    val category: String,
    val isFavorite: Boolean = false,
    val isDailyQuote: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

