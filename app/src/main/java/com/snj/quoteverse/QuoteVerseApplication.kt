package com.snj.quoteverse

import android.app.Application
import com.snj.quoteverse.data.database.QuoteDatabase
import com.snj.quoteverse.data.repository.QuoteRepository
import com.snj.quoteverse.utils.NotificationScheduler
import com.snj.quoteverse.utils.PreferencesManager
import com.snj.quoteverse.utils.ThemeManager

/**
 * Application class initializing theme, Room database, preferences, and repository singletons.
 */
class QuoteVerseApplication : Application() {

    val preferencesManager: PreferencesManager by lazy {
        PreferencesManager(this)
    }

    val database: QuoteDatabase by lazy {
        QuoteDatabase.getInstance(this)
    }

    val networkMonitor: com.snj.quoteverse.utils.NetworkMonitor by lazy {
        com.snj.quoteverse.utils.NetworkMonitor(this)
    }

    val repository: QuoteRepository by lazy {
        QuoteRepository(database.quoteDao(), preferencesManager)
    }

    override fun onCreate() {
        super.onCreate()
        com.snj.quoteverse.utils.FontOption.init(this)
        ThemeManager.applyTheme(preferencesManager.getThemeMode())
        NotificationScheduler.createNotificationChannel(this)
        if (preferencesManager.isNotificationsEnabled()) {
            NotificationScheduler.scheduleDailyQuote(
                this,
                preferencesManager.getNotificationHour(),
                preferencesManager.getNotificationMinute()
            )
        }
    }
}

