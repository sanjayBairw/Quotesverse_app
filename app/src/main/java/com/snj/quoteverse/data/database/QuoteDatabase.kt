package com.snj.quoteverse.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.snj.quoteverse.data.dao.QuoteDao
import com.snj.quoteverse.data.entity.QuoteEntity

/**
 * Room database holding QuoteVerse offline quote data.
 * Initial quote seeding is coordinated through QuoteRepository.ensureInitialized().
 */
@Database(
    entities = [QuoteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class QuoteDatabase : RoomDatabase() {

    abstract fun quoteDao(): QuoteDao

    companion object {
        private const val DATABASE_NAME = "quoteverse_database"

        @Volatile
        private var INSTANCE: QuoteDatabase? = null

        fun getInstance(context: Context): QuoteDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(appContext: Context): QuoteDatabase {
            return Room.databaseBuilder(
                appContext,
                QuoteDatabase::class.java,
                DATABASE_NAME
            ).build()
        }
    }
}
