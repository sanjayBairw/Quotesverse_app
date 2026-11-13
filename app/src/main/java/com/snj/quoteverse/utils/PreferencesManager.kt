package com.snj.quoteverse.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val nightMode: Int, val storageKey: String) {
    SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, "system"),
    LIGHT(AppCompatDelegate.MODE_NIGHT_NO, "light"),
    DARK(AppCompatDelegate.MODE_NIGHT_YES, "dark");

    companion object {
        fun fromKey(key: String?): ThemeMode {
            return entries.firstOrNull { it.storageKey == key } ?: SYSTEM
        }
    }
}

/**
 * Abstraction over local user preferences so repositories and ViewModels remain unit-testable.
 */
interface QuotePreferences {
    val recentQuoteIds: Flow<List<Long>>
    val themeMode: Flow<ThemeMode>
    val notificationsEnabled: Flow<Boolean>
    val notificationTime: Flow<Pair<Int, Int>>
    val appFont: Flow<FontOption>
    val detailFontSize: Flow<Float>

    fun getLastDailyQuoteDate(): String?
    fun setLastDailyQuoteDate(dateKey: String)
    fun addRecentQuoteId(quoteId: Long)
    fun getRecentQuoteIds(): List<Long>
    fun getThemeMode(): ThemeMode
    fun setThemeMode(mode: ThemeMode)
    fun isNotificationsEnabled(): Boolean
    fun setNotificationsEnabled(enabled: Boolean)
    fun getNotificationHour(): Int
    fun getNotificationMinute(): Int
    fun setNotificationTime(hour: Int, minute: Int)
    fun getSelectedDetailStyle(): Int
    fun setSelectedDetailStyle(styleIndex: Int)
    fun getLastSyncTimestamp(): Long
    fun setLastSyncTimestamp(timestamp: Long)
    fun getLastTodayApiSyncDate(): String?
    fun setLastTodayApiSyncDate(dateKey: String)
    fun getAppFont(): FontOption
    fun setAppFont(font: FontOption)
    fun getDetailFontSize(): Float
    fun setDetailFontSize(sizeSp: Float)
}

/**
 * SharedPreferences-backed persistence for theme, notification schedule, and recent quotes.
 */
class PreferencesManager(context: Context) : QuotePreferences {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _recentQuoteIds = MutableStateFlow(readRecentIds())
    override val recentQuoteIds: Flow<List<Long>> = _recentQuoteIds.asStateFlow()

    private val _themeMode = MutableStateFlow(getThemeMode())
    override val themeMode: Flow<ThemeMode> = _themeMode.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(isNotificationsEnabled())
    override val notificationsEnabled: Flow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _notificationTime = MutableStateFlow(getNotificationHour() to getNotificationMinute())
    override val notificationTime: Flow<Pair<Int, Int>> = _notificationTime.asStateFlow()

    private val _appFont = MutableStateFlow(getAppFont())
    override val appFont: Flow<FontOption> = _appFont.asStateFlow()

    private val _detailFontSize = MutableStateFlow(getDetailFontSize())
    override val detailFontSize: Flow<Float> = _detailFontSize.asStateFlow()

    override fun getLastDailyQuoteDate(): String? = prefs.getString(KEY_DAILY_QUOTE_DATE, null)

    override fun setLastDailyQuoteDate(dateKey: String) {
        prefs.edit().putString(KEY_DAILY_QUOTE_DATE, dateKey).apply()
    }

    override fun addRecentQuoteId(quoteId: Long) {
        if (quoteId <= 0L) return
        val updated = buildList {
            add(quoteId)
            _recentQuoteIds.value
                .filter { it != quoteId }
                .take(MAX_RECENT_QUOTES - 1)
                .forEach { add(it) }
        }
        prefs.edit().putString(KEY_RECENT_IDS, updated.joinToString(",")).apply()
        _recentQuoteIds.value = updated
    }

    override fun getRecentQuoteIds(): List<Long> = _recentQuoteIds.value

    override fun getThemeMode(): ThemeMode =
        ThemeMode.fromKey(prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.storageKey))

    override fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.storageKey).apply()
        _themeMode.value = mode
    }

    override fun isNotificationsEnabled(): Boolean =
        prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, false)

    override fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
        _notificationsEnabled.value = enabled
    }

    override fun getNotificationHour(): Int = prefs.getInt(KEY_NOTIFICATION_HOUR, DEFAULT_HOUR)

    override fun getNotificationMinute(): Int = prefs.getInt(KEY_NOTIFICATION_MINUTE, DEFAULT_MINUTE)

    override fun setNotificationTime(hour: Int, minute: Int) {
        val validHour = hour.coerceIn(0, 23)
        val validMinute = minute.coerceIn(0, 59)
        prefs.edit()
            .putInt(KEY_NOTIFICATION_HOUR, validHour)
            .putInt(KEY_NOTIFICATION_MINUTE, validMinute)
            .apply()
        _notificationTime.value = validHour to validMinute
    }

    override fun getSelectedDetailStyle(): Int = prefs.getInt(KEY_DETAIL_STYLE, 0)

    override fun setSelectedDetailStyle(styleIndex: Int) {
        prefs.edit().putInt(KEY_DETAIL_STYLE, styleIndex.coerceAtLeast(0)).apply()
    }

    override fun getLastSyncTimestamp(): Long = prefs.getLong(KEY_LAST_SYNC_TIMESTAMP, 0L)

    override fun setLastSyncTimestamp(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC_TIMESTAMP, timestamp).apply()
    }

    override fun getLastTodayApiSyncDate(): String? = prefs.getString(KEY_LAST_TODAY_SYNC_DATE, null)

    override fun setLastTodayApiSyncDate(dateKey: String) {
        prefs.edit().putString(KEY_LAST_TODAY_SYNC_DATE, dateKey).apply()
    }

    override fun getAppFont(): FontOption =
        FontOption.fromKey(prefs.getString(KEY_APP_FONT, FontOption.DEFAULT.key))

    override fun setAppFont(font: FontOption) {
        prefs.edit().putString(KEY_APP_FONT, font.key).apply()
        _appFont.value = font
    }

    override fun getDetailFontSize(): Float =
        prefs.getFloat(KEY_DETAIL_FONT_SIZE, DEFAULT_FONT_SIZE)

    override fun setDetailFontSize(sizeSp: Float) {
        val clamped = sizeSp.coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE)
        prefs.edit().putFloat(KEY_DETAIL_FONT_SIZE, clamped).apply()
        _detailFontSize.value = clamped
    }

    private fun readRecentIds(): List<Long> {
        val raw = prefs.getString(KEY_RECENT_IDS, "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.split(",")
            .mapNotNull { it.trim().toLongOrNull() }
            .distinct()
            .take(MAX_RECENT_QUOTES)
    }

    companion object {
        private const val PREFS_NAME = "quoteverse_prefs"
        private const val KEY_DAILY_QUOTE_DATE = "daily_quote_date"
        private const val KEY_RECENT_IDS = "recent_quote_ids"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        private const val KEY_NOTIFICATION_HOUR = "notification_hour"
        private const val KEY_NOTIFICATION_MINUTE = "notification_minute"
        private const val KEY_DETAIL_STYLE = "detail_style_index"
        private const val KEY_LAST_SYNC_TIMESTAMP = "last_sync_timestamp"
        private const val KEY_LAST_TODAY_SYNC_DATE = "last_today_sync_date"
        private const val KEY_APP_FONT = "app_font_key"
        private const val KEY_DETAIL_FONT_SIZE = "detail_font_size_sp"
        private const val DEFAULT_FONT_SIZE = 26f
        const val MIN_FONT_SIZE = 18f
        const val MAX_FONT_SIZE = 36f
        private const val DEFAULT_HOUR = 8
        private const val DEFAULT_MINUTE = 0
        private const val MAX_RECENT_QUOTES = 10
    }
}

