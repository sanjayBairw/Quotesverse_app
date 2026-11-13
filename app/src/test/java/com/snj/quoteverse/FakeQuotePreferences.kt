package com.snj.quoteverse

import com.snj.quoteverse.utils.QuotePreferences
import com.snj.quoteverse.utils.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory [QuotePreferences] implementation for deterministic unit testing.
 */
class FakeQuotePreferences : QuotePreferences {

    private val _recentIds = MutableStateFlow<List<Long>>(emptyList())
    override val recentQuoteIds: Flow<List<Long>> = _recentIds.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    override val themeMode: Flow<ThemeMode> = _themeMode.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(false)
    override val notificationsEnabled: Flow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _notificationTime = MutableStateFlow(8 to 0)
    override val notificationTime: Flow<Pair<Int, Int>> = _notificationTime.asStateFlow()

    private val _appFont = MutableStateFlow(com.snj.quoteverse.utils.FontOption.DEFAULT)
    override val appFont: Flow<com.snj.quoteverse.utils.FontOption> = _appFont.asStateFlow()

    private val _detailFontSize = MutableStateFlow(26f)
    override val detailFontSize: Flow<Float> = _detailFontSize.asStateFlow()

    private var lastDailyDate: String? = null
    private var lastTodayApiDate: String? = null
    private var lastSyncTime: Long = 0L
    private var detailStyle: Int = 0

    override fun getLastDailyQuoteDate(): String? = lastDailyDate

    override fun setLastDailyQuoteDate(dateKey: String) {
        lastDailyDate = dateKey
    }

    override fun getLastSyncTimestamp(): Long = lastSyncTime

    override fun setLastSyncTimestamp(timestamp: Long) {
        lastSyncTime = timestamp
    }

    override fun getLastTodayApiSyncDate(): String? = lastTodayApiDate

    override fun setLastTodayApiSyncDate(dateKey: String) {
        lastTodayApiDate = dateKey
    }

    override fun addRecentQuoteId(quoteId: Long) {
        if (quoteId <= 0L) return
        _recentIds.value = (listOf(quoteId) + _recentIds.value.filter { it != quoteId }).take(10)
    }

    override fun getRecentQuoteIds(): List<Long> = _recentIds.value

    override fun getThemeMode(): ThemeMode = _themeMode.value

    override fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    override fun isNotificationsEnabled(): Boolean = _notificationsEnabled.value

    override fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
    }

    override fun getNotificationHour(): Int = _notificationTime.value.first

    override fun getNotificationMinute(): Int = _notificationTime.value.second

    override fun setNotificationTime(hour: Int, minute: Int) {
        _notificationTime.value = hour.coerceIn(0, 23) to minute.coerceIn(0, 59)
    }

    override fun getSelectedDetailStyle(): Int = detailStyle

    override fun setSelectedDetailStyle(styleIndex: Int) {
        detailStyle = styleIndex.coerceAtLeast(0)
    }

    override fun getAppFont(): com.snj.quoteverse.utils.FontOption = _appFont.value

    override fun setAppFont(font: com.snj.quoteverse.utils.FontOption) {
        _appFont.value = font
    }

    override fun getDetailFontSize(): Float = _detailFontSize.value

    override fun setDetailFontSize(sizeSp: Float) {
        _detailFontSize.value = sizeSp
    }
}

